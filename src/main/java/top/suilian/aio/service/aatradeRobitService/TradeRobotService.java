/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service.aatradeRobitService;

import com.alibaba.fastjson.JSON;
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
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.TradeEnum;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.vo.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <B>Description:</B>  <br>
 * <B>Create on:</B> 2021/9/14 9:59 <br>
 *
 * @author dong.wan
 * @version 1.0
 */
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

    Map<Integer, String> map = new ConcurrentHashMap<>();


    /**
     * 挂单接口
     *
     * @param tradeReq
     */
    public ResponseEntity trade(TradeReq tradeReq) throws UnsupportedEncodingException {
        RobotAction robotAction = getRobotAction(tradeReq.getRobotId());
        String orderId = robotAction.submitOrderStr(tradeReq.getType(), new BigDecimal(tradeReq.getPrice()), new BigDecimal(tradeReq.getAmount()));
        ApitradeLog apitradeLog = new ApitradeLog();
        apitradeLog.setAmount(new BigDecimal(tradeReq.getAmount()));
        apitradeLog.setPrice(new BigDecimal(tradeReq.getPrice()));
        apitradeLog.setRobotId(tradeReq.getRobotId());
        apitradeLog.setMemberId(tradeReq.getUserId());
        apitradeLog.setType(tradeReq.getType());
        apitradeLog.setTradeType(2);
        apitradeLog.setStatus(0);
        apitradeLog.setOrderId(orderId);
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
            case Constant.KEY_EXCHANGE_BHEX:
                robotAction = new WbfexParentService();
                break;
            default:
                return null;
        }
        robotAction.setParam(robotId);
        return robotAction;
    }

    public void cancalfastTrade(CancalAllOrder req) {
        if (!"运行中".equals(map.get(req.getRobotId()))) {
            throw new RuntimeException("有一个任务正在运行中");
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
        RobotAction robotAction = getRobotAction(req.getRobotId());
        ApitradeLog apitradeLog = apitradeLogMapper.selectByRobotIdAndOrderId(req.getRobotId(), req.getOrderId());
        if (!apitradeLog.getStatus().equals(TradeEnum.CANCEL.getStatus())) {
            String str = robotAction.cancelTradeStr(req.getOrderId());
            if (!"true".equals(str)) {
                throw new RuntimeException(ResultCode.ERROR.getMessage());
            }
            apitradeLog.setStatus(-1);
            apitradeLog.setUpdatedAt(new Date());
            apitradeLogMapper.updateByPrimaryKey(apitradeLog);
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
                //计算挂单数量
                Double amountPrecision = RandomUtilsme.getRandom(fastTradeReq.getMaxAmount() - fastTradeReq.getMinAmount(), Integer.parseInt(param.get("amountPrecision")));
                BigDecimal amount = new BigDecimal(fastTradeReq.getMinAmount() + amountPrecision).setScale(Integer.parseInt(param.get("amountPrecision")), BigDecimal.ROUND_HALF_UP);

                //决定是挂买单还是卖单
                boolean type = RandomUtils.nextBoolean();
                BigDecimal price = BigDecimal.ZERO;
                if (type && newBuyOrder < fastTradeReq.getBuyOrdermun()) {
                    //挂买单
                    newBuyOrder++;
                    //当基础买价为null就去拿盘口价格
                    BigDecimal baseBuyPrice = fastTradeReq.getSellorderBasePrice() != null ? BigDecimal.valueOf(fastTradeReq.getBuyorderBasePrice()) : BigDecimal.ZERO;
                    //计算买价
                    Double pricePrecision = RandomUtilsme.getRandom(fastTradeReq.getBuyorderRangePrice(), Integer.parseInt(param.get("pricePrecision")));
                    price = new BigDecimal(fastTradeReq.getBuyorderBasePrice() - pricePrecision).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);


                } else {
                    //挂卖单
                    newSellOrder++;
                    //当基础卖价为null就去拿盘口价格
                    BigDecimal baseSellPrice = fastTradeReq.getSellorderBasePrice() != null ? BigDecimal.valueOf(fastTradeReq.getSellorderBasePrice()) : BigDecimal.ZERO;
                    //计算卖价
                    Double pricePrecision = RandomUtilsme.getRandom(fastTradeReq.getSellorderRangePrice(), Integer.parseInt(param.get("pricePrecision")));
                    price = new BigDecimal(fastTradeReq.getSellorderBasePrice() + pricePrecision).setScale(Integer.parseInt(param.get("pricePrecision")), BigDecimal.ROUND_HALF_UP);
                }
                String orderStr = robotAction.submitOrderStr(type ? 1 : 2, price, amount);
                System.out.println("换单··数量：" + amount + "**价格：" + price + "**方向：" + (type ? "买" : "卖"));
                ApitradeLog apitradeLog = new ApitradeLog();
                apitradeLog.setAmount(amount);
                apitradeLog.setPrice(price);
                apitradeLog.setRobotId(fastTradeReq.getRobotId());
                apitradeLog.setMemberId(fastTradeReq.getUserId());
                apitradeLog.setType(type ? 1 : 2);
                apitradeLog.setTradeType(1);
                apitradeLog.setStatus(0);
                apitradeLog.setMemo(uuid);
                apitradeLog.setOrderId(orderStr);
                apitradeLog.setCreatedAt(new Date());
                if (first) {
                    apitradeLog.setMemo(uuid + "_" + JSON.toJSONString(fastTradeReq));
                    first = false;
                }
                apitradeLogMapper.insert(apitradeLog);
                //挂单间隔时间
                int randomTime = fastTradeReq.getMinTime() + RandomUtils.nextInt(timeChange);
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


}
