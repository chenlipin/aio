package top.suilian.aio.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.suilian.aio.dao.RobotArgsMapper;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.dao.TelegramChannelMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.TelegramChannel;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.hotcoin.HotcoinBalanceQueryService;
import top.suilian.aio.service.hotcoin.HotcoinMarketQueryService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证群权限、API账户去重及只读指令回复，不连接任何真实外部服务。 */
public class TelegramCommandServiceTest {
    private TelegramCommandService service;
    private TelegramChannelMapper channels;
    private RobotMapper robots;
    private RobotArgsMapper args;
    private RedisHelper redis;
    private TelegramApiClient api;
    private HotcoinBalanceQueryService balances;
    private HotcoinMarketQueryService markets;

    /** 建立启用群绑定，并为查询依赖注入模拟对象。 */
    @Before
    public void setUp() {
        service = new TelegramCommandService();
        channels = mock(TelegramChannelMapper.class);
        robots = mock(RobotMapper.class);
        args = mock(RobotArgsMapper.class);
        redis = mock(RedisHelper.class);
        api = mock(TelegramApiClient.class);
        balances = mock(HotcoinBalanceQueryService.class);
        markets = mock(HotcoinMarketQueryService.class);
        ReflectionTestUtils.setField(service, "channelMapper", channels);
        ReflectionTestUtils.setField(service, "robotMapper", robots);
        ReflectionTestUtils.setField(service, "argsMapper", args);
        ReflectionTestUtils.setField(service, "redisHelper", redis);
        ReflectionTestUtils.setField(service, "apiClient", api);
        ReflectionTestUtils.setField(service, "balanceQuery", balances);
        ReflectionTestUtils.setField(service, "marketQuery", markets);
        Map<String, String> ticker = new java.util.HashMap<>();
        ticker.put("price", "123.456");
        ticker.put("volume24h", "9876.543");
        when(markets.latestTickers()).thenReturn(Collections.singletonMap("btc_usdc", ticker));
        TelegramChannel binding = new TelegramChannel();
        binding.setId(1);
        binding.setTypename("hotcoin-a");
        when(channels.selectEnabledBindingByChatId("-100123")).thenReturn(binding);
        when(redis.setIfAbsent(anyString(), anyString(), eq(5L))).thenReturn(true);
    }

