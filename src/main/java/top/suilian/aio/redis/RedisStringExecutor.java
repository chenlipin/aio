package top.suilian.aio.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("redisStringExecutor")
public class RedisStringExecutor {
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    public RedisStringExecutor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value) {
        try {
            this.redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
    }

    public void set(String key, String value, Long timeout) {
        try {
            this.redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
    }

    public Long increment(String key, Long incrementCount) {
        return this.redisTemplate.opsForValue().increment(key, incrementCount);
    }

    public boolean setIfAbsent(String key, String value) {
        return this.redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public String get(String key) {
        try {
            return key != null && key.length() != 0 ? (String) this.redisTemplate.opsForValue().get(key) : null;
        } catch (Exception e) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
        return null;
    }

    public String getAndSet(String key, String value) {
        return key != null && key.length() != 0 ? (String) this.redisTemplate.opsForValue().getAndSet(key, value) : null;
    }

    public void setExpireTime(String key, TimeUnit timeUnit, int expireTime) {
        if (timeUnit != null && expireTime > 0) {
            this.redisTemplate.expire(key, (long) expireTime, timeUnit);
        }
    }

    public void remove(String... keys) {
        try {
            if (keys != null && keys.length != 0) {
                String[] var2 = keys;
                int var3 = keys.length;

                for (int var4 = 0; var4 < var3; ++var4) {
                    String key = var2[var4];
                    if (key != null) {
                        this.redisTemplate.delete(key);
                    }
                }
            }
        } catch (Exception e) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
    }

    public void removeParent(String keys) {
        try {
            redisTemplate.delete(redisTemplate.keys(keys + "*"));
        } catch (Exception e) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
    }


    public Long incrementAndTime(String key, long time, TimeUnit timeUnit) {
        Long incr = this.redisTemplate.opsForValue().increment(key, 1L);
        this.redisTemplate.expire(key, time, timeUnit);
        return incr;
    }
}
