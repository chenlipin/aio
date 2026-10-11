package top.suilian.aio.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.dao.RobotArgsMapper;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.dao.TelegramChannelMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TelegramChannel;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.hotcoin.HotcoinBalanceQueryService;
import top.suilian.aio.service.hotcoin.HotcoinMarketQueryService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 处理当前群绑定的查询及通知开关指令，不提供交易控制功能。 */
@Service
public class TelegramCommandService {
    /** 同一个群的所有指令共享5秒冷却，冷却期静默忽略以避免提示刷屏。 */
    private static final long COMMAND_COOLDOWN_SECONDS = 5;
    /** 限制单次查询账户数，控制交易所请求量和消息处理时间。 */
    private static final int MAX_ACCOUNTS = 5;
    private static final Logger logger = Logger.getLogger(TelegramCommandService.class);
    @Autowired
    private TelegramChannelMapper channelMapper;
    @Autowired
    private RobotMapper robotMapper;
    @Autowired
    private RobotArgsMapper argsMapper;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private HotcoinBalanceQueryService balanceQuery;
    /** 状态指令共用一次公共行情查询，不逐条记录重复请求。 */
    @Autowired
    private HotcoinMarketQueryService marketQuery;
    @Autowired
    private TelegramApiClient apiClient;

    /** 解析普通群和超级群的指令；私聊、机器人消息及发给其他Bot的指令均忽略。 */
    public void handle(String token, String username, JSONObject update) {
        JSONObject message = update.getJSONObject("message");
        if (message == null || message.getJSONObject("chat") == null) {
            return;
        }
        JSONObject chat = message.getJSONObject("chat");
        String type = chat.getString("type");
        if (!"group".equals(type) && !"supergroup".equals(type)) {
            return;
        }
        JSONObject from = message.getJSONObject("from");
        if (from != null && from.getBooleanValue("is_bot")) {
            return;
        }
        String text = message.getString("text");
        if (StringUtils.isBlank(text)) {
            return;
        }
        // 中文快捷消息只接受完整匹配，避免普通聊天误触发通知开关。
        text = text.trim();
        if ("打开通知".equals(text)) {
            text = "/notify_on";
        } else if ("关闭通知".equals(text)) {
            text = "/notify_off";
        } else if (!text.startsWith("/")) {
            return;
        }
        String command = text.trim().split("\\s+", 2)[0];
        int at = command.indexOf('@');
        if (at >= 0) {
            if (!command.substring(at + 1).equalsIgnoreCase(username)) {
                return;
            }
            command = command.substring(0, at);
        }
        command = command.toLowerCase(Locale.ROOT);
        String chatId = chat.getString("id");
        // 只记录已知指令名称，不记录完整消息或参数，避免用户误发的密钥进入日志。
        boolean noticeSwitch = "/notify_on".equals(command) || "/notify_off".equals(command);
        String logCommand = "/help".equals(command) || "/balance".equals(command) || "/status".equals(command) || noticeSwitch
                ? command : "未知指令";
        logger.info("Telegram收到群指令，chatId=" + chatId + ", command=" + logCommand
                + ", messageId=" + message.getLongValue("message_id"));
        // 查询时不使用用户输入的机器人ID或typename，防止跨群读取其他账户。
        TelegramChannel binding = channelMapper.selectEnabledBindingByChatId(chatId);
        if (binding == null || StringUtils.isBlank(binding.getTypename())) {
            logger.info("Telegram群未配置启用的typename绑定，忽略指令，chatId=" + chatId);
            return;
        }
        if (!redisHelper.setIfAbsent("TELEGRAM_COMMAND_COOLDOWN:" + chatId, "1", COMMAND_COOLDOWN_SECONDS)) {
            logger.info("Telegram群指令处于5秒冷却期，chatId=" + chatId + ", command=" + logCommand);
            return;
        }
        logger.info("Telegram群绑定匹配成功，chatId=" + chatId + ", typename=" + binding.getTypename());
        String reply;
        if (!"/help".equals(command) && !"/balance".equals(command) && !"/status".equals(command) && !noticeSwitch) {
            reply = "不支持该指令，请输入 /help。";
        } else if (text.trim().split("\\s+", 2).length > 1) {
            reply = "直接输入 " + command + " 即可，无需机器人ID或其他参数。";
        } else if ("/help".equals(command)) {
            reply = "/help 查看指令\n/balance 查询热币实时余额\n/status 查看本群机器人状态\n"
                    + "/notify_on 打开主动通知（管理员）\n/notify_off 关闭主动通知（管理员）\n"
                    + "也可发送：打开通知、关闭通知\n"
                    + "当前绑定：" + binding.getTypename();
        } else if (noticeSwitch) {
            reply = noticeSwitchReply(token, chatId, message, binding, "/notify_on".equals(command));
        } else {
            try {
                List<Robot> robots = robotMapper.selectByTypename(binding.getTypename());
                if (robots == null || robots.isEmpty()) {
                    reply = "当前群绑定的机器人没有可查询的记录。";
                } else {
                    reply = "/balance".equals(command) ? balanceReply(binding.getTypename(), robots)
                            : statusReply(robots);
                }
            } catch (Exception e) {
                logger.warn("Telegram群查询失败，channelId=" + binding.getId());
                reply = "查询失败，请稍后重试或检查后台配置。";
            }
        }
        // 回复失败时让轮询服务重试该更新；不能把失败回复当作已成功处理。
        try {
            apiClient.reply(token, chatId, message.getLongValue("message_id"), limitReply(reply));
            logger.info("Telegram群指令回复成功，chatId=" + chatId + ", command=" + logCommand);
        } catch (Exception e) {
            logger.warn("Telegram群指令回复失败，chatId=" + chatId + ", command=" + logCommand);
            redisHelper.remove("TELEGRAM_COMMAND_COOLDOWN:" + chatId);
            throw e;
        }
    }

