package top.suilian.aio.service.euexReferbibox.depthReferToBibox;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.euexReferbibox.EuexReferBiboxParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EuexReferDepthToBibox extends EuexReferBiboxParentService {
    public EuexReferDepthToBibox(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_EUEXREFERBIBOX_REFER_DEPTH, id);
    }

    private boolean start = true;
    private String nearId = null;
    private String referBaseUrl = "https://api.bibox.com";

    public void init() throws UnsupportedEncodingException {

        if (start) {
            logger.info("设置机器人参数开始");
            setParam();
            logger.info("设置机器人参数结束");

            logger.info("设置机器人交易规则开始");
            setPrecision();
            logger.info("设置机器人交易规则结束");
            start = false;
        }

        //获取bibox深度
        String trades = httpUtil.get(referBaseUrl + "/v1/mdata?cmd=depth&pair="+exchange.get("market").toUpperCase()+"&size=20");
        JSONObject tradesObj = judgeRes(trades, "result", "getdepth");

        if (tradesObj!=null && tradesObj.getJSONObject("result") != null) {
            JSONObject data = tradesObj.getJSONObject("result");
            JSONArray bids=data.getJSONArray("bids");
            JSONArray asks=data.getJSONArray("asks");
            List<JSONObject> buyPrices=new ArrayList<>();

            for (int i=0;i<bids.size();i++) {
                buyPrices.add(bids.getJSONObject(i));
            }

            logger.info("对标买单:" + buyPrices);

            List<JSONObject> sellPrices=new ArrayList<>();
            for (int i=0;i<asks.size();i++) {
                sellPrices.add(asks.getJSONObject(i));
            }
            logger.info("对标卖单:" + sellPrices);


            //获取自己的挂单
            System.out.println(exchange);
            String tradeOrders = getTradeOrders(0);
            logger.info("自己的挂买单:" + tradeOrders);





            /*-----------------------------------------------------------------------------*/


            List<JSONObject> ownBuyOrders = new ArrayList<>();
            List<JSONObject> ownSellOrders = new ArrayList<>();
            JSONObject jsonOrders = judgeRes(tradeOrders, "code", "getTradeOrders");
            if (jsonOrders != null && jsonOrders.getString("code").equals("0000")) {


                JSONArray resultList = jsonOrders.getJSONArray("data");

                for (int i = 0; i < resultList.size(); i++) {
                    JSONObject trade = resultList.getJSONObject(i);

                    if (trade.getString("type").equals("0")) {
                        //获取 自己的买单
                        ownBuyOrders.add(trade);
                    } else if (trade.getString("type").equals("1")) {
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
            for (int i = 0; i < sellPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String sellPrice = String.valueOf(sellPrices.get(i).getString("price"));

                String sellAmount = String.valueOf(sellPrices.get(i).getString("volume"));

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
                sleep(1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                BigDecimal finalSellPrice = nN(new BigDecimal(String.valueOf(neadSell.get(i).get(0))), Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalSellAmount = nN(new BigDecimal(String.valueOf(neadSell.get(i).get(1))).multiply(new BigDecimal(exchange.get("depthRate"))), Integer.parseInt(precision.get("amountPrecision").toString()));

                submitOrder(1, finalSellPrice, finalSellAmount);
//                setTradeLog(id, "对标挂卖单amount:" + finalSellAmount + "--price:" + finalSellPrice, 0, "ff6224");

            }

            //撤销订单
            for (int i = 0; i < ownSellOrders.size(); i++) {
                sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
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
                    judgeCancel(tradeId);
                }

            }


            /**
             * 遍历 对标 买单
             */

            List<List<String>> neadBuy = new ArrayList<>();
            List<String> notNeadCancelBuy = new ArrayList<>();
            for (int i = 0; i < buyPrices.size(); i++) {
                if (i == Integer.parseInt(exchange.get("depthNum"))) {
                    break;
                }
                //获取价格
                String buyPrice = String.valueOf(buyPrices.get(i).getString("price"));

                String buyAmount = String.valueOf(buyPrices.get(i).getString("volume"));

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
                sleep(1000, Integer.parseInt(exchange.get("isMobileSwitch")));
                BigDecimal finalBuyPrice = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(0))), Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalBuyAmount = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(1))).multiply(new BigDecimal(exchange.get("depthRate"))), Integer.parseInt(precision.get("amountPrecision").toString()));

                submitOrder(0, finalBuyPrice, finalBuyAmount);
//                setTradeLog(id, "对标挂买单", 0, "05cbc8");
                //ff6224
            }

            //撤销订单
            for (int i = 0; i < ownBuyOrders.size(); i++) {
                sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
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
                    judgeCancel(tradeId);
                }

            }
            neadBuy.clear();
            neadSell.clear();
            notNeadCancel.clear();
            notNeadCancelBuy.clear();
        }


        logger.info("--------------------------------------------结束---------------------------------------------");
        try {
            setBalanceRedis();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        clearLog();
        sleep(3000, Integer.parseInt(exchange.get("isMobileSwitch")));

    }
}
