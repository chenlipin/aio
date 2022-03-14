package top.suilian.aio.refer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class DeepVo implements Serializable {
    private Integer type;
    private BigDecimal price;
    private BigDecimal amount;
}
