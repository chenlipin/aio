package top.suilian.aio.service.wbfex.depthReferToLoex;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.wbfex.WbfexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepthToLoex extends WbfexParentService {

    public DepthToLoex(
            CancelExceptionService cancelExceptionService,
            CancelOrderService cancelOrderService,
            ExceptionMessageService exceptionMessageService,
            RobotArgsService robotArgsService,
            RobotLogService robotLogService,
            RobotService robotService,
            TradeLogService tradeLogService,
            HttpUtil httpUtil,
            RedisHelper redisHelper,
            int id
    ) {
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_WBFEX_REFER_DEPTH, id);
    }

    private boolean start = true;
    private String nearId = null;
    private String referBaseUrl = "https://openapi.loex.io";

    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            if (!setPrecision()) {
                return;
            }
            logger.info("设置机器人交易规则结束");
            start = false;
        }

        //获取loex深度
        String trades = httpUtil.get("https://openapi.loex.io/open/api/market_dept?symbol=" + exchange.get("market") + "&type=step0");
        JSONObject tradesObj = judgeRes(trades, "code", "getRandomPrice");
        if (tradesObj != null && tradesObj.getInt("code") == 0) {
            JSONObject data = tradesObj.getJSONObject("data");

            JSONObject tick = data.getJSONObject("tick");
            //   获取对标买单
            List<List<String>> buyPrices = (List<List<String>>) tick.get("bids");
            logger.info("对标买单:" + buyPrices);
            //   获取对标卖单
            List<List<String>> sellPrices = (List<List<String>>) tick.get("asks");
            logger.info("对标卖单:" + sellPrices);

            //获取设置的买一卖一的值
            BigDecimal buyOneLimit = new BigDecimal(exchange.get("buyOneLimit"));
            BigDecimal sellOneLimit = new BigDecimal(exchange.get("sellOneLimit"));
            //获取对标平台的卖一买一
            BigDecimal buyOne = new BigDecimal(String.valueOf(buyPrices.get(0).get(0)));
            BigDecimal sellOne = new BigDecimal(String.valueOf(sellPrices.get(0).get(0)));
            //判断买一买一是否满足对标要求
            if (buyOneLimit != null && sellOneLimit != null) {
                if (buyOneLimit != BigDecimal.ZERO && sellOneLimit != BigDecimal.ZERO) {
                    if (buyOneLimit.compareTo(sellOneLimit) < 0) {
                        if (buyOne.compareTo(buyOneLimit) >= 0 && sellOne.compareTo(sellOneLimit) <= 0) {
                            logger.info("当前盘口正常，买一限价：" + buyOneLimit + ",卖一限价：" + sellOneLimit);
                        } else {
                            setTradeLog(id, "对标盘口不在预期盘口内，机器人停止，买一限价：" + buyOneLimit + ",卖一限价：" + sellOneLimit, 0, "000000");
                            setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                            return;
                        }
                    } else {
                        setTradeLog(id, "参数设置有误：买价大于卖价", 0, "000000");
                        setRobotStatus(id, Constant.KEY_ROBOT_STATUS_OUT);
                        return;
                    }
                } else {
                    logger.info("未启用买卖上下限功能");
                }
            } else {
                logger.info("未启用买卖上下限功能");
            }


            //获取自己的挂单
            String tradeOrders = getTradeOrders();
            logger.info("自己的挂单:" + tradeOrders);

            List<JSONObject> ownBuyOrders = new ArrayList<>();
            List<JSONObject> ownSellOrders = new ArrayList<>();
            JSONObject jsonOrders = judgeRes(tradeOrders, "code", "getTradeOrders");
            if (jsonOrders != null && jsonOrders.getInt("code") == 0) {


                JSONObject dataList = jsonOrders.getJSONObject("data");
                if (!dataList.getString("resultList").equals("null")) {
                    JSONArray resultList = dataList.getJSONArray("resultList");

                    for (int i = 0; i < resultList.size(); i++) {
                        JSONObject trade = resultList.getJSONObject(i);

                        if (trade.getString("side").equals("BUY")) {
                            //获取 自己的买单
                            ownBuyOrders.add(trade);
                        } else if (trade.getString("side").equals("SELL")) {
                            //获取 自己的卖单
                            ownSellOrders.add(trade);
                        }
                    }
                    logger.info("当前买单数:" + ownBuyOrders.size());
                    logger.info("当前卖单数:" + ownSellOrders.size());
                }

            }

            /**
             *  如果自己有，对标有，就不动
             * 	如果自己有，对标没有，就取消这个挂单
             * 	如果自己没有，对标有，就增加这个价格的挂单
             */

            //遍历 对标 卖单

            List<List<String>> neadSell = new ArrayList<>();
            List<String> notNeadCancel = new ArrayList<>();
            for (int i = 0; i < sellPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String sellPrice = String.valueOf(sellPrices.get(i).get(0));

                String sellAmount = String.valueOf(sellPrices.get(i).get(1));

                boolean has = false;

                for (int j = 0; j < ownSellOrders.size(); j++) {
                    String ownSellPrice = ownSellOrders.get(j).getString("price");

                    BigDecimal compoSellPrice = nN(new BigDecimal(sellPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    BigDecimal compoOwnSell = nN(new BigDecimal(ownSellPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    //对标卖单和自己卖单相等  移除 对标单中 和自己  的该笔订单
                    if (compoSellPrice.compareTo(compoOwnSell) == 0) {
                        has = true;
                        String price = ownSellOrders.get(j).getString("price");
                        notNeadCancel.add(price);
                        break;
                    }
                }
                if (!has && new BigDecimal(sellPrice).compareTo(BigDecimal.ZERO) > 0) {
                    List<String> amountAndPrice = new ArrayList<>();
                    amountAndPrice.add((sellPrice));
                    amountAndPrice.add(String.valueOf(sellAmount));
                    neadSell.add(amountAndPrice);
                }

            }


            // 挂 卖单
            int sellNum;
            if (neadSell.size() > Integer.parseInt(exchange.get("depthNum"))) {
                sellNum = Integer.parseInt(exchange.get("depthNum"));
            } else {
                sellNum = neadSell.size();
            }

            for (int i = 0; i < sellNum; i++) {
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                BigDecimal finalSellPrice = nN(new BigDecimal(String.valueOf(neadSell.get(i).get(0))), Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalSellAmount = nN(new BigDecimal(String.valueOf(neadSell.get(i).get(1))).multiply(new BigDecimal(exchange.get("depthRate"))), Integer.parseInt(precision.get("amountPrecision").toString()));

                submitOrder(2, finalSellPrice, finalSellAmount);
            }

            //撤销订单
            for (int i = 0; i < ownSellOrders.size(); i++) {
                String tradeId = ownSellOrders.get(i).getString("id");
                String price = ownSellOrders.get(i).getString("price");

                boolean has = false;

                for (int j = 0; j < notNeadCancel.size(); j++) {
                    if (new BigDecimal(price).compareTo(new BigDecimal(notNeadCancel.get(j))) == 0) {
                        has = true;
                        break;
                    }
                }

                if (!has) {
                    String res = cancelTrade(tradeId);
                    sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                    JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                    setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                    setTradeLog(id, "对标深度卖单撤单[" + price + "]=>" + res, 0, "000000");
                }

            }


            List<List<String>> neadBuy = new ArrayList<>();
            List<String> notNeadCancelBuy = new ArrayList<>();
            for (int i = 0; i < buyPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String buyPrice = String.valueOf(buyPrices.get(i).get(0));

                String buyAmount = String.valueOf(buyPrices.get(i).get(1));

                boolean has = false;

                for (int j = 0; j < ownBuyOrders.size(); j++) {
                    String ownBuyPrice = ownBuyOrders.get(j).getString("price");

                    BigDecimal compoBuyPrice = nN(new BigDecimal(buyPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    BigDecimal compoOwnBuy = nN(new BigDecimal(ownBuyPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    //对标卖单和自己卖单相等  移除 对标单中 和自己  的该笔订单
                    if (compoBuyPrice.compareTo(compoOwnBuy) == 0) {
                        has = true;
                        String price = ownBuyOrders.get(j).getString("price");
                        notNeadCancelBuy.add(price);
                        break;
                    }
                }
                if (!has && new BigDecimal(buyPrice).compareTo(BigDecimal.ZERO) > 0) {
                    List<String> amountAndPrice = new ArrayList<>();
                    amountAndPrice.add((buyPrice));
                    amountAndPrice.add(String.valueOf(buyAmount));
                    neadBuy.add(amountAndPrice);
                }

            }


            // 挂 买单
            int buyNum;
            if (neadBuy.size() > Integer.parseInt(exchange.get("depthNum"))) {
                buyNum = Integer.parseInt(exchange.get("depthNum"));
            } else {
                buyNum = neadBuy.size();
            }

            for (int i = 0; i < buyNum; i++) {
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                BigDecimal finalBuyPrice = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(0))), Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalBuyAmount = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(1))).multiply(new BigDecimal(exchange.get("depthRate"))), Integer.parseInt(precision.get("amountPrecision").toString()));

                submitOrder(1, finalBuyPrice, finalBuyAmount);
            }

            //撤销订单
            for (int i = 0; i < ownBuyOrders.size(); i++) {
                sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                String tradeId = ownBuyOrders.get(i).getString("id");
                String price = ownBuyOrders.get(i).getString("price");

                boolean has = false;


                for (int j = 0; j < notNeadCancelBuy.size(); j++) {
                    if (new BigDecimal(price).compareTo(new BigDecimal(notNeadCancelBuy.get(j))) == 0) {
                        has = true;
                        break;
                    }
                }

                if (!has) {
                    String res = cancelTrade(tradeId);
                    JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                    setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                    setTradeLog(id, "对标深度买单撤单[" + price + "]=>" + res, 0, "000000");
                }

            }
            neadBuy.clear();
            neadSell.clear();
            notNeadCancel.clear();
            notNeadCancelBuy.clear();
        }

        clearLog();
        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sleep(3000, Integer.parseInt(exchange.get("isMobileSwitch")));

    }
}
