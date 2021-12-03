
package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;



@Data
public class CancalOrderReq {

    /**orderIdid*/
    @NotNull(message = "orderId不能为空")
    private String orderId;

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
