# Telegram 群指令接入

当前支持 `/help`、`/balance`、`/status`。无需填写机器人ID，权限来自当前群启用的 `typename` 绑定。绑定群里的所有成员均可执行这三个只读指令，私聊不支持。

另支持管理员指令 `/notify_on`（打开通知）、`/notify_off`（关闭通知），只控制当前群绑定的主动推送，不控制交易策略，查询回复不受影响。

热币新旧K线支持 `order_collision` 撞单主动预警，每次检测到外部成交，或按现有防撞单逻辑计入的撤单，推送一次。同一笔订单在一次检查中既撤单又有外部成交时，只推送一次，并展示外部成交明细。完全成交且全部为自成交、主动吃盘口不触发撞单推送。撞单通知不使用冷却，忽略全局和类型冷却配置，仍受群启用状态、订阅类型和 `/notify_off` 开关控制。

部署前，已有 `notice_types='balance_low'` 等余额订阅的群执行一次 [撞单订阅SQL](sql/telegram-order-collision-notice.sql)，追加 `order_collision`；空订阅已接收全部通知，无需修改。SQL不会打开已关闭的通知。通知发送失败保留发送日志，不自动补发。

## 主动通知开关升级

部署新代码前，执行一次 [通知开关SQL](sql/telegram-notice-switch.sql)：

```sql
-- 默认开启，保持现有通知行为；已有该字段时不要重复执行。
ALTER TABLE robot_telegram_channel
    ADD COLUMN notice_enabled TINYINT NOT NULL DEFAULT 1
    COMMENT '主动通知开关：1开启，0关闭；不影响群查询指令';
```

群主或管理员发送 `/notify_on`、`/notify_off`，也可直接发送完整中文“打开通知”“关闭通知”。中文普通消息需要Bot能接收群消息；隐私模式下优先使用 `/notify_off@你的Bot用户名`。匿名管理员请切换个人身份操作。

