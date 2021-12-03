
package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


@Data
public class TradeReq implements Serializable {
    /**1:买单 2：卖单*/
    @NotNull(message = "type不能为空")
    private Integer type;
    /**数量*/
    @NotNull(message = "amount不能为空")
    private String amount;
    /**价格*/
    @NotNull(message = "price不能为空")
    private String price;
    /**机器人id*/
    @NotNull(message = "robotId不能为空")
    private Integer robotId;
    /**操作人id*/
    @NotNull(message = "userId不能为空")
    private Integer userId;
    /**签名*/
    @NotNull(message = "signature不能为空")
    private String signature;

    @NotNull(message = "token不能为空")
    private String token;
}
