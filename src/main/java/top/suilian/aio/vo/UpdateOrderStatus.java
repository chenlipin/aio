package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;


@Data
public class UpdateOrderStatus {

    /**机器人id*/
    @NotNull(message = "robotId不能为空")
    private String robotId;
    /**操作人id*/
    @NotNull(message = "userId不能为空")
    private Integer userId;
}