    /** 仅管理员可切换本群主动通知，保存至数据库以便重启后继续生效。 */
    private String noticeSwitchReply(String token, String chatId, JSONObject message,
                                     TelegramChannel binding, boolean enabled) {
        JSONObject from = message.getJSONObject("from");
        // 匿名管理员及频道代发消息无法可靠核验用户身份，要求改用个人身份。
        if (message.getJSONObject("sender_chat") != null || from == null || from.getLongValue("id") <= 0) {
            return "请群主或管理员使用个人身份切换通知。";
        }
        try {
            if (!apiClient.isGroupAdministrator(token, chatId, from.getLongValue("id"))) {
                return "只有群主或管理员可以打开、关闭主动通知。";
            }
        } catch (Exception e) {
            logger.warn("Telegram通知开关权限校验失败，chatId=" + chatId);
            return "无法核验管理员身份，请检查机器人群管理员权限后重试。";
        }
        int target = enabled ? 1 : 0;
        try {
            int changed = channelMapper.updateNoticeEnabled(binding.getId(), binding.getTypename(), target);
            // 数据库可能对重复设置返回0；重新读取，不能把失效绑定误报为成功。
            if (changed == 0) {
                TelegramChannel current = channelMapper.selectEnabledBindingByChatId(chatId);
                if (current == null || !binding.getId().equals(current.getId())
                        || !binding.getTypename().equals(current.getTypename())
                        || !Integer.valueOf(target).equals(current.getNoticeEnabled())) {
                    return "通知开关未更新，请检查群绑定后重试。";
                }
            }
            logger.info("Telegram主动通知开关更新，chatId=" + chatId + ", typename="
                    + binding.getTypename() + ", enabled=" + target);
            return "本群主动通知已" + (enabled ? "打开" : "关闭") + "，余额、状态等查询不受影响。";
        } catch (Exception e) {
            logger.warn("Telegram通知开关保存失败，chatId=" + chatId);
            return "通知开关保存失败，请检查数据库配置后重试。";
        }
    }

    /** 热币按API Key去重，分别展示不同账户；不会读取或修改共享交易服务状态。 */
    private String balanceReply(String typename, List<Robot> robots) {
        StringBuilder reply = new StringBuilder("机器人：" + typename + "\nHotcoin实时余额\n");
        Set<String> queriedKeys = new HashSet<>();
        int accountCount = 0;
        for (Robot robot : robots) {
            if (!Integer.valueOf(Constant.KEY_EXCHANGE_HOTCOIN).equals(robot.getStrategyId())) {
                continue;
            }
            Map<String, String> params = params(robot.getRobotId());
            String apiKey = params.get("apikey");
            if (StringUtils.isBlank(apiKey)) {
                reply.append("记录 ").append(robot.getRobotId()).append("：未配置API Key\n");
                continue;
            }
            if (!queriedKeys.add(apiKey)) {
                logger.info("Telegram余额查询跳过重复API账户，robotId=" + robot.getRobotId());
                continue;
            }
            if (accountCount >= MAX_ACCOUNTS) {
                reply.append("单次最多查询5个账户，其余账户请在后台查看。\n");
                break;
            }
            reply.append("\n账户 ").append(++accountCount).append("（记录 ")
                    .append(robot.getRobotId()).append("）\n");
            try {
                logger.info("Telegram开始查询热币余额，typename=" + typename + ", robotId=" + robot.getRobotId());
                List<Map<String, String>> assets = balanceQuery.query(params);
                logger.info("Telegram热币余额查询成功，robotId=" + robot.getRobotId()
                        + ", nonZeroAssets=" + assets.size());
                if (assets.isEmpty()) {
                    reply.append("所有币种余额为0\n");
                }
                int lines = 0;
                for (Map<String, String> asset : assets) {
                    // 限制各账户的显示数量，确保其他账户也能出现在回复中。
                    if (lines++ >= 10) {
                        reply.append("其余币种请在后台查看\n");
                        break;
                    }
                    reply.append(asset.get("coinName")).append("：总额 ").append(asset.get("total"))
                            .append("，冻结 ").append(asset.get("freeze")).append('\n');
                }
            } catch (Exception e) {
                logger.warn("Telegram热币余额查询失败，robotId=" + robot.getRobotId());
                reply.append("查询失败，请检查账户API配置或稍后重试\n");
            }
        }
        if (accountCount == 0) {
            reply.append("没有可查询的热币账户，目前/balance仅支持热币。\n");
        }
        return reply.toString();
    }

