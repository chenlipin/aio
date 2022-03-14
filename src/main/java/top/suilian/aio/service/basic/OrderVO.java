package top.suilian.aio.service.basic;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderVO implements Serializable {
    private String type;
    private String number;
    private String pair;
    private String price;
}
