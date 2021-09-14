/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.service.aatradeRobitService;

/**
 * @author wandong
 * @description:
 * @date 2021/9/14 10:03
 */
public interface TradeService {
    /**
     * 挂单
     * @param type 1买单 2卖单
     * @param amount  数量
     * @param price 价格
     * @return
     */
    public String trade(Integer type,String amount,String price);

    /**
     * 撤单
     * @param orderId  订单id
     */
    public void cancal(String orderId);


    /**
     * 撤所有定单
     */
    public void cancalAll();

    /**
     * 一键挂单
     */
    public void fastTrade();


}
