
package top.suilian.aio.service;

import java.math.BigDecimal;
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
     * 根据单号撤单
     * @param orderId
     * @return
     */
    String cancelTradeStr(String orderId);


    public void setParam(Integer id);



    public Map<String, String> getParam();
}
