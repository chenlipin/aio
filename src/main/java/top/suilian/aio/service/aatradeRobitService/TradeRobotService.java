/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service.aatradeRobitService;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.vo.FastTradeReq;
import top.suilian.aio.vo.ResponseEntity;
import top.suilian.aio.vo.TradeReq;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
     * @param req
     * @return
     */
    public ResponseEntity fastTrade(FastTradeReq req) {
        RobotAction robotAction = getRobotAction(req.getRobotId());
        FastTradeM fastTradeM = new FastTradeM(req, robotAction);
        Future<JSONObject> submit = executor.submit(fastTradeM);
        try {
            JSONObject jsonObject = submit.get();
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
            return null;
        }
    }


}
