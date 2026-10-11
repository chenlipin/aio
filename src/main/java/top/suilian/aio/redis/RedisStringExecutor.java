package top.suilian.aio.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.Collections;

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

    /** 必须成功写入的实例登记信息，Redis异常向上抛出，避免误认为登记成功。 */
    public void setRequired(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
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

    /** 在同一条Redis命令中设置NX和过期时间，避免并发重复通知或留下永久冷却Key。 */
    public boolean setIfAbsent(String key, String value, long timeout) {
        return Boolean.TRUE.equals(this.redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, TimeUnit.SECONDS));
    }

    /** 仅允许锁的持有者续期，避免过期后的旧实例操作新实例的锁。 */
    public boolean renewOwnedLock(String key, String owner, long timeout) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end", Long.class);
        return Long.valueOf(1).equals(redisTemplate.execute(script, Collections.singletonList(key), owner, String.valueOf(timeout)));
    }

    /** 原子比较持有者后删除锁，避免误删其他实例正在使用的锁。 */
    public void releaseOwnedLock(String key, String owner) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
        redisTemplate.execute(script, Collections.singletonList(key), owner);
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
