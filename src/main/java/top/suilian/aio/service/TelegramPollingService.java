package top.suilian.aio.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.SysConfigMapper;
import top.suilian.aio.model.SysConfig;
import top.suilian.aio.redis.RedisHelper;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Telegram长轮询接收器；Redis协调多实例，持久保存offset，避免重启重复执行。 */
@Service
public class TelegramPollingService {
    /** 一条余额指令最多查询5个账户，锁租期覆盖有限HTTP超时和回复时间。 */
    private static final long LEASE_SECONDS = 300;
    private static final Logger logger = Logger.getLogger(TelegramPollingService.class);
    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private TelegramApiClient apiClient;
    @Autowired
    private TelegramCommandService commandService;
    /** 收录未绑定的候选群，查询权限仍由指令服务独立校验。 */
    @Autowired
    private TelegramGroupDiscoveryService groupDiscovery;
    /** 使用独立线程，长轮询不会占用交易执行线程池。 */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "telegram-command-polling");
        thread.setDaemon(true);
        return thread;
    });
    /** 当前实例的锁持有者标识 */
    private final String owner = UUID.randomUUID().toString();
    /** 可选稳定实例名称；多台服务器应配置不同值，同一服务重启保持不变。 */
    @Value("${telegram.command.instance-id:}")
    private String configuredInstanceId;
    /** 未指定实例名称时，使用主机名和服务端口区分本地及服务器实例。 */
    @Value("${server.port:8080}")
    private String serverPort = "8080";
    /** 每次启动每个Bot仅清理一次上次遗留锁，运行中不反复抢占锁。 */
    private final Set<String> initializedBots = new HashSet<>();
    /** 正在持有的接收锁，关闭时立即按owner比较释放。 */
    private volatile String activeLockKey;
    private volatile boolean stopped;
    private boolean started;
    private String cachedToken;
    private String username;
    /** 仅在接收状态改变时打印，避免每10秒重复输出相同待机原因。 */
    private String lastState;
    /** 正常轮询每分钟打印一次心跳，便于确认线程仍在接收消息。 */
    private long lastHeartbeat;

    /** 应用准备完成后启动；未开启telegram.command.enabled时不访问Telegram。 */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!started) {
            started = true;
            logger.info("Telegram指令接收线程启动，等待检查数据库开关和Bot配置");
            worker.submit(this::run);
        }
    }

    /** 动态读取数据库开关和Token；错误时退避，不在短时间内持续重试。 */
    private void run() {
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            try {
                String enabled = config("telegram.command.enabled");
                String token = config("telegram.bot.token");
                if (!("1".equals(enabled) || "true".equalsIgnoreCase(enabled))) {
                    reportState("disabled", "Telegram指令接收未开启：请将sys_config中的telegram.command.enabled设为1并启用该配置项");
                    pause();
                    continue;
                }
                if (StringUtils.isBlank(token)) {
                    reportState("missing-token", "Telegram Bot Token未配置：请检查telegram.bot.token的值及enabled状态");
                    pause();
                    continue;
                }
                pollOnce(token);
            } catch (Exception e) {
                lastState = "error";
                // 不输出Token或原始HTTP异常，避免密钥出现在启动日志。
                logger.warn("Telegram指令接收失败，请检查Token、Webhook、Redis及网络配置（"
                        + e.getClass().getSimpleName() + "）");
                pause();
            }
        }
    }

    /** 单轮接收；处理消息前续期确认锁归属，只有当前持有者可以推进offset。 */
    void pollOnce(String token) {
        if (!token.matches("[0-9]+:[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Bot Token格式错误");
        }
        String botId = token.substring(0, token.indexOf(':'));
        String lockKey = "TELEGRAM_POLL_LOCK:" + botId;
        clearPreviousInstanceLock(botId, lockKey);
        boolean acquired;
        // 与stop协调，防止关闭释放之后工作线程又取得新锁。
        synchronized (this) {
            if (stopped) {
                return;
            }
            acquired = redisHelper.setIfAbsent(lockKey, owner, LEASE_SECONDS);
            if (acquired) {
                activeLockKey = lockKey;
            }
        }
        if (!acquired) {
            reportState("waiting-lock", "Telegram轮询锁被其他实例占用，本实例等待接收，botId=" + botId);
            pause();
            return;
        }
        try {
            if (stopped) {
                return;
            }
            if (!token.equals(cachedToken)) {
                if (apiClient.hasWebhook(token)) {
                    logger.warn("Telegram已配置Webhook，长轮询暂停；请先调用deleteWebhook再接收群指令。");
                    throw new IllegalStateException("请先移除Webhook再启用长轮询");
                }
                username = apiClient.getUsername(token);
                cachedToken = token;
                logger.info("Telegram Bot连接成功，botId=" + botId + ", username=" + username);
            }
            reportState("polling", "Telegram长轮询已开始，botId=" + botId);
            String offsetKey = "TELEGRAM_UPDATE_OFFSET:" + botId;
            String savedOffset = redisHelper.get(offsetKey);
            long offset = StringUtils.isBlank(savedOffset) ? 0L : Long.parseLong(savedOffset);
            // 心跳不打印Token、消息内容或账户密钥。
            if (System.currentTimeMillis() - lastHeartbeat >= 60000) {
                lastHeartbeat = System.currentTimeMillis();
                logger.info("Telegram正在接收群指令，botId=" + botId + ", offset=" + offset);
            }
            JSONArray updates = apiClient.getUpdates(token, offset);
            if (updates == null) {
                throw new IllegalStateException("Telegram更新响应缺少result");
            }
            if (!updates.isEmpty()) {
                logger.info("Telegram收到更新，botId=" + botId + ", count=" + updates.size());
            }
            for (int i = 0; i < updates.size(); i++) {
                if (stopped || !redisHelper.renewOwnedLock(lockKey, owner, LEASE_SECONDS)) {
                    return;
                }
                JSONObject update = updates.getJSONObject(i);
                long updateId = update.getLongValue("update_id");
                if (updateId < offset) {
                    continue;
                }
                String doneKey = "TELEGRAM_UPDATE_DONE:" + botId + ":" + updateId;
                if (!"done".equals(redisHelper.get(doneKey))) {
                    // 先发现群，再处理指令；未绑定群也能在后台配置页面被搜索到。
                    groupDiscovery.observe(update);
                    // 历史群指令超过5分钟不再执行，避免启动后集中回复旧消息。
                    JSONObject message = update.getJSONObject("message");
                    if (message == null || System.currentTimeMillis() / 1000 - message.getLongValue("date") <= 300) {
                        commandService.handle(token, username, update);
                    } else {
                        logger.info("Telegram跳过超过5分钟的旧消息，updateId=" + updateId);
                    }
                    // 失去锁归属时不确认消息，由新的接收实例继续处理。
                    if (!redisHelper.renewOwnedLock(lockKey, owner, LEASE_SECONDS)) {
                        return;
                    }
                    redisHelper.setSt(doneKey, "done", 86400L);
                }
                offset = updateId + 1;
                redisHelper.setSt(offsetKey, String.valueOf(offset));
            }
        } finally {
            try {
                redisHelper.releaseOwnedLock(lockKey, owner);
            } finally {
                activeLockKey = null;
            }
        }
    }

    /** 启动时只删除登记属于本实例上次启动的owner，保留其他实例的接收锁和offset。 */
    private void clearPreviousInstanceLock(String botId, String lockKey) {
        if (initializedBots.contains(botId)) {
            return;
        }
        String instanceId = configuredInstanceId;
        if (StringUtils.isBlank(instanceId)) {
            String host = System.getenv("COMPUTERNAME");
            if (StringUtils.isBlank(host)) {
                host = System.getenv("HOSTNAME");
            }
            if (StringUtils.isBlank(host)) {
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                    throw new IllegalStateException("请配置telegram.command.instance-id以识别重启实例");
                }
            }
            instanceId = host + ":" + serverPort;
        }
        String encodedId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(instanceId.trim().getBytes(StandardCharsets.UTF_8));
        String registryKey = "TELEGRAM_POLL_OWNER:" + botId + ":" + encodedId;
        String previousOwner = redisHelper.get(registryKey);
        if (StringUtils.isNotBlank(previousOwner) && !owner.equals(previousOwner)) {
            redisHelper.releaseOwnedLock(lockKey, previousOwner);
            logger.info("Telegram重启清理本实例遗留轮询锁，botId=" + botId + ", instanceId=" + instanceId);
        }
        // 登记当前启动owner供下次重启使用；其他实例持锁时比较删除不会生效。
        redisHelper.setRequired(registryKey, owner);
        initializedBots.add(botId);
    }

    /** 从现有系统配置表读取启用项，不需要增加数据库表。 */
    private String config(String key) {
        SysConfig value = configMapper.selectByConfigKey(key);
        return value == null || value.getConfigValue() == null ? null : value.getConfigValue().trim();
    }

    /** 记录状态变化，不重复打印相同的开关、Token或锁等待提示。 */
    private void reportState(String state, String message) {
        if (!state.equals(lastState)) {
            lastState = state;
            logger.info(message);
        }
    }

    /** 失败或待机时等待10秒，关闭应用时可以立即中断等待。 */
    private void pause() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 应用关闭时停止轮询线程；正在进行的HTTP请求受有限超时约束。 */
    @PreDestroy
    public synchronized void stop() {
        stopped = true;
        logger.info("Telegram指令接收线程停止");
        worker.shutdownNow();
        if (activeLockKey != null) {
            try {
                redisHelper.releaseOwnedLock(activeLockKey, owner);
                logger.info("Telegram关闭服务，已释放本实例轮询锁");
            } catch (Exception e) {
                logger.warn("Telegram关闭时释放轮询锁失败，将等待锁自动过期");
            }
        }
    }
}
