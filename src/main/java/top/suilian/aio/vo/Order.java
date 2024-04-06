package top.suilian.aio.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Order
{
    private Integer type;
    private BigDecimal price;
    private BigDecimal amount;
    private Boolean firstCancle = false;
    private String orderId;
}
