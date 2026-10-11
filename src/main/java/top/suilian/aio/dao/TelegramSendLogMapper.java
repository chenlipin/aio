package top.suilian.aio.dao;

import top.suilian.aio.model.TelegramSendLog;

/**
 * Telegram发送日志写入接口。
 */
public interface TelegramSendLogMapper {
    /**
     * 保存一次Telegram通知发送结果。
     */
    int insert(TelegramSendLog record);
}
