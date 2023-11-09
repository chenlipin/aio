
package top.suilian.aio.service;

import top.suilian.aio.vo.getAllOrderPonse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface RobotAction {
    /**
     * 挂单
     * @param type 类型
     * @param price 价格
     * @param amount 数量
     * @return 单号
     */
    Map<String,String>  submitOrderStr(int type, BigDecimal price, BigDecimal amount)  ;


    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    Map<String,Integer> selectOrderStr(String orderId);

    /**
     * 查询所有订单详情
     * @param
     * @return
     */
    List<getAllOrderPonse> selectOrder();


    /**
     * 撤销所有订单  0 账户下所有挂单,1机器人挂单
     * tradeType:1 buy :2:sell
     * @param type
     * @return
     */
    List<String> cancelAllOrder(Integer type,Integer tradeType);

    /**
     * 根据单号撤单
     * @param orderId
     * @return
     */
    String cancelTradeStr(String orderId);


    public void setParam(Integer id);



    public Map<String, String> getParam();
}
