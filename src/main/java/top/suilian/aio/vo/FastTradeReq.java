/*
 * Copyright (C) 1997-2021 康成投资（中国）有限公司
 *
 * http://www.rt-mart.com
 *
 * 版权归本公司所有，不得私自使用、拷贝、修改、删除，否则视为侵权
 */
package top.suilian.aio.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * <B>Description:</B> 一件挂单参数 <br>
 * <B>Create on:</B> 2021/9/14 10:14 <br>
 *
 * @author dong.wan
 * @version 1.0
 */
@Data
public class FastTradeReq {
    /** 买单挂单档位*/
    @NotNull(message = "buyOrdermun不能为空")
    private Integer buyOrdermun;

    /** 卖单挂单档位*/
    @NotNull(message = "sellOrdermun不能为空")
    private Integer sellOrdermun;

    /**时间范围最小值*/
    @NotNull(message = "minTime不能为空")
    private Integer minTime;

    /**时间范围最大值 */
    @NotNull(message = "maxTime不能为空")
    private Integer maxTime;

    /**数量范围最小值 */
    @NotNull(message = "minAmount不能为空")
    private Integer minAmount;

    /**数量范围最大值 */
    @NotNull(message = "maxAmount不能为空")
    private Integer maxAmount;

    /**买单基础价格  当使用最新成交价作为基准时候传-1 */
    @NotNull(message = "buyorderBasePrice不能为空")
    private String buyorderBasePrice;

    /**卖单基础价格  当使用最新成交价作为基准时候传-1 */
    @NotNull(message = "sellorderBasePrice不能为空")
    private String sellorderBasePrice;

    /**买单比基准价低的范围 */
    @NotNull(message = "buyorderRangePrice不能为空")
    private String buyorderRangePrice;

    /**卖单比基准价高的范围 */
    @NotNull(message = "sellorderRangePrice不能为空")
    private String sellorderRangePrice;

    /**机器人id*/
    @NotNull(message = "robotId不能为空")
    private Integer robotId;
    /**操作人id*/
    @NotNull(message = "userId不能为空")
    private Integer userId;
    /**签名*/
//    @NotNull(message = "signature不能为空")
    private String signature;
}
