/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service.aatradeRobitService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.service.RobotAction;
import top.suilian.aio.service.loex.LoexParentService;
import top.suilian.aio.service.wbfex.WbfexParentService;
import top.suilian.aio.vo.ResponseEntity;
import top.suilian.aio.vo.TradeReq;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

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


    /**
     * 挂单接口
     *
     * @param tradeReq
     */
    public ResponseEntity trade(TradeReq tradeReq) throws UnsupportedEncodingException {
        RobotAction robotAction = getRobotAction(tradeReq.getRobotId());
        robotAction.submitOrder(tradeReq.getType(),new BigDecimal("tradeReq.getPrice()"),new BigDecimal(tradeReq.getAmount()));
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


}