Bot应设为群管理员以可靠核验发起人的管理员身份；无法核验时不会修改开关。权限查询采用官方 [getChatMember](https://core.telegram.org/bots/api#getchatmember)。

开关保存到 `robot_telegram_channel.notice_enabled`，重启后保留；不会修改 `notice_types`、群或绑定的 `enabled`。关闭后停止本群所有主动通知类型，已有在途推送可能仍会送达。重新打开不补发历史通知、不清空原有通知冷却，后续符合条件的通知按原冷却规则发送。

## 1. 配置群与机器人绑定

使用现有的 `telegram_channel` 和 `robot_telegram_channel` 表。已配置的群不用重复插入。新版创建机器人页面支持直接选择候选群：先把Bot拉入群并发送 `/help@Bot用户名`，轮询收到群消息后将群名与ID记为未启用候选；不自动绑定，不开放查询。管理员页面选择群并创建成功后才启用与绑定，无需手工执行以下SQL。候选可能是历史群，仍须确认Bot未被移除及有发送权限。已有绑定名称和开关不会被群发现覆盖。

以下示例仅适用于仍需手工配置的场景，必须先替换群ID和typename：

```sql
-- 使用真实的群chat_id，群ID通常为负数；不能使用群名称代替。
SET @telegram_chat_id = '-100填写真实群ID';
-- 必须与robot表中的typename一致。
SET @robot_typename = '填写机器人表中的typename';

-- 创建群记录；同一个chat_id不要配置多个channel记录。
INSERT INTO telegram_channel(name, chat_id, enabled, created_at, updated_at)
SELECT '热币机器人群', @telegram_chat_id, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM telegram_channel WHERE chat_id = @telegram_chat_id
);

-- 一个群只绑定一个typename；已有绑定则更新typename和启用状态。
INSERT INTO robot_telegram_channel(typename, channel_id, notice_types, enabled, created_at, updated_at)
SELECT @robot_typename, id, 'balance_low,order_collision', 1, NOW(), NOW()
FROM telegram_channel WHERE chat_id = @telegram_chat_id
ON DUPLICATE KEY UPDATE typename = VALUES(typename), enabled = 1, updated_at = NOW();

-- 确保群本身启用。
UPDATE telegram_channel SET enabled = 1, updated_at = NOW()
WHERE chat_id = @telegram_chat_id;
```

`notice_types` 只影响主动通知，不影响查询指令。停用群或绑定后，该群的指令会被忽略。

## 2. 配置Token并开启接收

确保 `sys_config` 中启用的 `telegram.bot.token` 保存了当前有效Token，不要把Token写入源码或文档。确认服务器能访问 `https://api.telegram.org` 和 `https://api.hotcoinfin.com`，所有接收实例连接同一Redis。

```sql
-- 首次添加开关；默认只有配置值为1或true时才访问Telegram接收指令。
INSERT INTO sys_config(config_key, config_value, remark, enabled, created_at, updated_at)
SELECT 'telegram.command.enabled', '1', '开启Telegram群指令长轮询', 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'telegram.command.enabled'
);

-- 已存在开关时启用。
UPDATE sys_config
SET config_value = '1', enabled = 1, updated_at = NOW()
WHERE config_key = 'telegram.command.enabled';
```

重新构建并部署 `aio` 后开始接收。开启后的配置变化通常在当前长轮询和指令处理结束后生效；关闭时把开关设为 `0`，已开始的查询可能继续完成。

若Bot已经配置Webhook，需要先使用Telegram官方 `deleteWebhook` 方法移除，轮询服务不会替你自动移除。配置开关后无需公网回调接口。

## 3. 配置群权限和指令菜单

将Bot加入对应群，并确保它能发送消息。为了稳定使用不带Bot名称的 `/balance`，可将Bot设置为群管理员。也可以在BotFather中关闭隐私模式并重新加入群；开启隐私模式时，可以使用 `/balance@你的Bot用户名` 明确发给当前Bot。

在BotFather的 `/setcommands` 中选择当前Bot，提交：

```text
help - 查看指令
balance - 查询热币实时余额
status - 查看机器人状态
notify_on - 打开主动通知 / Enable notifications
notify_off - 关闭主动通知 / Disable notifications
```

菜单配置只负责显示指令，实际接收和查询由 `aio` 处理。

## 4. 验证

先发 `/help`，确认回复显示正确的typename；等待5秒后发 `/status`；再等待5秒发 `/balance`。

- `/balance` 当前仅支持热币（strategy_id=18），使用每条记录的 `apikey` 和 `tpass` 调用余额接口。相同API Key只查询一次，不同API Key分别显示，单次最多查询5个账户，每个账户最多显示10个非零余额币种。不同API Key如果属于同一个实际账户，仍会分别显示。
- `/status` 仅显示active=1的记录，名称使用robot表的name字段，机器人行展示名称及数据库状态，不代表实时进程健康检查。交易所 `/v1/market/ticker` 接口的 `last` 最新成交价和 `vol` 24小时市场成交量统一放在公共行情部分，同交易对只显示一次，不同交易对分别展示。每条状态指令只请求一次公共行情，无需账户API配置，不查询账户成交记录。
- 交易量是该交易对全市场最近24小时量，不是机器人自身成交量，也不是北京时间自然日量。接口失败、交易对未找到或字段异常时显示“未获取”；接口明确返回0时才显示0。
- 同一群的全部指令共享5秒冷却，冷却期间静默忽略。
- 指令接收使用独立线程，Redis锁协调多个实例，offset持久保存。超过5分钟的旧消息不会执行。正常关闭时立即按owner释放轮询锁；启动时根据本实例上次登记的owner清理异常退出遗留锁，不删除其他实例持有的锁，也不清空offset。
- 正常回复后保存处理记录；在发送成功但进程尚未保存处理状态时崩溃，仍可能产生重复回复，不能保证严格只执行一次。
- 指令查询不会修改交易服务参数、余额缓存，也不会触发主动余额不足告警。

没有回复时依次检查开关、Token是否有效、群与绑定是否启用、typename、群发送权限、隐私模式、Webhook、服务器网络和Redis。不要在服务开启时手动调用getUpdates或让其他程序轮询同一个Bot。

## 本地日志

查看运行 `aio` 的IDE控制台或启动终端。现有log4j配置也将日志写入启动工作目录下的 `logs/all.log`；如果从 `C:\robotWork\aio` 启动，路径为 `C:\robotWork\aio\logs\all.log`。

启动后会显示指令线程启动、开关未开启、Token未配置、等待其他实例释放轮询锁或Bot连接成功等状态。正常接收时每分钟输出一次心跳；收到指令后显示群绑定匹配、查询结果及回复成功或失败。日志不输出Token、账户密钥或完整消息内容。

重启实例默认以主机名加server.port识别。容器主机名或端口会变化时，在应用配置中设置固定的 `telegram.command.instance-id`（例如 `hotcoin-local`）；同一服务重启保持一致，不同并行服务必须使用不同值。这个配置放在应用properties或启动参数中，不是sys_config数据库项。首次从旧版本升级时，因为旧锁没有实例登记信息，可能仍需等待旧锁最多5分钟自动过期。

官方参考：[getUpdates](https://core.telegram.org/bots/api#getupdates)、[deleteWebhook](https://core.telegram.org/bots/api#deletewebhook)、[群隐私模式](https://core.telegram.org/bots/features#privacy-mode)。

热币数据口径参考：[行情接口](https://hotcoinex.github.io/zh/spot/market.html)。
