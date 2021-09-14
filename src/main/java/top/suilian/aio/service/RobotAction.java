/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service;

import java.math.BigDecimal;

/**
 * @author wandong
 * @description:
 * @date 2021/9/14 15:11
 */
public interface RobotAction {
    /**
     * 挂单
     * @param type 类型
     * @param price 价格
     * @param amount 数量
     * @return 单号
     */
    String submitOrder(int type, BigDecimal price, BigDecimal amount);


    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    String selectOrder(String orderId);


    /**
     * 根据单号撤单
     * @param orderId
     * @return
     */
    String cancelTrade(String orderId);


    public void setParam(Integer id);
}
