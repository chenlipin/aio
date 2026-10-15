package top.suilian.aio.service;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.dao.SysConfigMapper;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.dao.TelegramChannelMapper;
import top.suilian.aio.dao.TelegramSendLogMapper;
import top.suilian.aio.model.SysConfig;
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.TelegramChannel;
import top.suilian.aio.model.TelegramSendLog;
import top.suilian.aio.redis.RedisHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram通知服务，负责读取Bot配置、匹配接收目标、发送消息并记录发送日志。
 */
@Service
public class TelegramNotifyService {
    private static final Logger logger = Logger.getLogger(TelegramNotifyService.class);
    /** sys_config表中保存Telegram Bot Token的配置键 */
    private static final String BOT_TOKEN_KEY = "telegram.bot.token";
    /** 通知冷却默认配置键，单位秒 */
    private static final String DEFAULT_COOLDOWN_KEY = "telegram.notice.cooldown.default";
    /** 通知冷却按类型配置前缀，完整键为 telegram.notice.cooldown.{noticeType} */
    private static final String TYPE_COOLDOWN_KEY_PREFIX = "telegram.notice.cooldown.";
    /** 通知限流Redis Key前缀 */
    private static final String NOTICE_LIMIT_KEY_PREFIX = "TELEGRAM_NOTICE_LIMIT:";
    /** 默认通知冷却时间，单位秒 */
    private static final long DEFAULT_COOLDOWN_SECONDS = 600L;
    /** 余额不足默认冷却时间，单位秒 */
    private static final long BALANCE_LOW_DEFAULT_COOLDOWN_SECONDS = 1800L;
    /** 余额不足通知类型，和robot_telegram_channel.notice_types保持一致 */
    private static final String NOTICE_TYPE_BALANCE_LOW = "balance_low";
    /** 撞单按事件通知，不使用冷却。 */
    private static final String NOTICE_TYPE_ORDER_COLLISION = "order_collision";

    /** 系统配置读取，用来获取Bot Token */
    @Autowired
    private SysConfigMapper sysConfigMapper;
    /** 接收目标读取，用来获取当前机器人需要推送的群组或用户 */
    @Autowired
    private TelegramChannelMapper telegramChannelMapper;
    /** 发送日志写入，用来追踪Telegram接口返回结果 */
    @Autowired
    private TelegramSendLogMapper telegramSendLogMapper;
    /** Redis缓存，用来做通知冷却限流 */
    @Autowired
    private RedisHelper redisHelper;
    /** 从实际机器人记录读取typename，避免把记录ID当成群绑定标识 */
    @Autowired
    private RobotMapper robotMapper;

    /**
     * 发送Hotcoin余额不足通知。
     */
    public void sendHotcoinBalanceNotice(Integer robotId, String market, String coinName, String balance) {
        String message = "Hotcoin余额不足\n"
                + "机器人ID：" + robotId + "\n"
                + "交易对：" + emptyToDash(market) + "\n"
                + "币种：" + emptyToDash(coinName) + "\n"
                + "当前余额：" + emptyToDash(balance);
        sendRobotNotice(robotId, NOTICE_TYPE_BALANCE_LOW, coinName, message);
    }

    public void sendHotcoinCollisionNotice(Integer robotId, String market, String detail) {
        String message = "Hotcoin撞单预警\n"
                + "机器人ID：" + robotId + "\n"
                + "交易对：" + emptyToDash(market) + "\n"
                + "撞单详情：" + emptyToDash(detail);
        sendRobotNotice(robotId, NOTICE_TYPE_ORDER_COLLISION, message);
    }

    /**
     * 兼容旧调用方式；未传dedupeKey时，按通知类型整体限流。
     */
    public void sendRobotNotice(Integer robotId, String noticeType, String message) {
        sendRobotNotice(robotId, noticeType, "", message);
    }

    /**
     * 保留记录ID调用方式，在服务内部解析typename；通知异常不能影响交易主流程。
     */
    public void sendRobotNotice(Integer robotId, String noticeType, String dedupeKey, String message) {
        try {
            sendNoticeByTypename(robotId, noticeType, dedupeKey, message);
        } catch (Exception e) {
            logger.warn("Telegram通知处理失败，robotId=" + robotId, e);
        }
    }

    /** 按typename路由通知，同类型记录共享通知类型和业务去重键的冷却状态。 */
    private void sendNoticeByTypename(Integer robotId, String noticeType, String dedupeKey, String message) {
        String typename = findTypename(robotId);
        if (StringUtils.isBlank(typename)) {
            return;
        }
        String limitKey = buildLimitKey(typename, noticeType, dedupeKey);

        // Bot Token统一从系统配置读取，避免把密钥写死在代码里。
        SysConfig tokenConfig = sysConfigMapper.selectByConfigKey(BOT_TOKEN_KEY);
        if (tokenConfig == null || StringUtils.isBlank(tokenConfig.getConfigValue())) {
            logger.info("Telegram Bot Token未配置");
            return;
        }

        // notice_types为空表示接收所有通知；否则只接收匹配类型。
        List<TelegramChannel> channels = telegramChannelMapper.selectEnabledByTypenameAndNoticeType(typename, noticeType);
        if (channels == null || channels.isEmpty()) {
            logger.info("机器人未配置Telegram通知目标，typename=" + typename + ", noticeType=" + noticeType);
            return;
        }

        // 发送前原子占用冷却Key，防止同typename的多条记录并发重复推送。
        long cooldownSeconds = getCooldownSeconds(noticeType);
        if (cooldownSeconds > 0 && !redisHelper.setIfAbsent(limitKey,
                String.valueOf(System.currentTimeMillis()), cooldownSeconds)) {
            logger.info("Telegram通知处于冷却期，limitKey=" + limitKey);
            return;
        }

        for (TelegramChannel channel : channels) {
            sendToChannel(tokenConfig.getConfigValue(), robotId, typename, channel, noticeType,
                    "机器人：" + typename + "\n" + message);
        }
    }

