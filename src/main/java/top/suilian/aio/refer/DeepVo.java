package top.suilian.aio.refer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class DeepVo implements Serializable {
    private Integer type;
    private BigDecimal price;
    private BigDecimal amount;

    private String priceStr;
    private String amountStr;

    public String getPriceStr() {
        return price.stripTrailingZeros().toPlainString();
    }

    public String getAmountStr() {
        return amount.stripTrailingZeros().toPlainString();
    }
}
