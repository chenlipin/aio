
package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


@Data
public class CancalAllOrder implements Serializable {
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

    private String type;

    private String time;



}
