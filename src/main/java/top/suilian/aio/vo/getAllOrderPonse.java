package top.suilian.aio.vo;

import lombok.Data;

import java.io.Serializable;



@Data
public class getAllOrderPonse implements Serializable {
    private Integer type;
    private String price;
    private String amount;
    private Integer status;
    private String  orderId;
    /**
     * 0 全局
     * 1:机器的单子
     */
    private Integer  myself;
    private String createdAt;
}
