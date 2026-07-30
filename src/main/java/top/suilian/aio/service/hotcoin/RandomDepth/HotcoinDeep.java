package top.suilian.aio.service.hotcoin.RandomDepth;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.CancelExceptionService;
import top.suilian.aio.service.CancelOrderService;
import top.suilian.aio.service.ExceptionMessageService;
import top.suilian.aio.service.RobotArgsService;
import top.suilian.aio.service.RobotLogService;
import top.suilian.aio.service.RobotService;
import top.suilian.aio.service.TradeLogService;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.vo.Order;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * 动态盘口变化层：维护少量可见订单，每轮只替换一笔，避免整批撤挂造成机械闪烁。
 */
public class HotcoinDeep extends HotCoinParentService {
    private static final int BUY = 1;
    private static final int SELL = 2;

    private final List<Order> activeOrders = new ArrayList<>();
    private final Random random = new Random();
    private boolean start = true;
    private int tradeNum = 0;
    private long buyCooldownUntil = 0L;
    private long sellCooldownUntil = 0L;
    private long buySuspendUntil = 0L;
    private long sellSuspendUntil = 0L;
    private final Deque<Long> buyCollisionTimes = new ArrayDeque<>();
    private final Deque<Long> sellCollisionTimes = new ArrayDeque<>();

    public HotcoinDeep(CancelExceptionService cancelExceptionService, CancelOrderService cancelOrderService,
                       ExceptionMessageService exceptionMessageService, RobotArgsService robotArgsService,
                       RobotLogService robotLogService, RobotService robotService, TradeLogService tradeLogService,
                       HttpUtil httpUtil, RedisHelper redisHelper, int id) {
        super.cancelExceptionService = cancelExceptionService;
        super.cancelOrderService = cancelOrderService;
        super.exceptionMessageService = exceptionMessageService;
        super.robotArgsService = robotArgsService;
        super.robotLogService = robotLogService;
        super.robotService = robotService;
        super.tradeLogService = tradeLogService;
        super.httpUtil = httpUtil;
        super.redisHelper = redisHelper;
        super.id = id;
        super.logger = getLogger("hotcoin/deepNew", id);
    }

    public void init() {
        if (start) {
            setParam();
            setPrecision();
            start = false;
        }
        if (tradeNum >= intParam("depthCancelNum", 20)) {
            setWarmLog(id, 2, "变化层疑似成交达到上限，暂停策略", "");
            sleep(60_000L);
            return;
        }

        Depth depth = loadDepth();
        if (depth == null) {
            sleep(randomDelay());
            return;
        }

        int orderCount = Math.max(1, intParam("depthChangeOrderCount", 3));
        int targetOrderCount = (isSuspended(BUY) ? 0 : orderCount) + (isSuspended(SELL) ? 0 : orderCount);
        if (activeOrders.size() < targetOrderCount) {
            int side = nextBootstrapSide(orderCount);
            if (side != 0) {
                place(side, depth);
            }
        } else {
            // 买卖独立变化；一次只替换一笔，盘口不会整面闪烁。
            Order oldOrder = activeOrders.get(random.nextInt(activeOrders.size()));
            replace(oldOrder, depth);
        }
        sleep(randomDelay());
    }

    private void replace(Order oldOrder, Depth depth) {
        boolean filled = isFilled(oldOrder);
        if (filled) {
            tradeNum++;
            long cooldownUntil = System.currentTimeMillis() + intParam("depthCooldownSeconds", 30) * 1000L;
            if (oldOrder.getType() == BUY) {
                buyCooldownUntil = cooldownUntil;
            } else {
                sellCooldownUntil = cooldownUntil;
            }
            setWarmLog(id, 2, "变化单疑似成交，订单=" + oldOrder.getOrderId() + "，该方向转入可见缓冲区", "");
            registerCollision(oldOrder.getType());
        } else {
            cancel(oldOrder);
        }
        activeOrders.remove(oldOrder);
        if (isSuspended(oldOrder.getType())) {
            return;
        }
        place(oldOrder.getType(), depth);
    }

