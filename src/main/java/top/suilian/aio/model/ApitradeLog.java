package top.suilian.aio.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * apitrade_log
 *
 * @author
 */
@Data
public class ApitradeLog implements Serializable {
    private Integer id;

    /**
     * 机器人id
     */
    private Integer robotId;

    /**
     * 订单id
     */
    private String orderId;

    /**
     * 挂单人id
     */
    private Integer memberId;

    /**
     * 类型
     * 1：买单
     * 2：买单
     */
    private Integer type;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 数量
     */
    private BigDecimal amount;

    /**
     * 0:未成交
     * 1:半成交
     * 2:已成交
     * -1:已撤单
     */
    private Integer status;

    /**
     * 说明
     */
    private String memo;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;

    private static final long serialVersionUID = 1L;
}