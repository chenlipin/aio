package top.suilian.aio.service.hotcoin;

import lombok.Data;

import java.io.Serializable;

@Data
public class BananceVo implements Serializable {
    private String symbol;
    private double total;//可用
    private double frozen;

}