    /**
     * 同方向在窗口内连续被成交，说明该方向报价可能被针对：撤该侧订单并短暂停止该侧变化。
     */
    private void registerCollision(int side) {
        long now = System.currentTimeMillis();
        int windowSeconds = intParam("depthCollisionWindowSeconds", 300);
        int threshold = Math.max(1, intParam("depthCollisionThreshold", 3));
        Deque<Long> times = side == BUY ? buyCollisionTimes : sellCollisionTimes;
        while (!times.isEmpty() && now - times.peekFirst() > windowSeconds * 1000L) {
            times.removeFirst();
        }
        times.addLast(now);
        if (times.size() < threshold) {
            return;
        }

        long suspendUntil = now + intParam("depthCollisionSuspendSeconds", 180) * 1000L;
        if (side == BUY) {
            buySuspendUntil = suspendUntil;
        } else {
            sellSuspendUntil = suspendUntil;
        }
        times.clear();
        cancelSideOrders(side);
        setWarmLog(id, 3, "变化层" + (side == BUY ? "买" : "卖") + "单在 " + windowSeconds
                + " 秒内连续成交 " + threshold + " 次，已撤该侧订单并暂停 "
                + intParam("depthCollisionSuspendSeconds", 180) + " 秒", "");
    }

    private void cancelSideOrders(int side) {
        List<Order> orders = new ArrayList<>(activeOrders);
        for (Order order : orders) {
            if (order.getType() == side) {
                cancel(order);
                activeOrders.remove(order);
            }
        }
    }

    private boolean isSuspended(int side) {
        return System.currentTimeMillis() < (side == BUY ? buySuspendUntil : sellSuspendUntil);
    }

    private int nextBootstrapSide(int orderCount) {
        boolean needBuy = !isSuspended(BUY) && count(BUY) < orderCount;
        boolean needSell = !isSuspended(SELL) && count(SELL) < orderCount;
        if (needBuy && needSell) {
            return count(BUY) <= count(SELL) ? BUY : SELL;
        }
        return needBuy ? BUY : (needSell ? SELL : 0);
    }

    private boolean isFilled(Order order) {
        try {
            String result = selectOrder(order.getOrderId());
            JSONObject json = judgeRes(result, "code", "selectOrder");
            if (json == null || json.getInt("code") != 200) {
                return false;
            }
            int status = json.getJSONObject("data").getInt("statusCode");
            // 保持旧策略对 2、3 状态的处理口径，视为不可继续撤销的异常成交状态。
            return status == 2 || status == 3;
        } catch (Exception e) {
            logger.info("查询变化单失败：" + order.getOrderId() + ", " + e.getMessage());
            return false;
        }
    }

    private void cancel(Order order) {
        try {
            String result = cancelTrade(order.getOrderId());
            logger.info("撤变化单 " + order.getOrderId() + "：" + result);
        } catch (Exception e) {
            logger.info("撤变化单失败：" + order.getOrderId() + ", " + e.getMessage());
        }
    }

    private void place(int side, Depth depth) {
        // 复用原有深度范围参数，前端展示名称调整为“变化区域”。
        int[] normalRange = rangeParam("depthOrderRange", 3, 12);
        int[] riskRange = rangeParam("depthRiskRange", 13, 15);
        boolean risk = side == BUY ? System.currentTimeMillis() < buyCooldownUntil
                : System.currentTimeMillis() < sellCooldownUntil;
        int[] range = risk ? riskRange : normalRange;
        List<List<String>> levels = side == BUY ? depth.bids : depth.asks;
        int from = Math.max(0, range[0] - 1);
        int to = Math.min(levels.size() - 1, range[1] - 1);
        if (from > to) {
            logger.info("盘口档位不足，跳过变化单：" + side);
            return;
        }

        BigDecimal tick = BigDecimal.ONE.movePointLeft(intParam("pricePrecision", 8));
        BigDecimal minSpread = tick.multiply(BigDecimal.valueOf(Math.max(1, intParam("depthMinSpreadTicks", 1))));
        BigDecimal price = new BigDecimal(levels.get(from + random.nextInt(to - from + 1)).get(0));
        price = safePrice(side, price, depth, minSpread).setScale(intParam("pricePrecision", 8), RoundingMode.HALF_UP);
        BigDecimal amount = getOrderAmount(exchange.get("depthOrderLowerLimit"), exchange.get("depthOrderTopLimit"),
                intParam("amountPrecision", 5));
        if (risk) {
            amount = amount.multiply(new BigDecimal("0.5"));
        }

        String result = submitOrder(side, price, amount);
        JSONObject json = judgeRes(result, "code", "submitTrade");
        if (json != null && json.getInt("code") == 200) {
            Order order = new Order();
            order.setType(side);
            order.setPrice(price);
            order.setAmount(amount);
            order.setOrderId(json.getJSONObject("data").getString("ID"));
            activeOrders.add(order);
            setTradeLog(id, "变化层挂" + (side == BUY ? "买" : "卖") + "单，价格=" + price + "，数量=" + amount
                    + (risk ? "，缓冲区" : "，展示区"), 0, side == BUY ? "05cbc8" : "ff6224");
        }
    }

