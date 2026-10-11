package top.suilian.aio.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.suilian.aio.redis.RedisHelper;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证offset持久化、已处理消息去重和失去轮询锁后的行为。 */
public class TelegramPollingServiceTest {
    private TelegramPollingService service;
    private RedisHelper redis;
    private TelegramApiClient api;
    private TelegramCommandService commands;
    private TelegramGroupDiscoveryService discovery;

    /** 使用模拟客户端和Redis，不启动后台轮询线程。 */
    @Before
    public void setUp() {
        service = new TelegramPollingService();
        redis = mock(RedisHelper.class);
        api = mock(TelegramApiClient.class);
        commands = mock(TelegramCommandService.class);
        discovery = mock(TelegramGroupDiscoveryService.class);
        ReflectionTestUtils.setField(service, "redisHelper", redis);
        ReflectionTestUtils.setField(service, "apiClient", api);
        ReflectionTestUtils.setField(service, "commandService", commands);
        ReflectionTestUtils.setField(service, "groupDiscovery", discovery);
        ReflectionTestUtils.setField(service, "configuredInstanceId", "test-instance");
        when(redis.setIfAbsent(anyString(), anyString(), eq(300L))).thenReturn(true);
        when(redis.renewOwnedLock(anyString(), anyString(), eq(300L))).thenReturn(true);
        when(api.getUsername("123:test")).thenReturn("test_bot");
        when(redis.get("TELEGRAM_UPDATE_OFFSET:123")).thenReturn("100");
    }

    /** 关闭测试服务，避免留下线程资源。 */
    @After
    public void tearDown() {
        service.stop();
    }

    /** 已处理的消息不再执行，新的消息处理后推进offset。 */
    @Test
    public void persistedOffsetAndDoneMarkersPreventDuplicates() {
        JSONObject done = update(100);
        JSONObject fresh = update(101);
        JSONArray batch = new JSONArray();
        batch.add(done);
        batch.add(fresh);
        when(api.getUpdates("123:test", 100L)).thenReturn(batch);
        when(redis.get("TELEGRAM_UPDATE_DONE:123:100")).thenReturn("done");
        service.pollOnce("123:test");
        verify(commands, never()).handle("123:test", "test_bot", done);
        verify(commands).handle("123:test", "test_bot", fresh);
        verify(discovery).observe(fresh);
        verify(discovery, never()).observe(done);
        verify(redis).setSt("TELEGRAM_UPDATE_DONE:123:101", "done", 86400L);
        verify(redis).setSt("TELEGRAM_UPDATE_OFFSET:123", "102");
        verify(redis).releaseOwnedLock(eq("TELEGRAM_POLL_LOCK:123"), anyString());
    }

    /** 回复处理失败不能确认该消息，下一轮仍然可以重试。 */
    @Test(expected = IllegalStateException.class)
    public void failedCommandDoesNotAdvanceOffset() {
        JSONObject fresh = update(100);
        JSONArray batch = new JSONArray();
        batch.add(fresh);
        when(api.getUpdates("123:test", 100L)).thenReturn(batch);
        doThrow(new IllegalStateException("test failure")).when(commands).handle("123:test", "test_bot", fresh);
        try {
            service.pollOnce("123:test");
        } finally {
            verify(redis, never()).setSt(eq("TELEGRAM_UPDATE_OFFSET:123"), anyString());
            verify(redis).releaseOwnedLock(eq("TELEGRAM_POLL_LOCK:123"), anyString());
        }
    }

    /** 发现群失败不确认消息、不执行查询，留给下次接收重试。 */
    @Test(expected = IllegalStateException.class)
    public void failedDiscoveryDoesNotAdvanceOffset() {
        JSONObject fresh = update(100);
        JSONArray batch = new JSONArray();
        batch.add(fresh);
        when(api.getUpdates("123:test", 100L)).thenReturn(batch);
        doThrow(new IllegalStateException("test failure")).when(discovery).observe(fresh);
        try { service.pollOnce("123:test"); }
        finally {
            verify(commands, never()).handle(anyString(), anyString(), any(JSONObject.class));
            verify(redis, never()).setSt(eq("TELEGRAM_UPDATE_OFFSET:123"), anyString());
        }
    }

    /** 失去锁持有权后不能继续处理消息。 */
    @Test
    public void lostLeaseStopsProcessing() {
        JSONArray batch = new JSONArray();
        batch.add(update(100));
        when(api.getUpdates("123:test", 100L)).thenReturn(batch);
        when(redis.renewOwnedLock(anyString(), anyString(), eq(300L))).thenReturn(false);
        service.pollOnce("123:test");
        verify(commands, never()).handle(anyString(), anyString(), any(JSONObject.class));
        verify(redis, never()).setSt(eq("TELEGRAM_UPDATE_OFFSET:123"), anyString());
    }