    /**
     * 清除指定通知的冷却状态，适合余额恢复等场景使用。
     */
    public void clearRobotNoticeCooldown(Integer robotId, String noticeType, String dedupeKey) {
        try {
            String typename = findTypename(robotId);
            if (StringUtils.isNotBlank(typename)) {
                redisHelper.remove(buildLimitKey(typename, noticeType, dedupeKey));
            }
        } catch (Exception e) {
            logger.warn("Telegram通知冷却清除失败，robotId=" + robotId, e);
        }
    }

    /**
     * 清除Hotcoin余额不足通知冷却状态。
     */
    public void clearHotcoinBalanceNoticeCooldown(Integer robotId, String coinName) {
        clearRobotNoticeCooldown(robotId, NOTICE_TYPE_BALANCE_LOW, coinName);
    }

    /**
     * 调用Telegram Bot API发送消息到指定chat_id。
     */
    private void sendToChannel(String botToken, Integer robotId, String typename, TelegramChannel channel, String noticeType, String message) {
        TelegramSendLog sendLog = new TelegramSendLog();
        sendLog.setRobotId(robotId);
        sendLog.setTypename(typename);
        sendLog.setChannelId(channel.getId());
        sendLog.setNoticeType(noticeType);
        sendLog.setMessage(message);
        sendLog.setSuccess(0);

        try {
            // Telegram sendMessage接口使用表单参数提交。
            Map<String, Object> params = new HashMap<>();
            params.put("chat_id", channel.getChatId());
            params.put("text", message);
            params.put("disable_web_page_preview", "true");
            String response = HttpUtil.post("https://api.telegram.org/bot" + botToken + "/sendMessage", params);
            sendLog.setResponse(response);

            // 只有Telegram返回ok=true才认为本次发送成功。
            JSONObject responseJson = JSONObject.fromObject(response);
            if (responseJson != null && responseJson.getBoolean("ok")) {
                sendLog.setSuccess(1);
            }
        } catch (Exception e) {
            sendLog.setResponse(e.getMessage());
            logger.info("Telegram发送失败：" + e.getMessage());
        } finally {
            insertSendLog(sendLog);
        }
    }

    /**
     * 保存发送日志；日志保存失败不能影响余额查询和交易主流程。
     */
    private void insertSendLog(TelegramSendLog sendLog) {
        try {
            telegramSendLogMapper.insert(sendLog);
        } catch (Exception e) {
            logger.info("Telegram发送日志保存失败：" + e.getMessage());
        }
    }

    /**
     * 读取实际记录的typename；缺失时跳过通知，避免误发到其他机器人绑定的群。
     */
    private String findTypename(Integer robotId) {
        Robot robot = robotId == null ? null : robotMapper.selectByPrimaryKey(robotId);
        if (robot == null || StringUtils.isBlank(robot.getTypename())) {
            logger.info("机器人typename未配置，跳过Telegram通知，robotId=" + robotId);
            return null;
        }
        return robot.getTypename().trim();
    }

    /**
     * 获取通知类型的冷却秒数，配置不存在时使用代码默认值。
     */
    private long getCooldownSeconds(String noticeType) {
        if (NOTICE_TYPE_ORDER_COLLISION.equals(noticeType)) {
            return 0L;
        }
        Long typeCooldown = getLongConfig(TYPE_COOLDOWN_KEY_PREFIX + noticeType);
        if (typeCooldown != null) {
            return typeCooldown;
        }

        Long defaultCooldown = getLongConfig(DEFAULT_COOLDOWN_KEY);
        if (defaultCooldown != null) {
            return defaultCooldown;
        }

        if (NOTICE_TYPE_BALANCE_LOW.equals(noticeType)) {
            return BALANCE_LOW_DEFAULT_COOLDOWN_SECONDS;
        }
        return DEFAULT_COOLDOWN_SECONDS;
    }

    /**
     * 从系统配置读取Long类型配置，格式错误时忽略该配置。
     */
    private Long getLongConfig(String configKey) {
        SysConfig config = sysConfigMapper.selectByConfigKey(configKey);
        if (config == null || StringUtils.isBlank(config.getConfigValue())) {
            return null;
        }

        try {
            return Long.valueOf(config.getConfigValue());
        } catch (Exception e) {
            logger.info("Telegram通知冷却配置格式错误，configKey=" + configKey + ", configValue=" + config.getConfigValue());
            return null;
        }
    }

    /**
     * 生成通知限流Key，维度为typename、通知类型和业务去重键。
     */
    private String buildLimitKey(String typename, String noticeType, String dedupeKey) {
        return NOTICE_LIMIT_KEY_PREFIX
                + normalizeKeyPart(typename)
                + ":"
                + normalizeKeyPart(noticeType)
                + ":"
                + normalizeKeyPart(dedupeKey);
    }

    /**
     * 规范化Redis Key片段，避免空格和冒号影响Key结构。
     */
    private String normalizeKeyPart(String value) {
        return emptyToDefault(value, "default").trim().replace(":", "_").replace(" ", "_");
    }

    /**
     * 空字符串兜底为默认值。
     */
    private String emptyToDefault(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    /**
     * 通知文案兜底展示，避免空值导致消息不完整。
     */
    private String emptyToDash(String value) {
        return StringUtils.isBlank(value) ? "--" : value;
    }
}