    private BigDecimal safePrice(int side, BigDecimal price, Depth depth, BigDecimal minSpread) {
        BigDecimal bestBid = new BigDecimal(depth.bids.get(0).get(0));
        BigDecimal bestAsk = new BigDecimal(depth.asks.get(0).get(0));
        if (side == BUY) {
            return price.min(bestAsk.subtract(minSpread));
        }
        return price.max(bestBid.add(minSpread));
    }

    private Depth loadDepth() {
        try {
            String uri = "/v1/depth";
            Map<String, Object> params = new TreeMap<>();
            params.put("AccessKeyId", exchange.get("apikey"));
            params.put("SignatureVersion", 2);
            params.put("SignatureMethod", "HmacSHA256");
            params.put("Timestamp", new Date().getTime());
            params.put("symbol", exchange.get("market"));
            params.put("step", 3060);
            params.put("Signature", getSignature(exchange.get("tpass"), host, uri, "GET", params));
            com.alibaba.fastjson.JSONObject response = JSON.parseObject(httpUtil.get(baseUrl + uri + "?" + splicing(params)));
            if (response == null || response.getInteger("code") == null || response.getInteger("code") != 200) {
                return null;
            }
            com.alibaba.fastjson.JSONObject book = response.getJSONObject("data").getJSONObject("depth");
            Depth depth = new Depth();
            depth.bids = (List<List<String>>) book.get("bids");
            depth.asks = (List<List<String>>) book.get("asks");
            return depth.bids == null || depth.asks == null || depth.bids.isEmpty() || depth.asks.isEmpty() ? null : depth;
        } catch (Exception e) {
            logger.info("获取变化层盘口失败：" + e.getMessage());
            return null;
        }
    }

    private int count(int side) {
        int count = 0;
        for (Order order : activeOrders) if (order.getType() == side) count++;
        return count;
    }

    private int intParam(String key, int defaultValue) {
        try { return Integer.parseInt(exchange.getOrDefault(key, String.valueOf(defaultValue))); }
        catch (Exception e) { return defaultValue; }
    }

    private int[] rangeParam(String key, int from, int to) {
        try {
            String[] values = exchange.getOrDefault(key, from + "_" + to).split("_");
            return new int[]{Integer.parseInt(values[0]), Integer.parseInt(values[1])};
        } catch (Exception e) { return new int[]{from, to}; }
    }

    private long randomDelay() {
        int lower = Math.max(1, intParam("intervalLowerLimit", 2));
        int upper = Math.max(1, intParam("intervalTopLimit", 10));
        if (lower > upper) {
            int temp = lower;
            lower = upper;
            upper = temp;
        }
        return (lower + RandomUtils.nextInt(upper - lower + 1)) * 1000L;
    }
    private void sleep(long millis) { try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    private static class Depth {
        private List<List<String>> bids;
        private List<List<String>> asks;
    }

    private BigDecimal getOrderAmount(String min, String max, int precision) {
        BigDecimal low = new BigDecimal(min);
        BigDecimal high = new BigDecimal(max);
        if (low.compareTo(high) > 0) { BigDecimal temp = low; low = high; high = temp; }
        BigDecimal factor = BigDecimal.TEN.pow(precision);
        long lowValue = low.multiply(factor).longValue();
        long highValue = high.multiply(factor).longValue();
        long value = lowValue + (long) (random.nextDouble() * Math.max(1, highValue - lowValue + 1));
        return BigDecimal.valueOf(value).divide(factor, precision, RoundingMode.DOWN);
    }
}