    /** 存在Webhook时拒绝启动轮询，避免两种接收方式冲突。 */
    @Test(expected = IllegalStateException.class)
    public void existingWebhookPreventsPolling() {
        when(api.hasWebhook("123:test")).thenReturn(true);
        try {
            service.pollOnce("123:test");
        } finally {
            verify(api, never()).getUpdates(anyString(), anyLong());
            verify(redis).releaseOwnedLock(eq("TELEGRAM_POLL_LOCK:123"), anyString());
        }
    }

    /** 超过5分钟的旧指令只推进offset，不执行查询。 */
    @Test
    public void oldCommandIsNotExecutedOnStartup() {
        JSONObject old = update(100);
        old.getJSONObject("message").put("date", System.currentTimeMillis() / 1000 - 600);
        JSONArray batch = new JSONArray();
        batch.add(old);
        when(api.getUpdates("123:test", 100L)).thenReturn(batch);
        service.pollOnce("123:test");
        verify(commands, never()).handle(anyString(), anyString(), any(JSONObject.class));
        verify(redis).setSt("TELEGRAM_UPDATE_OFFSET:123", "101");
    }

    /** 同一实例重启时先比较删除旧owner的锁，且本次启动只清理一次。 */
    @Test
    public void restartClearsPreviousOwnerOnceBeforeAcquiringLock() {
        String registry = "TELEGRAM_POLL_OWNER:123:"
                + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        "test-instance".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(redis.get(registry)).thenReturn("previous-owner");
        when(api.getUpdates("123:test", 100L)).thenReturn(new JSONArray());
        service.pollOnce("123:test");
        service.pollOnce("123:test");
        org.mockito.InOrder order = inOrder(redis);
        order.verify(redis).releaseOwnedLock("TELEGRAM_POLL_LOCK:123", "previous-owner");
        order.verify(redis).setRequired(eq(registry), anyString());
        order.verify(redis).setIfAbsent(eq("TELEGRAM_POLL_LOCK:123"), anyString(), eq(300L));
        verify(redis, times(1)).releaseOwnedLock("TELEGRAM_POLL_LOCK:123", "previous-owner");
        verify(redis, times(1)).setRequired(eq(registry), anyString());
        verify(redis, never()).remove("TELEGRAM_UPDATE_OFFSET:123");
    }

    /** 不认识的其他实例锁不能在启动时无条件删除。 */
    @Test
    public void startupDoesNotDeleteUnknownOtherOwner() {
        when(redis.get("TELEGRAM_POLL_LOCK:123")).thenReturn("other-instance-owner");
        when(api.getUpdates("123:test", 100L)).thenReturn(new JSONArray());
        service.pollOnce("123:test");
        verify(redis, never()).releaseOwnedLock("TELEGRAM_POLL_LOCK:123", "other-instance-owner");
        verify(redis, never()).remove("TELEGRAM_POLL_LOCK:123");
    }

    /** 关闭期间无需等待HTTP长轮询返回，就立即比较释放本实例的锁。 */
    @Test
    public void shutdownImmediatelyReleasesActiveLock() {
        when(api.getUpdates("123:test", 100L)).thenAnswer(invocation -> {
            service.stop();
            verify(redis).releaseOwnedLock(eq("TELEGRAM_POLL_LOCK:123"), anyString());
            return new JSONArray();
        });
        service.pollOnce("123:test");
        // 关闭立即释放，finally再次比较删除是幂等操作，不会误删新实例的锁。
        verify(redis, times(2)).releaseOwnedLock(eq("TELEGRAM_POLL_LOCK:123"), anyString());
    }

    /** 关闭后的服务不能再获取接收锁或发起长轮询。 */
    @Test
    public void stoppedServiceCannotAcquireAnotherLock() {
        service.stop();
        service.pollOnce("123:test");
        verify(redis, never()).setIfAbsent(eq("TELEGRAM_POLL_LOCK:123"), anyString(), anyLong());
        verify(api, never()).getUpdates(anyString(), anyLong());
    }

    /** 创建时间有效的模拟群更新。 */
    private JSONObject update(long id) {
        JSONObject message = new JSONObject();
        message.put("date", System.currentTimeMillis() / 1000);
        JSONObject update = new JSONObject();
        update.put("update_id", id);
        update.put("message", message);
        return update;
    }
}