    /** 同API Key的多个交易对只查询一次，群回复不能包含密钥。 */
    @Test
    public void balanceDeduplicatesApiKeyAndRepliesToOriginalGroup() {
        Robot first = robot(1);
        Robot second = robot(2);
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Arrays.asList(first, second));
        for (int id : new int[]{1, 2}) {
            when(args.selectAll(id)).thenReturn(Arrays.asList(arg("apikey", "secret-api-key"),
                    arg("tpass", "secret-password"), arg("market", "btc_usdc")));
        }
        Map<String, String> asset = new java.util.HashMap<>();
        asset.put("coinName", "USDC");
        asset.put("total", "15.12");
        asset.put("freeze", "2.00");
        when(balances.query(anyMap())).thenReturn(Collections.singletonList(asset));
        service.handle("test", "test_bot", update("/balance"));
        verify(balances, times(1)).query(anyMap());
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("15.12") && text.contains("2.00") && text.contains("hotcoin-a")
                        && !text.contains("secret-api-key") && !text.contains("secret-password")));
    }

    /** 未绑定群、私聊和指向其他Bot的指令不能获取机器人数据。 */
    @Test
    public void unauthorizedOrWrongDestinationIsIgnored() {
        JSONObject unbound = update("/balance");
        unbound.getJSONObject("message").getJSONObject("chat").put("id", -100999L);
        service.handle("test", "test_bot", unbound);
        JSONObject privateChat = update("/balance");
        privateChat.getJSONObject("message").getJSONObject("chat").put("type", "private");
        service.handle("test", "test_bot", privateChat);
        service.handle("test", "test_bot", update("/balance@another_bot"));
        verify(robots, never()).selectByTypename(anyString());
        verify(api, never()).reply(anyString(), anyString(), anyLong(), anyString());
    }

    /** 带当前Bot名称的帮助指令可以使用，不需要读取交易账户。 */
    @Test
    public void addressedHelpIsSupported() {
        service.handle("test", "test_bot", update("/help@TEST_BOT"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("/balance"));
        verify(robots, never()).selectByTypename(anyString());
    }

    /** 管理员可用中文关闭本群推送，随后查询仍正常执行。 */
    @Test
    public void administratorCanDisableNoticesWithoutDisablingQueries() {
        when(api.isGroupAdministrator("test", "-100123", 123L)).thenReturn(true);
        when(channels.updateNoticeEnabled(1, "hotcoin-a", 0)).thenReturn(1);
        service.handle("test", "test_bot", adminUpdate("关闭通知"));
        verify(channels).updateNoticeEnabled(1, "hotcoin-a", 0);
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("主动通知已关闭"));
        // 模拟关闭开关后的绑定，余额查询不能因noticeEnabled=0被拦截。
        TelegramChannel binding = new TelegramChannel();
        binding.setId(1);
        binding.setTypename("hotcoin-a");
        binding.setNoticeEnabled(0);
        when(channels.selectEnabledBindingByChatId("-100123")).thenReturn(binding);
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Collections.singletonList(robot(1)));
        when(args.selectAll(1)).thenReturn(Arrays.asList(arg("apikey", "test-key"), arg("tpass", "test-pass")));
        when(balances.query(anyMap())).thenReturn(Collections.emptyList());
        service.handle("test", "test_bot", update("/balance"));
        service.handle("test", "test_bot", update("/status"));
        verify(balances).query(anyMap());
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("Hotcoin实时余额"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("状态\n"));
    }

    /** 中英文开启指令和带Bot名称的关闭指令均受管理员权限保护。 */
    @Test
    public void administratorCanUseChineseAndAddressedSwitchCommands() {
        when(api.isGroupAdministrator("test", "-100123", 123L)).thenReturn(true);
        when(channels.updateNoticeEnabled(eq(1), eq("hotcoin-a"), anyInt())).thenReturn(1);
        service.handle("test", "test_bot", adminUpdate("打开通知"));
        service.handle("test", "test_bot", adminUpdate("/notify_on"));
        service.handle("test", "test_bot", adminUpdate("/notify_off@TEST_BOT"));
        verify(channels, times(2)).updateNoticeEnabled(1, "hotcoin-a", 1);
        verify(channels).updateNoticeEnabled(1, "hotcoin-a", 0);
        verify(robots, never()).selectByTypename(anyString());
    }

    /** 普通成员和权限接口失败都不能修改通知设置。 */
    @Test
    public void nonAdministratorAndFailedPermissionCheckCannotSwitch() {
        service.handle("test", "test_bot", adminUpdate("/notify_off"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("只有群主或管理员"));
        when(api.isGroupAdministrator("test", "-100123", 123L)).thenThrow(new IllegalStateException("test"));
        service.handle("test", "test_bot", adminUpdate("/notify_on"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("无法核验"));
        verify(channels, never()).updateNoticeEnabled(anyInt(), anyString(), anyInt());
    }

    /** 匿名代发、跨群和指向其他Bot的消息不能切换主动通知。 */
    @Test
    public void anonymousAndWrongDestinationCannotSwitch() {
        JSONObject anonymous = adminUpdate("/notify_off");
        anonymous.getJSONObject("message").put("sender_chat", new JSONObject());
        service.handle("test", "test_bot", anonymous);
        JSONObject unbound = adminUpdate("关闭通知");
        unbound.getJSONObject("message").getJSONObject("chat").put("id", -100999L);
        service.handle("test", "test_bot", unbound);
        service.handle("test", "test_bot", adminUpdate("/notify_off@other_bot"));
        service.handle("test", "test_bot", update("/notify_off"));
        verify(api, never()).isGroupAdministrator(anyString(), anyString(), anyLong());
        verify(channels, never()).updateNoticeEnabled(anyInt(), anyString(), anyInt());
    }

    /** 重复设置时数据库返回0，必须核实保存状态后才回复成功。 */
    @Test
    public void repeatedSwitchChecksPersistedState() {
        when(api.isGroupAdministrator("test", "-100123", 123L)).thenReturn(true);
        TelegramChannel binding = new TelegramChannel();
        binding.setId(1);
        binding.setTypename("hotcoin-a");
        binding.setNoticeEnabled(0);
        when(channels.selectEnabledBindingByChatId("-100123")).thenReturn(binding);
        service.handle("test", "test_bot", adminUpdate("/notify_off"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("主动通知已关闭"));
        service.handle("test", "test_bot", adminUpdate("/notify_on"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("未更新"));
    }

    /** 带参数或普通聊天不能触发开关，保存失败时不误报成功。 */
    @Test
    public void switchRejectsArgumentsAndReportsDatabaseFailure() {
        service.handle("test", "test_bot", adminUpdate("/notify_off 2"));
        service.handle("test", "test_bot", adminUpdate("请关闭通知"));
        verify(channels, never()).updateNoticeEnabled(anyInt(), anyString(), anyInt());
        when(api.isGroupAdministrator("test", "-100123", 123L)).thenReturn(true);
        when(channels.updateNoticeEnabled(1, "hotcoin-a", 0)).thenThrow(new IllegalStateException("test"));
        service.handle("test", "test_bot", adminUpdate("/notify_off"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("保存失败"));
    }

    /** 指令冷却时静默忽略，不再发送冷却提示造成刷屏。 */
    @Test
    public void cooldownSuppressesQueryAndReply() {
        when(redis.setIfAbsent(anyString(), anyString(), eq(5L))).thenReturn(false);
        service.handle("test", "test_bot", update("/balance"));
        verify(balances, never()).query(anyMap());
        verify(api, never()).reply(anyString(), anyString(), anyLong(), anyString());
    }

    /** 状态仅显示active=1的记录，同时展示机器人ID、name及运行状态。 */
    @Test
    public void statusUsesBoundTypenameAndExistingStatusConstants() {
        Robot robot = robot(1);
        robot.setStatus(1);
        robot.setActive(1);
        robot.setName("热币主策略");
        Robot inactive = robot(2);
        inactive.setActive(0);
        inactive.setName("停用策略");
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Arrays.asList(inactive, robot));
        when(args.selectAll(1)).thenReturn(Arrays.asList(arg("market", "btc_usdc"),
                arg("apikey", "test-key"), arg("tpass", "test-secret")));
        service.handle("test", "test_bot", update("/status"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("btc_usdc") && text.contains("运行中") && text.contains("状态\n")
                        && !text.contains("后台记录")
                        && text.contains("热币主策略") && !text.contains("停用策略")
                        && text.contains("最新成交价：123.456")
                        && text.contains("24成交量：9876.543")
                        && text.contains("ID：1 | 热币主策略 | 运行中")
                        && !text.contains("ID：2 |") && !text.contains("hotcoin-a")));
        verify(args, never()).selectAll(2);
        verify(markets, times(1)).latestTickers();
        verify(balances, never()).query(anyMap());
    }

    /** 无active=1记录时给出明确提示，不查询停用记录的参数。 */
    @Test
    public void statusWithNoActiveRecordsShowsEmptyMessage() {
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Collections.singletonList(robot(1)));
        service.handle("test", "test_bot", update("/status"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), contains("没有active=1"));
        verify(args, never()).selectAll(anyInt());
        verify(markets, never()).latestTickers();
    }

    /** 多条启用记录共用一次市场查询，即使没有账户API配置也能显示交易量。 */
    @Test
    public void statusSharesMarketQueryWithoutAccountCredentials() {
        Robot first = robot(1);
        Robot second = robot(2);
        first.setActive(1);
        second.setActive(1);
        first.setName("策略A");
        second.setName("策略B");
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Arrays.asList(first, second));
        for (int id : new int[]{1, 2}) {
            when(args.selectAll(id)).thenReturn(Collections.singletonList(arg("market", id == 1 ? "BTC_USDC" : " btc_usdc ")));
        }
        service.handle("test", "test_bot", update("/status"));
        verify(markets, times(1)).latestTickers();
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("策略A") && text.contains("策略B") && text.contains("9876.543")
                        && org.apache.commons.lang.StringUtils.countMatches(text, "最新成交价：") == 1
                        && org.apache.commons.lang.StringUtils.countMatches(text, "24成交量：") == 1
                        && text.indexOf("公共行情") < text.indexOf("状态\n")));
    }

    /** 不同交易对分别展示一次，避免把多个市场的公共价格或交易量混在一起。 */
    @Test
    public void statusDisplaysEachDistinctMarketOnce() {
        Robot first = robot(1);
        Robot second = robot(2);
        first.setActive(1);
        second.setActive(1);
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Arrays.asList(first, second));
        when(args.selectAll(1)).thenReturn(Collections.singletonList(arg("market", "btc_usdc")));
        when(args.selectAll(2)).thenReturn(Collections.singletonList(arg("market", "eth_usdc")));
        Map<String, Map<String, String>> tickers = new java.util.HashMap<>();
        Map<String, String> btc = new java.util.HashMap<>();
        btc.put("price", "123");
        btc.put("volume24h", "9876");
        Map<String, String> eth = new java.util.HashMap<>();
        eth.put("price", "45");
        eth.put("volume24h", "6789");
        tickers.put("btc_usdc", btc);
        tickers.put("eth_usdc", eth);
        when(markets.latestTickers()).thenReturn(tickers);
        service.handle("test", "test_bot", update("/status"));
        verify(markets, times(1)).latestTickers();
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("btc_usdc") && text.contains("eth_usdc")
                        && text.contains("123") && text.contains("45")
                        && org.apache.commons.lang.StringUtils.countMatches(text, "最新成交价：") == 2
                        && org.apache.commons.lang.StringUtils.countMatches(text, "24成交量：") == 2));
    }

    /** gnr_usdt仅隐藏名称，大小写及空格不影响价格和成交量展示。 */
    @Test
    public void statusHidesGnrUsdtNameButKeepsMarketAndRobotStatus() {
        Robot first = robot(1);
        Robot second = robot(2);
        first.setActive(1);
        second.setActive(1);
        first.setName("GNR策略");
        second.setName("BTC策略");
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Arrays.asList(first, second));
        when(args.selectAll(1)).thenReturn(Collections.singletonList(arg("market", " GNR_USDT ")));
        when(args.selectAll(2)).thenReturn(Collections.singletonList(arg("market", "btc_usdc")));
        // 提供两个市场的独立行情，确保隐藏名称不会漏掉GNR数据。
        Map<String, Map<String, String>> tickers = new java.util.HashMap<>(markets.latestTickers());
        Map<String, String> gnr = new java.util.HashMap<>();
        gnr.put("price", "0.12");
        gnr.put("volume24h", "45678");
        tickers.put("gnr_usdt", gnr);
        when(markets.latestTickers()).thenReturn(tickers);
        service.handle("test", "test_bot", update("/status"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("GNR策略") && text.contains("BTC策略") && text.contains("btc_usdc")
                        && !text.toLowerCase(java.util.Locale.ROOT).contains("gnr_usdt")
                        && text.contains("最新成交价：0.12")
                        && text.contains("24成交量：45678")
                        && org.apache.commons.lang.StringUtils.countMatches(text, "最新成交价：") == 2));
    }

    /** 行情接口失败时仍展示状态，不能把未知价格或交易量显示成0。 */
    @Test
    public void statusQueryFailuresShowUnavailableInsteadOfZero() {
        Robot robot = robot(1);
        robot.setActive(1);
        robot.setName("策略A");
        when(robots.selectByTypename("hotcoin-a")).thenReturn(Collections.singletonList(robot));
        when(args.selectAll(1)).thenReturn(Arrays.asList(arg("market", "btc_usdc"),
                arg("apikey", "test-key"), arg("tpass", "test-secret")));
        when(markets.latestTickers()).thenThrow(new IllegalStateException("test market failure"));
        service.handle("test", "test_bot", update("/status"));
        verify(api).reply(eq("test"), eq("-100123"), eq(7L), argThat(text ->
                text.contains("策略A") && text.contains("最新成交价：未获取")
                        && text.contains("24成交量：未获取")));
    }

    /** 回复失败清除指令冷却，使轮询重试时能够再次回复。 */
    @Test(expected = IllegalStateException.class)
    public void failedReplyAllowsRetry() {
        doThrow(new IllegalStateException("test reply failure"))
                .when(api).reply(anyString(), anyString(), anyLong(), anyString());
        try {
            service.handle("test", "test_bot", update("/help"));
        } finally {
            verify(redis).remove("TELEGRAM_COMMAND_COOLDOWN:-100123");
        }
    }

    /** 创建不带任何真实用户信息的群消息。 */
    private JSONObject update(String command) {
        JSONObject message = new JSONObject();
        message.put("message_id", 7L);
        message.put("text", command);
        JSONObject chat = new JSONObject();
        chat.put("id", -100123L);
        chat.put("type", "supergroup");
        message.put("chat", chat);
        JSONObject update = new JSONObject();
        update.put("message", message);
        return update;
    }

    /** 给开关测试消息设置模拟用户身份，不使用真实Telegram用户数据。 */
    private JSONObject adminUpdate(String command) {
        JSONObject update = update(command);
        JSONObject from = new JSONObject();
        from.put("id", 123L);
        from.put("is_bot", false);
        update.getJSONObject("message").put("from", from);
        return update;
    }

    /** 创建热币交易记录。 */
    private Robot robot(int id) {
        Robot robot = new Robot();
        robot.setRobotId(id);
        robot.setStrategyId(18);
        robot.setTypename("hotcoin-a");
        return robot;
    }

    /** 创建隔离测试中的账户参数。 */
    private RobotArgs arg(String name, String value) {
        RobotArgs arg = new RobotArgs();
        arg.setVariable(name);
        arg.setValue(value);
        return arg;
    }
}
