package top.suilian.aio.model.request;

/**
 * 机器人启动停止实体
 */
public class OperationRequest extends BaseRequest {
    /**
     * 机器人ID
     */
    private int id;

    /**
     * 机器人编号
     * KEY_EXCHANGE_FCHAIN
     */
    private int coin;

    /**
     * 策略类型
     */
    private int type;

    /**
     * 操作类型
     * 0:后台
     * 1:前台
     *
     * @return
     */

    private int category;

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "OperationRequest{" +
                "id=" + id +
                ", coin=" + coin +
                ", type=" + type +
                ", category=" + category +
                '}';
    }
}
