package top.suilian.aio.redis;

import java.io.Serializable;


/**
 * Redis传输对象基类所有要进行缓存的类必须继承它
 *
 * @author KD
 */
public class RedisObject implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 最近活动时间
     */
    private long lastActiveDateTime = System.currentTimeMillis();

    /**
     * 扩展对象
     */
    private Object extObject;

    public long getLastActiveDateTime() {
        return lastActiveDateTime;
    }

    public void setLastActiveDateTime(long lastActiveDateTime) {
        this.lastActiveDateTime = lastActiveDateTime;
    }

    public Object getExtObject() {
        return extObject;
    }

    public void setExtObject(Object extObject) {
        this.extObject = extObject;
    }
}
