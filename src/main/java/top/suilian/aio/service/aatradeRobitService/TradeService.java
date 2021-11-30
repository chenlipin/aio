
package top.suilian.aio.service.aatradeRobitService;


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
