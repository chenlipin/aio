package top.suilian.aio.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;



@Data
public class getAllOrderPonse implements Serializable {
    private Integer type;
    private String price;
    private String amount;
    private Integer status;
    private String  orderId;

    private String createdAt;
}
