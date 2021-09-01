package top.suilian.aio.service.laex.depthReferToZg;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.refer.BiboxUtils;
import top.suilian.aio.refer.GateUtils;
import top.suilian.aio.refer.HuoBiUtils;
import top.suilian.aio.refer.ZgUtils;
import top.suilian.aio.service.*;
import top.suilian.aio.service.laex.LaexParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

public class DepthToZg extends LaexParentService {

    public DepthToZg(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_LAEX_DEPTH, id);
    }

    private boolean start = true;
    public List<List<String>> buyPrices=new ArrayList<>() ;       //买单存放集合
    public List<List<String>> sellPrices=new ArrayList<>() ;     //卖单存放集合
    public HashMap<String, Map<String,String>> depth = new HashMap<>();
    Map<String,String> bids= new LinkedHashMap<>();
    Map<String,String> asks=new LinkedHashMap<>();

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



        if ("0".equals(exchange.get("referexchange"))) {//获取zg深度
           depth= ZgUtils.getDepth(exchange.get("refersymbol"));

        }else if("1".equals(exchange.get("referexchange"))){//获取火币的深度
           depth=HuoBiUtils.getDepth(exchange.get("refersymbol"));


        } else if("2".equals(exchange.get("referexchange"))){//获取Bibox的深度
            depth= BiboxUtils.getDepth(exchange.get("refersymbol"));

        }else if("3".equals(exchange.get("referexchange"))){//获取gata.io平台深度
            depth= GateUtils.getDepth(exchange.get("refersymbol"));
        }

        if(depth!=null&&depth.get("bids")!=null&&depth.get("asks")!=null) {

            //Map降排序，将买单存入集合
            bids = depth.get("bids");

            for (String str : bids.keySet()) {
                List<String> buy = new ArrayList<>();
                buy.add(str);
                buy.add(bids.get(str));
                buyPrices.add(buy);
            }
            //map升序将卖单存入集合
            asks =depth.get("asks");
            for (String str : asks.keySet()) {
                List<String> sell = new ArrayList<>();
                sell.add(str);
                sell.add(asks.get(str));
                sellPrices.add(sell);
            }
            logger.info("买单：" + buyPrices);
            logger.info("卖单：" + sellPrices);


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

                submitOrder(1, finalSellPrice, finalSellAmount);
                //setTradeLog(id, "对标挂卖单amount:" + finalSellAmount + "  price:" + finalSellPrice, 0, "ff6224");

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
                    setTradeLog(id, "对标深度卖单撤单[" + tradeId + "]=>" + res, 0, "000000");
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
                sleep(500, Integer.parseInt(exchange.get("isMobileSwitch")));
                BigDecimal finalBuyPrice = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(0))), Integer.parseInt(precision.get("pricePrecision").toString()));

                BigDecimal finalBuyAmount = nN(new BigDecimal(String.valueOf(neadBuy.get(i).get(1))).multiply(new BigDecimal(exchange.get("depthRate"))), Integer.parseInt(precision.get("amountPrecision").toString()));

                submitOrder(0, finalBuyPrice, finalBuyAmount);
                //setTradeLog(id, "对标挂买单", 0, "05cbc8");
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
                    setTradeLog(id, "对标深度买单撤单[" + tradeId+ "]=>" + res, 0, "000000");
                }

            }
            neadBuy.clear();
            neadSell.clear();
            notNeadCancel.clear();
            notNeadCancelBuy.clear();



        }else {
            setTradeLog(id, "未切换交易对，请确认该对标平台存在该交易对" , 0, "000000");
        }
        buyPrices.clear();
        sellPrices.clear();
        depth.clear();
        bids.clear();
        asks.clear();

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
