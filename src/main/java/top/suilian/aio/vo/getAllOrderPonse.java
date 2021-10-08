package top.suilian.aio.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

/**
 * <B>Description:</B>  <br>
 * <B>Create on:</B> 2021/9/27 10:59 <br>
 *
 * @author dong.wan
 * @version 1.0
 */

@Data
public class getAllOrderPonse implements Serializable {
    private Integer type;
    private String price;
    private String amount;
    private Integer status;
    private String  orderId;

    private String createdAt;
}
