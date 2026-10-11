package top.suilian.aio.service;

import org.junit.Before;
import org.junit.Test;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.suilian.aio.dao.RobotMapper;
import top.suilian.aio.dao.SysConfigMapper;
import top.suilian.aio.dao.TelegramChannelMapper;
import top.suilian.aio.model.Robot;
import top.suilian.aio.model.SysConfig;
import top.suilian.aio.model.TelegramChannel;
import top.suilian.aio.redis.RedisHelper;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证typename路由和共享冷却；模拟冷却已占用，避免调用真实Telegram接口。 */
public class TelegramNotifyServiceTest {
    private TelegramNotifyService service;
    private RobotMapper robots;
    private SysConfigMapper configs;
    private TelegramChannelMapper channels;
    private RedisHelper redis;

    /** 注入隔离依赖，不连接数据库、Redis或Telegram。 */
    @Before
    public void setUp() {
        service = new TelegramNotifyService();
        robots = mock(RobotMapper.class);
        configs = mock(SysConfigMapper.class);
        channels = mock(TelegramChannelMapper.class);
        redis = mock(RedisHelper.class);
        ReflectionTestUtils.setField(service, "robotMapper", robots);
        ReflectionTestUtils.setField(service, "sysConfigMapper", configs);
        ReflectionTestUtils.setField(service, "telegramChannelMapper", channels);
        ReflectionTestUtils.setField(service, "redisHelper", redis);
        SysConfig token = new SysConfig();
        token.setConfigValue("test-token");
        when(configs.selectByConfigKey("telegram.bot.token")).thenReturn(token);
        when(channels.selectEnabledByTypenameAndNoticeType(anyString(), anyString()))
                .thenReturn(Collections.singletonList(new TelegramChannel()));
        when(redis.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(false);
    }

    /** 不同记录ID但同typename必须路由到同一绑定，并共享余额通知冷却。 */
    @Test
    public void sameTypenameSharesRouteAndCooldown() {
        bindRobot(1, "hotcoin-a");
        bindRobot(2, "hotcoin-a");
        service.sendHotcoinBalanceNotice(1, "btc_usdc", "USDC", "5");
        service.sendHotcoinBalanceNotice(2, "eth_usdc", "USDC", "5");
        verify(channels, times(2)).selectEnabledByTypenameAndNoticeType("hotcoin-a", "balance_low");
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(redis, times(2)).setIfAbsent(keys.capture(), anyString(), eq(1800L));
        assertEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
        service.clearHotcoinBalanceNoticeCooldown(2, "USDC");
        verify(redis).remove(keys.getAllValues().get(0));
    }

    /** typename、通知类型和业务币种各自独立，不能互相抑制通知。 */
    @Test
    public void differentNoticeDimensionsUseDifferentKeys() {
        bindRobot(1, "hotcoin-a");
        bindRobot(2, "hotcoin-b");
        service.sendRobotNotice(1, "balance_low", "USDC", "test");
        service.sendRobotNotice(2, "balance_low", "USDC", "test");
        service.sendRobotNotice(1, "balance_low", "BTC", "test");
        service.sendRobotNotice(1, "api_error", "USDC", "test");
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(redis, times(4)).setIfAbsent(keys.capture(), anyString(), anyLong());
        List<String> values = keys.getAllValues();
        assertEquals(4, new java.util.HashSet<>(values).size());
    }

    /** 缺失typename时不能使用记录ID兜底路由，避免发送到错误群。 */
    @Test
    public void missingTypenameSkipsRouting() {
        bindRobot(1, " ");
        service.sendHotcoinBalanceNotice(1, "btc_usdc", "USDC", "5");
        verify(channels, never()).selectEnabledByTypenameAndNoticeType(anyString(), anyString());
        verify(redis, never()).setIfAbsent(anyString(), anyString(), anyLong());
    }

    /** 所有接收群关闭主动通知时，不发送也不占用通知冷却。 */
    @Test
    public void disabledNoticeTargetsDoNotConsumeCooldown() {
        bindRobot(1, "hotcoin-a");
        when(channels.selectEnabledByTypenameAndNoticeType("hotcoin-a", "balance_low"))
                .thenReturn(Collections.emptyList());
        service.sendHotcoinBalanceNotice(1, "gnr_usdt", "USDT", "5");
        verify(redis, never()).setIfAbsent(anyString(), anyString(), anyLong());
    }

    /** 查询异常应被隔离，不能中断余额查询和交易流程。 */
    @Test
    public void databaseFailureDoesNotEscapeNotification() {
        when(robots.selectByPrimaryKey(1)).thenThrow(new IllegalStateException("test database failure"));
        service.sendHotcoinBalanceNotice(1, "btc_usdc", "USDC", "5");
        service.clearHotcoinBalanceNoticeCooldown(1, "USDC");
        verify(channels, never()).selectEnabledByTypenameAndNoticeType(anyString(), anyString());
    }

    /** 使用MyBatis真实解析器验证XML、typename参数绑定和发送日志字段。 */
    @Test
    public void mapperStatementsUseTypename() throws Exception {
        Configuration configuration = new Configuration();
        for (String file : new String[]{"RobotMapper.xml", "TelegramChannelMapper.xml", "TelegramSendLogMapper.xml"}) {
            try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/mybatis", file))) {
                new XMLMapperBuilder(input, configuration, file, configuration.getSqlFragments()).parse();
            }
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("typename", "hotcoin-a");
        params.put("noticeType", "balance_low");
        String channelSql = configuration.getMappedStatement(
                "top.suilian.aio.dao.TelegramChannelMapper.selectEnabledByTypenameAndNoticeType")
                .getBoundSql(params).getSql();
        assertTrue(channelSql.contains("r.typename = ?"));
        assertTrue(channelSql.contains("r.notice_enabled = 1"));
        assertFalse(channelSql.contains("r.robot_id"));
        HashMap<String, Object> groupParams = new HashMap<>();
        groupParams.put("chatId", "-100123");
        String groupSql = configuration.getMappedStatement(
                "top.suilian.aio.dao.TelegramChannelMapper.selectEnabledBindingByChatId")
                .getBoundSql(groupParams).getSql();
        assertTrue(groupSql.contains("c.chat_id = ?"));
        assertTrue(groupSql.contains("r.typename"));
        assertFalse(groupSql.contains("find_in_set"));
        assertFalse(groupSql.contains("r.notice_enabled = 1"));
        // 开关更新保留订阅类型和绑定权限，并严格限定当前群及typename。
        groupParams.put("channelId", 1);
        groupParams.put("typename", "hotcoin-a");
        groupParams.put("noticeEnabled", 0);
        String switchSql = configuration.getMappedStatement(
                "top.suilian.aio.dao.TelegramChannelMapper.updateNoticeEnabled").getBoundSql(groupParams).getSql();
        assertTrue(switchSql.contains("r.notice_enabled = ?"));
        assertTrue(switchSql.contains("r.channel_id = ?"));
        assertTrue(switchSql.contains("r.typename = ?"));
        assertFalse(switchSql.contains("set r.enabled"));
        String robotSql = configuration.getMappedStatement("top.suilian.aio.dao.RobotMapper.selectByTypename")
                .getBoundSql(params).getSql();
        assertTrue(robotSql.contains("typename = ?"));
        assertTrue(robotSql.contains("deleted = 0"));
        assertTrue(configuration.getMappedStatement("top.suilian.aio.dao.RobotMapper.selectByPrimaryKey")
                .getBoundSql(1).getSql().contains("typename"));
        assertTrue(configuration.getMappedStatement("top.suilian.aio.dao.TelegramSendLogMapper.insert")
                .getBoundSql(new top.suilian.aio.model.TelegramSendLog()).getSql().contains("typename"));
    }

    /** 为测试中的实际记录配置逻辑机器人标识。 */
    private void bindRobot(int id, String typename) {
        Robot robot = new Robot();
        robot.setRobotId(id);
        robot.setTypename(typename);
        when(robots.selectByPrimaryKey(id)).thenReturn(robot);
    }
}
