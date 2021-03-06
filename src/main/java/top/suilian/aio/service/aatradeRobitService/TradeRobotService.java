
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
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.bibox.BiboxParentService;
import top.suilian.aio.service.bision.BisionParentService;
import top.suilian.aio.service.bithumb.BithumbParentService;
import top.suilian.aio.service.bitmart.BitMartParentService;
import top.suilian.aio.service.bitrue.BitrueParentService;
import top.suilian.aio.service.bitterex.BitterexParentService;
import top.suilian.aio.service.bkex.coinnoe.BkexParentService;
import top.suilian.aio.service.citex.CitexParentService;
import top.suilian.aio.service.citex.kline.CitexKline;
import top.suilian.aio.service.coinnoe.CoinnoeParentService;
import top.suilian.aio.service.coinstore.CoinStoreParentService;
import top.suilian.aio.service.coinstore.CoinStoreService;
import top.suilian.aio.service.digifinex.DigifinexParentService;
import top.suilian.aio.service.hoo.HooParentService;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.iex.IexParentService;
import top.suilian.aio.service.kucoin.KucoinParentService;
import top.suilian.aio.service.lbank.LbankParentService;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.mxc.MxcParentService;
import top.suilian.aio.service.skiesex.SkiesexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.service.whitebit.WhitebitParentService;
import top.suilian.aio.service.whitebit.WhitebitService;
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
     * ????????????
     *
     * @param req
     */
    public ResponseEntity trade(TradeReq req) throws UnsupportedEncodingException {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("????????????????????????");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature??????");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        Map<String, String> stringStringMap = robotAction.submitOrderStr(req.getType(), new BigDecimal(req.getPrice()), new BigDecimal(req.getAmount()));
        if (!"true".equals(stringStringMap.get("res"))) {
            throw new RuntimeException("????????????_" + stringStringMap.get("orderId"));
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
     * ????????????
     *
     * @param req
     * @return
     */
    public ResponseEntity fastTrade(FastTradeReq req) {
        Member user = redisHelper.getUser(req.getToken());
//        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("????????????????????????");
//        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature??????");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        FastTradeM fastTradeM = new FastTradeM(req, robotAction);
        executor.execute(fastTradeM);
        return ResponseEntity.success();
    }

    /**
     * ???????????????????????????????????????
     *
     * @param robotId
     * @return
     */
    public RobotAction getRobotAction(Integer robotId) {
        Robot robot = robotMapper.selectByPrimaryKey(robotId);
        RobotAction robotAction = null;
        if (robot == null) {
            throw new RuntimeException("??????????????????");
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
                robotAction = new BitrueParentService();
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
            default:
                return null;
        }
        robotAction.setParam(robotId);
        return robotAction;
    }

    public void cancalfastTrade(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("????????????????????????");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature??????");
        }

        map.remove(req.getRobotId());
    }

    public String fastTradestatus(CancalAllOrder req) {
        if (map.get(req.getRobotId()) == null) {
            return "?????????";
        }
        return "?????????";
    }

    /**
     * ????????????
     *
     * @param req
     * @return
     */
    public List<getAllOrderPonse> getAllOrder(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("????????????????????????");
        }
        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature??????");
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
     * ????????????
     *
     * @param req
     */
    public ResponseEntity cancalAllOrder(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("????????????????????????");
        }

        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
        if (!checkSignature) {
//            throw new RuntimeException("Signature??????");
        }
        RobotAction robotAction = getRobotAction(req.getRobotId());
        FastCancalTradeM fastCancalTradeM = new FastCancalTradeM(robotAction, req);
        executor.execute(fastCancalTradeM);
        return ResponseEntity.success();

    }


    /**
     * ?????????????????????
     *
     * @param req
     * @return
     */
    public void cancalByOrderId(CancalOrderReq req) {
        Member user = redisHelper.getUser(req.getToken());
//        if (user == null || !user.getMemberId().equals(req.getUserId())) {
//            throw new RuntimeException("????????????????????????");
//        }
//        boolean checkSignature = checkSignature((JSONObject) JSONObject.toJSON(req), req.getSignature());
//        if (!checkSignature) {
////            throw new RuntimeException("Signature??????");
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
//            throw new RuntimeException("????????????????????????");
//        }
        String maxLeftQty = redisHelper.get("maxLeftQty_" + req.getRobotId()) == null ? "0" : redisHelper.get("maxLeftQty_" + req.getRobotId());
        String maxRightQty = redisHelper.get("maxRightQty_" + req.getRobotId()) == null ? "0" : redisHelper.get("maxRightQty_" + req.getRobotId());

        return maxLeftQty + "_" + maxRightQty;
    }

    public void clean(CancalAllOrder req) {
        Member user = redisHelper.getUser(req.getToken());
        if (user == null || !user.getMemberId().equals(req.getUserId())) {
            throw new RuntimeException("????????????????????????");
        }
        if (req.getType().equals("left")) {
            redisHelper.remove("maxLeftQty_" + req.getRobotId());
        } else {
            redisHelper.remove("maxRightQty_" + req.getRobotId());
        }
    }


    /**
     * ????????????????????????
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
            if ("?????????".equals(map.get(fastTradeReq.getRobotId()))) {
                throw new RuntimeException("????????????????????????????????????");
            }
            map.put(fastTradeReq.getRobotId(), "?????????");
            //?????????????????????
            Map<String, String> param = robotAction.getParam();
            //???????????????????????????
            int newBuyOrder = 0;
            //???????????????????????????
            int newSellOrder = 0;
            int timeChange = fastTradeReq.getMaxTime() - fastTradeReq.getMinTime();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            boolean first = true;
            while ((newBuyOrder + newSellOrder) < (fastTradeReq.getBuyOrdermun() + fastTradeReq.getSellOrdermun()) && "?????????".equals(map.get(fastTradeReq.getRobotId()))) {
                //?????????????????????null?????????????????????
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
                //??????????????????
                Double amountPrecision = RandomUtilsme.getRandom(fastTradeReq.getMaxAmount() - fastTradeReq.getMinAmount(), Integer.parseInt(param.get("amountPrecision")));
                BigDecimal amount = new BigDecimal(fastTradeReq.getMinAmount() + amountPrecision).setScale(Integer.parseInt(param.get("amountPrecision")), BigDecimal.ROUND_HALF_UP);

                /**
                 * ??????????????????
                 */
                double buyRange = fastTradeReq.getBuyorderRangePrice1() - fastTradeReq.getBuyorderRangePrice();
                /**
                 * ????????????????????????
                 */
                double buyOneRange = buyRange / fastTradeReq.getBuyOrdermun();
                /**
                 * ??????????????????
                 */
                double sellRange = fastTradeReq.getSellorderRangePrice1() - fastTradeReq.getSellorderRangePrice();
                /**
                 * ????????????????????????
                 */
                double sellOneRange = buyRange / fastTradeReq.getBuyOrdermun();

                //??????????????????????????????
                boolean type = true;
                boolean typeTrade = true;
                BigDecimal price = BigDecimal.ZERO;
                if (type && newBuyOrder < fastTradeReq.getBuyOrdermun()) {
                    //????????????
                    Double pricePrecision = RandomUtilsme.getRandom(buyOneRange, Integer.parseInt(param.get("pricePrecision")));
                    Double pricePrecision1 = (fastTradeReq.getBuyorderRangePrice() + pricePrecision + buyOneRange * newBuyOrder);
                    price = new BigDecimal(fastTradeReq.getBuyorderBasePrice() - pricePrecision1).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);
                    //?????????
                    newBuyOrder++;
                } else {
                    //????????????
                    Double pricePrecision = RandomUtilsme.getRandom(sellOneRange, Integer.parseInt(param.get("pricePrecision")));
                    Double pricePrecision1 = (fastTradeReq.getSellorderRangePrice() + pricePrecision + sellOneRange * newSellOrder);
                    price = new BigDecimal(fastTradeReq.getSellorderBasePrice() + pricePrecision1).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);
                    //?????????
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
                //??????????????????
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
     * ????????????????????????
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
            //????????????????????????????????????????????????
            List<ApitradeLog> apitradeLogs = apitradeLogMapper.selectByRobotIdNOTrade(cancalAllOrder.getRobotId());
            //????????????
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
     * MD5??????
     */
    public String getMD5String(String str) {
        try {
            // ????????????MD5??????????????????
            MessageDigest md = MessageDigest.getInstance("MD5");
            // ??????md5??????
            md.update(str.getBytes());
            // digest()??????????????????md5 hash??????????????????8?????????????????????md5 hash??????16??????hex?????????????????????8????????????
            // BigInteger????????????8????????????????????????16???hex??????????????????????????????????????????????????????hash???
            //??????byte??????????????????????????????2????????????????????????2???8????????????16???2?????????
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
