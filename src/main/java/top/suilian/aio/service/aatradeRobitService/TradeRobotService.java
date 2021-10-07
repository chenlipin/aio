/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service.aatradeRobitService;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.RandomUtilsme;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.vo.FastTradeReq;
import top.suilian.aio.vo.ResponseEntity;
import top.suilian.aio.vo.TradeReq;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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


    /**
     * 挂单接口
     *
     * @param tradeReq
     */
    public ResponseEntity trade(TradeReq tradeReq) throws UnsupportedEncodingException {
        RobotAction robotAction = getRobotAction(tradeReq.getRobotId());
        robotAction.submitOrderStr(tradeReq.getType(), new BigDecimal("tradeReq.getPrice()"), new BigDecimal(tradeReq.getAmount()));
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
        Future<JSONObject> submit = executor.submit(fastTradeM);
        try {
            submit.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
            //BHEX
            case Constant.KEY_EXCHANGE_BHEX:
                robotAction = new WbfexParentService();
                break;
            case Constant.KEY_EXCHANGE_HOTCOIN:
                robotAction = new HotCoinParentService();
                break;
            default:
                return null;
        }
        robotAction.setParam(robotId);
        return robotAction;
    }

    /**
     * 一键挂单核心逻辑
     */
    class FastTradeM implements Callable<JSONObject> {
        FastTradeReq fastTradeReq;
        RobotAction robotAction;

        public FastTradeM(FastTradeReq fastTradeReq, RobotAction robotAction) {
            this.fastTradeReq = fastTradeReq;
            this.robotAction = robotAction;
        }

        @Override
        public JSONObject call() throws Exception {
            //获取机器人参数
            Map<String, String> param = robotAction.getParam();
            //当前已经挂买单个数
            int newBuyOrder = 0;
            //当前已经挂卖单个数
            int newSellOrder = 0;
            int timeChange = fastTradeReq.getMaxTime() - fastTradeReq.getMinTime();
            while ((newBuyOrder + newSellOrder) < (fastTradeReq.getBuyOrdermun() + fastTradeReq.getSellOrdermun())) {
                //计算挂单数量
                Double amountPrecision = RandomUtilsme.getRandom(fastTradeReq.getMaxAmount() - fastTradeReq.getMinAmount(), Integer.parseInt(param.get("amountPrecision")));
                BigDecimal amount = new BigDecimal(fastTradeReq.getMinAmount() + amountPrecision);

                //决定是挂买单还是卖单
                boolean type = RandomUtils.nextBoolean();
                BigDecimal price = BigDecimal.ZERO;
                if (type && newBuyOrder < fastTradeReq.getBuyOrdermun()) {
                    //挂买单
                    newBuyOrder++;
                    //当基础买价为null就去拿盘口价格
                    BigDecimal baseBuyPrice = fastTradeReq.getSellorderBasePrice() != null ? new BigDecimal(fastTradeReq.getBuyorderBasePrice()) : BigDecimal.ZERO;
                    //计算买价
                    Double pricePrecision = RandomUtilsme.getRandom(fastTradeReq.getBuyorderRangePrice(), Integer.parseInt(param.get("pricePrecision")));
                    price = new BigDecimal(fastTradeReq.getBuyorderBasePrice() - pricePrecision);


                } else {
                    //挂卖单
                    newSellOrder++;
                    //当基础卖价为null就去拿盘口价格
                    BigDecimal baseSellPrice = fastTradeReq.getSellorderBasePrice() != null ? new BigDecimal(fastTradeReq.getSellorderBasePrice()) : BigDecimal.ZERO;
                    //计算卖价
                    Double pricePrecision = RandomUtilsme.getRandom(fastTradeReq.getSellorderRangePrice(), Integer.parseInt(param.get("pricePrecision")));
                    price = new BigDecimal(fastTradeReq.getSellorderBasePrice() + pricePrecision);
                }
                String orderStr = robotAction.submitOrderStr(type ? 1 : 2, price, amount);
                //挂单间隔时间
                int randomTime = fastTradeReq.getMinTime() + RandomUtils.nextInt(timeChange);
                Thread.sleep(randomTime);
            }

            return null;
        }
    }


}
