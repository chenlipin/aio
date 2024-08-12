package top.suilian.aio.service.hotcoin;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Vedit2 implements Serializable {
    private  String apikey;

    private String symbol;
    private double total;//可用

   private String validCode;
}
