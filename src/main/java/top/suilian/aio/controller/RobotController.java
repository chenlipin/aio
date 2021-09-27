/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.suilian.aio.service.aatradeRobitService.TradeRobotService;
import top.suilian.aio.vo.*;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * <B>Description:</B> 机器人操作接口 <br>
 * <B>Create on:</B> 2021/9/14 9:21 <br>
 *
 * @author dong.wan
 * @version 1.0
 */

@Controller
@RequestMapping("/robot")
public class RobotController {

    @Autowired
    TradeRobotService tradeRobotService;


    /**
     * 挂单接口
     *
     * @param tradeReq
     * @return
     */
    @RequestMapping(value = "/trade")
    @ResponseBody
    public ResponseEntity trade(@Valid @RequestBody TradeReq tradeReq) throws UnsupportedEncodingException {
        ResponseEntity responseEntity = tradeRobotService.trade(tradeReq);
        return ResponseEntity.success();

    }

    /**
     * 一键挂单
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/fastTrade")
    @ResponseBody
    public ResponseEntity fastTrade(@Valid @RequestBody FastTradeReq req) {
        ResponseEntity responseEntity = tradeRobotService.fastTrade(req);
        return ResponseEntity.success();

    }

    /**
     * 取消一键挂单
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/cancalfastTrade")
    @ResponseBody
    public ResponseEntity cancalfastTrade(@Valid @RequestBody CancalAllOrder req) {
         tradeRobotService.cancalfastTrade(req);
        return ResponseEntity.success();

    }

    /**
     * 一键挂单机器人状态
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/fastTradestatus")
    @ResponseBody
    public ResponseEntity fastTradestatus(@Valid @RequestBody  CancalAllOrder req) {
        String str=tradeRobotService.fastTradestatus(req);
        return  ResponseEntity.success(str);

    }


    /**
     * 根据订单号撤单
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/cancalOrder")
    @ResponseBody
    public ResponseEntity cancalOrder(@Valid @RequestBody CancalOrderReq req) {

        return ResponseEntity.success();

    }

    /**
     * 一键撤单
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/cancalAllOrder")
    @ResponseBody
    public ResponseEntity cancalAllOrder(@Valid @RequestBody CancalOrderReq req) {

        return ResponseEntity.success();

    }

    /**
     * 获取所有手动挂单 买卖单前10条
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/getAllOrder")
    @ResponseBody
    public ResponseEntity getAllOrder(@Valid @RequestBody CancalAllOrder req) {
        List<getAllOrderPonse> orders=tradeRobotService.getAllOrder(req);
        return ResponseEntity.success(orders);

    }




    @RequestMapping(value = "/test")
    @ResponseBody
    public ResponseEntity trade()   {
        return ResponseEntity.success(new FastTradeReq());

    }

}
