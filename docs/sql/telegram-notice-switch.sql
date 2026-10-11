-- 部署通知开关功能前执行一次；默认开启，保持已有主动通知行为。
ALTER TABLE robot_telegram_channel
    ADD COLUMN notice_enabled TINYINT NOT NULL DEFAULT 1
    COMMENT '主动通知开关：1开启，0关闭；不影响群查询指令';
