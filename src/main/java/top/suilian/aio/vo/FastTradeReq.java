
package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;


@Data
public class FastTradeReq {
    /** 买单挂单档位*/

    private Integer buyOrdermun;

    /** 卖单挂单档位*/

    private Integer sellOrdermun;

    /**时间范围最小值*/
    @NotNull(message = "minTime不能为空")
    private Integer minTime;

    /**时间范围最大值 */
    @NotNull(message = "maxTime不能为空")
    private Integer maxTime;

    /**数量范围最小值 */
    @NotNull(message = "minAmount不能为空")
    private Double minAmount;

    /**数量范围最大值 */
    @NotNull(message = "maxAmount不能为空")
    private Double maxAmount;

    /**买单基础价格  当使用最新成交价作为基准时候传null */

    private Double buyorderBasePrice;

    /**卖单基础价格  当使用最新成交价作为基准时候传null */

    private Double sellorderBasePrice;

    /**买单比基准价低的范围 */

    private Double buyorderRangePrice;

    /**买单比基准价低的范围 */

    private Double buyorderRangePrice1;

    /**卖单比基准价高的范围 */
    private Double sellorderRangePrice;

    /**卖单比基准价高的范围 */
    private Double sellorderRangePrice1;

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
