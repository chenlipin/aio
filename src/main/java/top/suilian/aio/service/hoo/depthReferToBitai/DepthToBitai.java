package top.suilian.aio.service.hoo.depthReferToBitai;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONTokener;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.hoo.HooParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DepthToBitai extends HooParentService {

    public DepthToBitai(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_HOO_REFER_DEPTH, id);
    }

    private boolean start = true;
    private String nearId = null;
    private String referBaseUrl = "https://api.bitai.io";

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


        //获取bitai深度
        String trades = httpUtil.get("https://api.bitai.io/openapi/quote/v1/depth?symbol=" + exchange.get("refersymbol"));
        JSONObject tradesObj = judgeRes(trades, "time", "getRandomPrice");
        if (tradesObj != null && tradesObj.getString("time") != null) {

            //   获取对标买单
            List<List<String>> buyPrices = tradesObj.getJSONArray("bids");
            logger.info("对标买单:" + buyPrices);
            logger.info("对标买单数量：" + buyPrices.size());
            //   获取对标卖单
            List<List<String>> sellPrices = tradesObj.getJSONArray("asks");
            logger.info("对标卖单:" + sellPrices);
            logger.info("对标卖单数量：" + sellPrices.size());

            //获取自己的挂单
            String tradeOrders = getTradeOrders();
            logger.info("自己委托中的单:" + tradeOrders);
            List<JSONObject> ownBuyOrders = new ArrayList<>();
            List<JSONObject> ownSellOrders = new ArrayList<>();
            JSONObject oederJson=JSONObject.fromObject(tradeOrders);
            JSONArray showOrderList = oederJson.getJSONArray("data");

            if (showOrderList != null && showOrderList.size() > 0) {

                for (int i = 0; i < showOrderList.size(); i++) {
                    JSONObject trade = showOrderList.getJSONObject(i);

                    if (trade.getInt("side")==1) {
                        //获取 自己的买单
                        ownBuyOrders.add(trade);
                    } else if (trade.getInt("side")==-1) {
                        //获取 自己的卖单
                        ownSellOrders.add(trade);
                    }
                }
                logger.info("当前买单数:" + ownBuyOrders.size());
                logger.info("当前卖单数:" + ownSellOrders.size());
            }


            /**
             *  如果自己有，对标有，就不动
             * 	如果自己有，对标没有，就取消这个挂单
             * 	如果自己没有，对标有，就增加这个价格的挂单
             */

            //遍历 对标 卖单

            List<List<String>> neadSell = new ArrayList<>();
            List<String> notNeadCancel = new ArrayList<>();
            //遍历平台的卖单
            for (int i = 0; i < sellPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String sellPrice = sellPrices.get(i).get(0);
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
                        //不需要扯的单
                        notNeadCancel.add(price);
                        break;


                    }
                }
                if (!has && new BigDecimal(sellPrice).compareTo(BigDecimal.ZERO) > 0) {
                    List<String> amountAndPrice = new ArrayList<>();
                    amountAndPrice.add((sellPrice));
                    amountAndPrice.add(String.valueOf(sellAmount));
                    //平台有，自己没有，需要卖的单
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
                String tradeId = ownSellOrders.get(i).getString("order_id");
                String tradeNo = ownSellOrders.get(i).getString("trade_no");
                String price = ownSellOrders.get(i).getString("price");

                boolean has = false;

                for (int j = 0; j < notNeadCancel.size(); j++) {
                    if (new BigDecimal(price).compareTo(new BigDecimal(notNeadCancel.get(j))) == 0) {
                        has = true;
                        break;
                    }
                }

                if (!has) {
                    String Result = selectOrder(tradeId);
                    sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                    Object object = new JSONTokener(Result).nextValue();
                    if (object instanceof JSONObject) {
                        JSONObject resultJson = JSONObject.fromObject(Result);
                        JSONObject dataJson = resultJson.getJSONObject("data");
                        if (dataJson.getInt("status")==2||dataJson.getInt("status")==3) {
                            String res = cancelTrade(tradeId,tradeNo);
                            sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                            JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                            setTradeLog(id, "对标深度卖单撤单[" + tradeId + "]=>" + res, 0, "000000");
                            logger.info("撤[" + price + "]---" + res);
                        }
                    } else {
                        logger.info("查询订单失败：" + tradeId + "失败=======》" + Result);
                    }
                }

            }

            logger.info("对标卖单结束------------------------------------");

            List<List<String>> neadBuy = new ArrayList<>();
            List<String> notNeadCancelBuy = new ArrayList<>();
            //遍历对标平台的买单
            for (int i = 0; i < buyPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String buyPrice = buyPrices.get(i).get(0);
                String buyAmount = String.valueOf(buyPrices.get(i).get(1));
                boolean has = false;

                for (int j = 0; j < ownBuyOrders.size(); j++) {
                    String ownBuyPrice = ownBuyOrders.get(j).getString("price");
                    BigDecimal compoBuyPrice = nN(new BigDecimal(buyPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    BigDecimal compoOwnBuy = nN(new BigDecimal(ownBuyPrice), Integer.valueOf(precision.get("pricePrecision").toString()));
                    //对标卖单和自己卖单相等  移除 对标单中 和自己  的该笔订单
                    if (compoBuyPrice.compareTo(compoOwnBuy) == 0) {
                        String price = ownBuyOrders.get(j).getString("price");
                        //不需要撤的买单
                        notNeadCancelBuy.add(price);
                        has = true;
                        break;
                    }
                }
                if (!has && new BigDecimal(buyPrice).compareTo(BigDecimal.ZERO) > 0) {
                    List<String> amountAndPrice = new ArrayList<>();
                    amountAndPrice.add((buyPrice));
                    amountAndPrice.add(String.valueOf(buyAmount));
                    //平台有，自己没有，需要挂买单
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
                String tradeId = ownBuyOrders.get(i).getString("order_id");
                String tradeNo = ownBuyOrders.get(i).getString("trade_no");
                String price = ownBuyOrders.get(i).getString("price");
                boolean has = false;

                for (int j = 0; j < notNeadCancelBuy.size(); j++) {
                    if (new BigDecimal(price).compareTo(new BigDecimal(notNeadCancelBuy.get(j))) == 0) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    String Result = selectOrder(tradeId);
                    sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                    Object object = new JSONTokener(Result).nextValue();
                    if (object instanceof JSONObject) {
                        JSONObject resultJson = JSONObject.fromObject(Result);
                        JSONObject dadaJson = resultJson.getJSONObject("data");
                        if (dadaJson.getInt("status")==2||dadaJson.getInt("status")==3) {
                            String res = cancelTrade(tradeId,tradeNo);
                            sleep(200, Integer.parseInt(exchange.get("isMobileSwitch")));
                            JSONObject cancelRes = judgeRes(res, "code", "cancelReferTrade");
                            setCancelOrder(cancelRes, res, tradeId, Constant.KEY_CANCEL_ORDER_TYPE_REFER_DEPTH);
                            setTradeLog(id, "对标深度卖单撤单[" + tradeId + "]=>" + res, 0, "000000");
                            logger.info("撤[" + price + "]---" + res);
                        }
                    } else {
                        logger.info("查询订单失败：" + tradeId + "失败=======》" + Result);
                    }
                }

            }
            logger.info("对标买单结束------------------------------------");
            neadBuy.clear();
            neadSell.clear();
            notNeadCancel.clear();
            notNeadCancelBuy.clear();
        }


        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (
                UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();
        sleep(3000, Integer.parseInt(exchange.get("isMobileSwitch")));

    }
}
