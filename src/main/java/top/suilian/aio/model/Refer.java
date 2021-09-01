package top.suilian.aio.model;

import java.io.Serializable;

public class Refer implements Serializable {

    String amount;   //数量
    String price;    //价格
    String id;       //唯一标识这个交易的参数。可id，可时间戳
    String isSell;   //买卖方向。买:buy  卖:sell

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsSell() {
        return isSell;
    }

    public void setIsSell(String isSell) {
        this.isSell = isSell;
    }

    @Override
    public String toString() {
        return "Refer{" +
                "amount='" + amount + '\'' +
                ", price='" + price + '\'' +
                ", id='" + id + '\'' +
                ", isSell='" + isSell + '\'' +
                '}';
    }
}