    /** 仅展示active=1记录的后台状态，名称使用机器人表name字段。 */
    private String statusReply(List<Robot> robots) {
        StringBuilder reply = new StringBuilder("状态\n");
        // 保持交易对首次出现顺序，同一公共行情不随机器人记录重复展示。
        Set<String> displayedMarkets = new LinkedHashSet<>();
        // 一次公共行情请求同时取得所有交易对的价格及交易量，无需账户成交记录查询。
        Map<String, Map<String, String>> latestTickers = new HashMap<>();
        // 仅在当前输出范围存在启用的热币记录时获取公共行情。
        if (robots.stream().anyMatch(robot -> robot.getActive() == 1
                && Integer.valueOf(Constant.KEY_EXCHANGE_HOTCOIN).equals(robot.getStrategyId()))) {
            try {
                latestTickers = marketQuery.latestTickers();
            } catch (Exception e) {
                logger.warn("Telegram状态查询未获取热币最新行情");
            }
        }
        int shown = 0;
        for (Robot robot : robots) {
            // active过滤仅适用于状态展示，不改变余额查询的账户范围。
            if (robot.getActive() != 1) {
                continue;
            }
            if (shown++ >= 30) {
                reply.append("其余记录请在后台查看\n");
                break;
            }
            Map<String, String> params = params(robot.getRobotId());
            // 展示数据库机器人ID，便于将群内状态与后台记录对应。
            reply.append("ID：").append(robot.getRobotId()).append(" | ")
                    .append(StringUtils.isBlank(robot.getName()) ? "未命名机器人" : robot.getName()).append(" | ")
                    .append(statusName(robot.getStatus())).append('\n');
            if (Integer.valueOf(Constant.KEY_EXCHANGE_HOTCOIN).equals(robot.getStrategyId())) {
                String market = params.get("market");
                if (StringUtils.isNotBlank(market)) {
                    String normalizedMarket = market.trim().toLowerCase(Locale.ROOT);
                    // 所有交易对均保留行情，名称隐藏规则仅在生成回复时生效。
                    displayedMarkets.add(normalizedMarket);
                }
            }
        }
        if (shown == 0) {
            reply.append("当前群没有active=1的机器人记录。\n");
        }
        if (displayedMarkets.isEmpty()) {
            return reply.toString();
        }
        // 公共价格和成交量统一放在机器人状态前，每个交易对只显示一次。
        StringBuilder publicMarket = new StringBuilder("公共行情\n");
        for (String market : displayedMarkets) {
            Map<String, String> ticker = latestTickers.get(market);
            String price = ticker == null ? null : ticker.get("price");
            String volume = ticker == null ? null : ticker.get("volume24h");
            // gnr_usdt仅隐藏交易对名称，不隐藏其价格和24小时成交量。
            if (!"gnr_usdt".equals(market)) {
                publicMarket.append(market).append('\n');
            }
            publicMarket.append("最新成交价：").append(price == null ? "未获取" : price).append('\n')
                    .append("24成交量：").append(volume == null ? "未获取" : volume).append('\n');
        }
        return publicMarket.append('\n').append(reply).toString();
    }

    /** 读取该记录的参数，密钥仅用于后端请求，不写入群消息或日志。 */
    private Map<String, String> params(Integer robotId) {
        Map<String, String> result = new HashMap<>();
        List<RobotArgs> args = argsMapper.selectAll(robotId);
        if (args != null) {
            for (RobotArgs arg : args) {
                result.put(arg.getVariable(), arg.getValue());
            }
        }
        return result;
    }

    /** 按现有机器人状态常量显示中文状态。 */
    private String statusName(int status) {
        switch (status) {
            case Constant.KEY_ROBOT_STATUS_FREE: return "空闲中";
            case Constant.KEY_ROBOT_STATUS_RUN: return "运行中";
            case Constant.KEY_ROBOT_STATUS_STOP: return "停止中";
            case Constant.KEY_ROBOT_STATUS_OUT: return "已退出";
            case Constant.KEY_ROBOT_STATUS_STOPPED: return "已停止";
            case Constant.KEY_ROBOT_STATUS_ERROR: return "策略错误";
            case Constant.KEY_ROBOT_STATUS_OVERDUE: return "已到期";
            default: return "未知状态(" + status + ")";
        }
    }

    /** 控制消息长度，截断时保留完整Unicode字符并提示省略内容。 */
    private String limitReply(String text) {
        if (text.length() <= 3800) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(3799)) ? 3799 : 3800;
        return text.substring(0, end) + "\n内容过多，剩余数据请在后台查看。";
    }
}
