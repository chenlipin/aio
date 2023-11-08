
package top.suilian.aio.service.aatradeRobitService;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.dao.ApitradeLogMapper;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.model.ApitradeLog;
import top.suilian.aio.model.Member;
import top.suilian.aio.model.Robot;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.bibox.BiboxParentService;
import top.suilian.aio.service.bifinance.BifinanceParentService;
import top.suilian.aio.service.bision.BisionParentService;
import top.suilian.aio.service.bithumb.BithumbParentService;
import top.suilian.aio.service.bitmart.BitMartParentService;
import top.suilian.aio.service.bitrue.BitureParentService;
import top.suilian.aio.service.bitterex.BitterexParentService;
import top.suilian.aio.service.bkex.coinnoe.BkexParentService;
import top.suilian.aio.service.citex.CitexParentService;
import top.suilian.aio.service.coinnoe.CoinnoeParentService;
import top.suilian.aio.service.coinstore.CoinStoreParentService;
import top.suilian.aio.service.coinw.CoinwParentService;
import top.suilian.aio.service.digifinex.DigifinexParentService;
import top.suilian.aio.service.gate.GateParentService;
import top.suilian.aio.service.hoo.HooParentService;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.iex.IexParentService;
import top.suilian.aio.service.kucoin.KucoinParentService;
import top.suilian.aio.service.lbank.LbankParentService;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.mxc.MxcParentService;
import top.suilian.aio.service.ok.OkParentService;
import top.suilian.aio.service.skiesex.SkiesexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.service.whitebit.WhitebitParentService;
import top.suilian.aio.service.zb.ZbParentService;
import top.suilian.aio.service.zbg.ZbgParentService;
import top.suilian.aio.service.zg.ZGParentService;
import top.suilian.aio.vo.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class TradeRobotService {
    @Autowired
    RobotMapper robotMapper;
    @Qualifier("threadPoolTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private ApitradeLogMapper apitradeLogMapper;
    @Autowired
    HotCoinParentService hotCoinParentService;
    @Autowired
    RedisHelper redisHelper;

    Map<Integer, String> map = new ConcurrentHashMap<>();


    /**
     * 挂单接口
     *
     * @param req
     */
    public ResponseEntity trade(TradeReq req) throws UnsupportedEncodingException {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("用户身份校验失败");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature失败");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        Map<String, String> stringStringMap = robotAction.submitOrderStr(req.getType(), new BigDecimal(req.getPrice()), new BigDecimal(req.getAmount()));
        if (!"true".equals(stringStringMap.get("res"))) {
            throw new RuntimeException("挂单失败_" + stringStringMap.get("orderId"));
        }
        ApitradeLog apitradeLog = new ApitradeLog();
        apitradeLog.setAmount(new BigDecimal(req.getAmount()));
        apitradeLog.setPrice(new BigDecimal(req.getPrice()));
        apitradeLog.setRobotId(req.getRobotId());
        apitradeLog.setMemberId(req.getUserId());
        apitradeLog.setType(req.getType());
        apitradeLog.setTradeType(2);
        apitradeLog.setStatus(0);
        apitradeLog.setOrderId(stringStringMap.get("orderId"));
        apitradeLog.setCreatedAt(new Date());
        apitradeLogMapper.insert(apitradeLog);
        return ResponseEntity.success();

    }

    /**
     * 一键挂单
     *
     * @param req
     * @return
     */
    public ResponseEntity fastTrade(FastTradeReq req) {
        Member user = redisHelper.getUser(req.getToken());
//        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("用户身份校验失败");
//        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature失败");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        FastTradeM fastTradeM = new FastTradeM(req, robotAction);
        executor.execute(fastTradeM);
        return ResponseEntity.success();
    }

    /**
     * 根据机器人获取对应的操作类
     *
     * @param robotId
     * @return
     */
    public RobotAction getRobotAction(Integer robotId) {
        Robot robot = robotMapper.selectByPrimaryKey(robotId);
        RobotAction robotAction = null;
        if (robot == null) {
            throw new RuntimeException("机器人不存在");
        }
        switch (robot.getStrategyId()) {
            //loex
            case Constant.KEY_EXCHANGE_LOEX:
                robotAction = new LoexParentService();
                break;
            //hocoin
            case Constant.KEY_EXCHANGE_HOTCOIN:
                robotAction = hotCoinParentService;
                break;
            //BHEX
            case Constant.KEY_EXCHANGE_WBFEX:
                robotAction = new WbfexParentService();
                break;
            case Constant.KEY_EXCHANGE_BITMART:
                robotAction = new BitMartParentService();
                break;
            case Constant.KEY_EXCHANGE_COINSTORE:
                robotAction = new CoinStoreParentService();
                break;
            case Constant.KEY_EXCHANGE_MXC:
                robotAction = new MxcParentService();
                break;
            case Constant.KEY_EXCHANGE_ZG:
                robotAction = new ZGParentService();
                break;
            case Constant.KEY_EXCHANGE_BiSION:
                robotAction = new BisionParentService();
                break;
            case Constant.KEY_EXCHANGE_KUCOIN:
                robotAction = new KucoinParentService();
                break;
            case Constant.KEY_EXCHANGE_BITTEREX:
                robotAction = new BitterexParentService();
                break;
            case Constant.KEY_EXCHANGE_ZB:
                robotAction = new ZbParentService();
                break;
            case Constant.KEY_EXCHANGE_COINNOE:
                robotAction = new CoinnoeParentService();
                break;
            case Constant.KEY_EXCHANGE_ZBG:
                robotAction = new ZbgParentService();
                break;
            case Constant.KEY_EXCHANGE_BKEX:
                robotAction = new BkexParentService();
                break;
            case Constant.KEY_EXCHANGE_bithumb:
                robotAction = new BithumbParentService();
                break;
            case Constant.KEY_EXCHANGE_SKIESEX:
                robotAction = new SkiesexParentService();
                break;
            case Constant.KEY_EXCHANGE_HOO:
                robotAction = new HooParentService();
                break;
            case Constant.KEY_EXCHANGE_BITRUE:
                robotAction = new BitureParentService();
                break;
            case Constant.KEY_EXCHANGE_DIGIFINEX:
                robotAction = new DigifinexParentService();
                break;
            case Constant.KEY_EXCHANGE_BIBOX:
                robotAction = new BiboxParentService();
                break;
            case Constant.KEY_EXCHANGE_Citex:
                robotAction = new CitexParentService();
                break;
            case Constant.KEY_EXCHANGE_LBANK:
                robotAction = new LbankParentService();
                break;
            case Constant.KEY_EXCHANGE_WHITEBIT:
                robotAction = new WhitebitParentService();
                break;
            case Constant.KEY_EXCHANGE_IEX:
                robotAction = new IexParentService();
                break;
            case Constant.KEY_EXCHANGE_GATE:
                robotAction = new GateParentService();
                break;
            case Constant.KEY_EXCHANGE_BIFINANCE:
                robotAction = new BifinanceParentService();
                break;
            case Constant.KEY_EXCHANGE_COINW:
                robotAction=new CoinwParentService();
                break;
            case Constant.KEY_EXCHANGE_OK:
                robotAction=new OkParentService();
                break;
            default:
                return null;
        }
        robotAction.setParam(robotId);
        return robotAction;
    }

    public void cancalfastTrade(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("用户身份校验失败");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature失败");
        }
        map.remove(req.getRobotId());
    }

    public String fastTradestatus(CancalAllOrder req) {
        if (map.get(req.getRobotId()) == null) {
            return "停止中";
        }
        return "运行中";
    }

    /**
     * 查询订单
     *
     * @param req
     * @return
     */
    public List<getAllOrderPonse> getAllOrder(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("用户身份校验失败");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature失败");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        List<getAllOrderPonse> list = apitradeLogMapper.selectByRobotId(req.getRobotId());
        Map<String, Integer> map = robotAction.selectOrderStr(list.stream().filter(e -> e.getStatus().equals(0) || e.getStatus().equals(1)).map(getAllOrderPonse::getOrderId).collect(Collectors.joining(",", "", "")));
        for (getAllOrderPonse order : list) {
            if (order.getStatus().equals(0) || order.getStatus().equals(1)) {
                if (map.get(order.getOrderId()) != null) {
                    if (!order.getStatus().equals(map.get(order.getOrderId()))) {
                        ApitradeLog apitradeLog = apitradeLogMapper.selectByRobotIdAndOrderId(req.getRobotId(), order.getOrderId());
                        apitradeLog.setStatus(map.get(order.getOrderId()));
                        apitradeLog.setUpdatedAt(new Date());
                        apitradeLogMapper.updateByPrimaryKey(apitradeLog);
                        order.setStatus(map.get(order.getOrderId()));
                    } else {
                        ApitradeLog apitradeLog = apitradeLogMapper.selectByRobotIdAndOrderId(req.getRobotId(), order.getOrderId());
                        apitradeLog.setUpdatedAt(new Date());
                        apitradeLogMapper.updateByPrimaryKey(apitradeLog);
                        order.setStatus(map.get(order.getOrderId()));
                    }
                } else {
                    ApitradeLog apitradeLog = apitradeLogMapper.selectByRobotIdAndOrderId(req.getRobotId(), order.getOrderId());
                    apitradeLog.setStatus(-1);
                    apitradeLog.setUpdatedAt(new Date());
                    apitradeLogMapper.updateByPrimaryKey(apitradeLog);
                    order.setStatus(-1);
                }
            }
        }
        return list;
    }

    /**
     * 一键撤单
     *
     * @param req
     */
    public ResponseEntity cancalAllOrder(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("用户身份校验失败");
        }

        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature失败");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        FastCancalTradeM fastCancalTradeM = new FastCancalTradeM(robotAction, req);
        executor.execute(fastCancalTradeM);
        return ResponseEntity.success();

    }


    /**
     * 根据订单号撤单
     *
     * @param req
     * @return
     */
    public void cancalByOrderId(CancalOrderReq req) {
        Member user = redisHelper.getUser(req.getToken());
//        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("用户身份校验失败");
//        }
//        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
//        if (!checkSignature) {
////            throw new RuntimeException("Signature失败");
//        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
//        ApitradeLog apitradeLog = apitradeLogMapper.selectByRobotIdAndOrderId(req.getRobotId(), req.getOrderId());
//        if (!apitradeLog.getStatus().equals(TradeEnum.CANCEL.getStatus())) {
            String str = robotAction.cancelTradeStr(req.getOrderId());
            if (!"true".equals(str)) {
                throw new RuntimeException(ResultCode.ERROR.getMessage());
            }
//            apitradeLog.setStatus(-1);
//            apitradeLog.setUpdatedAt(new Date());
//            apitradeLogMapper.updateByPrimaryKey(apitradeLog);
        }



    public String getReplenish(CancalAllOrder req) {
//        Member user = redisHelper.getUser(req.getToken());
//        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("用户身份校验失败");
//        }
        String maxLeftQty = redisHelper.get("maxLeftQty_" + req.getRobotId()) == null ? "0" : redisHelper.get("maxLeftQty_" + req.getRobotId());
        String maxRightQty = redisHelper.get("maxRightQty_" + req.getRobotId()) == null ? "0" : redisHelper.get("maxRightQty_" + req.getRobotId());

        return maxLeftQty + "_" + maxRightQty;
    }

    public void clean(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("用户身份校验失败");
        }
        if (req.getType().equals("left")) {
            redisHelper.remove("maxLeftQty_" + req.getRobotId());
        } else {
            redisHelper.remove("maxRightQty_" + req.getRobotId());
        }
    }


    /**
     * 一键挂单核心逻辑
     */
    class FastTradeM implements Runnable {
        FastTradeReq fastTradeReq;
        RobotAction robotAction;

        public FastTradeM(FastTradeReq fastTradeReq, RobotAction robotAction) {
            this.fastTradeReq = fastTradeReq;
            this.robotAction = robotAction;
        }

        @Override
        public void run() {
            if ("运行中".equals(map.get(fastTradeReq.getRobotId()))) {
                throw new RuntimeException("已经有一个任务正在运行中");
            }
            map.put(fastTradeReq.getRobotId(), "运行中");
            //获取机器人参数
            Map<String, String> param = robotAction.getParam();
            //当前已经挂买单个数
            int newBuyOrder = 0;
            //当前已经挂卖单个数
            int newSellOrder = 0;
            int timeChange = fastTradeReq.getMaxTime() - fastTradeReq.getMinTime();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            boolean first = true;
            while ((newBuyOrder + newSellOrder) < (fastTradeReq.getBuyOrdermun() + fastTradeReq.getSellOrdermun()) && "运行中".equals(map.get(fastTradeReq.getRobotId()))) {
                //当基础买卖价为null就去拿盘口价格
                if (fastTradeReq.getBuyorderBasePrice() == null || fastTradeReq.getBuyorderBasePrice() <= 0 ||
                        fastTradeReq.getSellorderBasePrice() == null || fastTradeReq.getSellorderBasePrice() <= 0) {
                    String sellPriceStr = redisHelper.get("kile_sell_" + fastTradeReq.getRobotId());

                    String buyPriceStr = redisHelper.get("kile_buy_" + fastTradeReq.getRobotId());

                    if (StringUtils.isEmpty(buyPriceStr)) {
                        Double sellPrice = Double.valueOf(sellPriceStr);
                        fastTradeReq.setSellorderBasePrice(sellPrice);
                    }
                    if (StringUtils.isEmpty(buyPriceStr)) {
                        Double buyPrice = Double.valueOf(buyPriceStr);
                        fastTradeReq.setBuyorderBasePrice(buyPrice);
                    }
                }
                //计算挂单数量
                Double amountPrecision = RandomUtilsme.getRandom(fastTradeReq.getMaxAmount() - fastTradeReq.getMinAmount(), Integer.parseInt(param.get("amountPrecision")));
                BigDecimal amount = new BigDecimal(fastTradeReq.getMinAmount() + amountPrecision).setScale(Integer.parseInt(param.get("amountPrecision")), BigDecimal.ROUND_HALF_UP);

                /**
                 * 买单区间差价
                 */
                double buyRange = fastTradeReq.getBuyorderRangePrice1() - fastTradeReq.getBuyorderRangePrice();
                /**
                 * 买单一个区间差价
                 */
                double buyOneRange = buyRange / fastTradeReq.getBuyOrdermun();
                /**
                 * 卖单区间差价
                 */
                double sellRange = fastTradeReq.getSellorderRangePrice1() - fastTradeReq.getSellorderRangePrice();
                /**
                 * 卖单一个区间差价
                 */
                double sellOneRange = buyRange / fastTradeReq.getBuyOrdermun();

                //决定是挂买单还是卖单
                boolean type = true;
                boolean typeTrade = true;
                BigDecimal price = BigDecimal.ZERO;
                if (type && newBuyOrder < fastTradeReq.getBuyOrdermun()) {
                    //计算买价
                    Double pricePrecision = RandomUtilsme.getRandom(buyOneRange, Integer.parseInt(param.get("pricePrecision")));
                    Double pricePrecision1 = (fastTradeReq.getBuyorderRangePrice() + pricePrecision + buyOneRange * newBuyOrder);
                    price = new BigDecimal(fastTradeReq.getBuyorderBasePrice() - pricePrecision1).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);
                    //挂买单
                    newBuyOrder++;
                } else {
                    //计算卖价
                    Double pricePrecision = RandomUtilsme.getRandom(sellOneRange, Integer.parseInt(param.get("pricePrecision")));
                    Double pricePrecision1 = (fastTradeReq.getSellorderRangePrice() + pricePrecision + sellOneRange * newSellOrder);
                    price = new BigDecimal(fastTradeReq.getSellorderBasePrice() + pricePrecision1).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);
                    //挂卖单
                    typeTrade = false;
                    newSellOrder++;
                }
                Map<String, String> stringStringMap = robotAction.submitOrderStr(typeTrade ? 1 : 2, price, amount);
                ApitradeLog apitradeLog = new ApitradeLog();
                apitradeLog.setAmount(amount);
                apitradeLog.setPrice(price);
                apitradeLog.setRobotId(fastTradeReq.getRobotId());
                apitradeLog.setMemberId(fastTradeReq.getUserId());
                apitradeLog.setType(type ? 1 : 2);
                apitradeLog.setTradeType(1);
                apitradeLog.setStatus(0);
                apitradeLog.setMemo(uuid);
                apitradeLog.setOrderId(stringStringMap.get("orderId"));
                apitradeLog.setCreatedAt(new Date());
                if (first) {
                    apitradeLog.setMemo(uuid + "_" + JSON.toJSONString(fastTradeReq));
                    first = false;
                }
                apitradeLogMapper.insert(apitradeLog);
                //挂单间隔时间
                int randomTime = fastTradeReq.getMinTime() + RandomUtils.nextInt(timeChange == 0 ? 1 : timeChange);
                try {
                    Thread.sleep(randomTime * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            map.remove(fastTradeReq.getRobotId());
        }
    }


    /**
     * 一键撤单核心逻辑
     */
    class FastCancalTradeM implements Runnable {
        RobotAction robotAction;
        CancalAllOrder cancalAllOrder;

        public FastCancalTradeM(RobotAction robotAction, CancalAllOrder cancalAllOrder) {
            this.robotAction = robotAction;
            this.cancalAllOrder = cancalAllOrder;
        }

        @Override
        public void run() {
            //获取机器人挂的单而且没成交的单子
            List<ApitradeLog> apitradeLogs = apitradeLogMapper.selectByRobotIdNOTrade(cancalAllOrder.getRobotId());
            //单号集合
            apitradeLogs.forEach(apitradeLog -> {
                String result = robotAction.cancelTradeStr(apitradeLog.getOrderId());
                if ("ok".equals(result)) {
                    apitradeLog.setStatus(-1);
                    apitradeLog.setUpdatedAt(new Date());
                    apitradeLogMapper.updateByPrimaryKeySelective(apitradeLog);
                }
            });

        }
    }


    public boolean checkSignature(JSONObject jsonObject, String signature) {
        Map treeMap = JSONObject.toJavaObject(jsonObject, Map.class);
        TreeMap<String, Object> treeMap1 = new TreeMap<>(treeMap);
        treeMap1.remove("signature");
        String toString = keySortToString(treeMap1);
        String md5String = getMD5String(toString + "_mimicat1278hdbCsLuf");
        if (!md5String.equals(signature)) {
            return false;
        }
        return true;
    }

    public String keySortToString(TreeMap<String, Object> params) {
        String str = "";
        Iterator<Map.Entry<String, Object>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            str += entry.getKey() + "=" + entry.getValue() + "&";
        }
        return str.substring(0, str.length() - 1);
    }

    /**
     * MD5加密
     */
    public String getMD5String(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
