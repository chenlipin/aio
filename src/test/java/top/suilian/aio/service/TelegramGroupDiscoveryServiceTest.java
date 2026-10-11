package top.suilian.aio.service;

import com.alibaba.fastjson.JSONObject;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import top.suilian.aio.dao.TelegramChannelMapper;
import java.util.UUID;
import static org.junit.Assert.*;

/** 使用真实MyBatis和独立内存数据库，禁止连接Telegram或生产数据库。 */
public class TelegramGroupDiscoveryServiceTest {
    private JdbcTemplate db;
    private TelegramGroupDiscoveryService service;

    /** 简化现有群表及共用锁记录，事务代理与线上调用方式一致。 */
    @Before
    public void setUp() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        db = new JdbcTemplate(source);
        db.execute("create table member(member_id int primary key)");
        db.update("insert into member values(1)");
        db.execute("create table telegram_channel(id int auto_increment primary key, name varchar(100), chat_id varchar(45), enabled int, created_at timestamp, updated_at timestamp)");
        db.execute("create table robot_telegram_channel(channel_id int, typename varchar(100))");
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setMapperLocations(new org.springframework.core.io.Resource[]{new FileSystemResource("src/main/resources/mybatis/TelegramChannelMapper.xml")});
        TelegramGroupDiscoveryService target = new TelegramGroupDiscoveryService();
        ReflectionTestUtils.setField(target, "channelMapper", new SqlSessionTemplate(factory.getObject()).getMapper(TelegramChannelMapper.class));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        service = (TelegramGroupDiscoveryService) proxy.getProxy();
    }

    /** 重复消息不重复建群，改名只更新未绑定候选，始终不启用群。 */
    @Test
    public void discoversUnboundGroupsAndUpdatesNamesWithoutGrantingAccess() {
        service.observe(update("supergroup", "-100123", "热币群"));
        service.observe(update("supergroup", "-100123", "新名称"));
        assertEquals(1, db.queryForObject("select count(*) from telegram_channel", Integer.class).intValue());
        assertEquals("新名称", db.queryForObject("select name from telegram_channel", String.class));
        assertEquals(0, db.queryForObject("select enabled from telegram_channel", Integer.class).intValue());
        assertEquals(0, db.queryForObject("select count(*) from robot_telegram_channel", Integer.class).intValue());
    }

    /** 已绑定群的管理名称、开关和绑定不能被收到的群标题覆盖。 */
    @Test
    public void preservesBoundGroups() {
        db.update("insert into telegram_channel(id,name,chat_id,enabled) values(1,'管理名称','-100123',1)");
        db.update("insert into robot_telegram_channel values(1,'HOTCOIN-MDAO')");
        service.observe(update("group", "-100123", "Telegram新名称"));
        assertEquals("管理名称", db.queryForObject("select name from telegram_channel", String.class));
        assertEquals(1, db.queryForObject("select enabled from telegram_channel", Integer.class).intValue());
        assertEquals("HOTCOIN-MDAO", db.queryForObject("select typename from robot_telegram_channel", String.class));
    }

    /** 只收录真实普通群与超级群，忽略私聊、频道及不完整更新。 */
    @Test
    public void ignoresNonGroupsAndMalformedUpdates() {
        service.observe(update("private", "123", "私聊"));
        service.observe(update("channel", "-100123", "频道"));
        service.observe(update("group", "123", "无效ID"));
        service.observe(update("group", "-100123", " "));
        service.observe(new JSONObject());
        assertEquals(0, db.queryForObject("select count(*) from telegram_channel", Integer.class).intValue());
    }

    /** Bot入群事件无需普通消息即可发现候选，仍不启用或授权。 */
    @Test
    public void discoversGroupWhenBotJoins() {
        JSONObject update = update("supergroup", "-100123", "入群发现");
        JSONObject membership = new JSONObject();
        membership.put("chat", update.getJSONObject("message").getJSONObject("chat"));
        JSONObject member = new JSONObject(); member.put("status", "member");
        membership.put("new_chat_member", member);
        update.remove("message"); update.put("my_chat_member", membership);
        service.observe(update);
        assertEquals("入群发现", db.queryForObject("select name from telegram_channel", String.class));
        assertEquals(0, db.queryForObject("select enabled from telegram_channel", Integer.class).intValue());
    }

    /** 离群、踢出和不完整成员事件不把未知群加入候选。 */
    @Test
    public void ignoresDepartedBotMembershipEvents() {
        for (String status : new String[]{"left", "kicked", "restricted", "unknown"}) {
            JSONObject update = update("group", "-100123", "不可发现");
            JSONObject membership = new JSONObject();
            membership.put("chat", update.getJSONObject("message").getJSONObject("chat"));
            JSONObject member = new JSONObject(); member.put("status", status); member.put("is_member", false);
            membership.put("new_chat_member", member);
            update.remove("message"); update.put("my_chat_member", membership);
            service.observe(update);
        }
        assertEquals(0, db.queryForObject("select count(*) from telegram_channel", Integer.class).intValue());
    }

    /** 超长标题不会超过现有字段长度，避免阻塞后续轮询消息。 */
    @Test
    public void truncatesLongTitles() {
        service.observe(update("group", "-100123", String.join("", java.util.Collections.nCopies(128, "a"))));
        assertEquals(100, db.queryForObject("select name from telegram_channel", String.class).length());
    }

    /** 构造不含真实群和用户信息的更新。 */
    private JSONObject update(String type, String id, String title) {
        JSONObject chat = new JSONObject();
        chat.put("type", type); chat.put("id", id); chat.put("title", title);
        JSONObject message = new JSONObject(); message.put("chat", chat);
        JSONObject update = new JSONObject(); update.put("message", message);
        return update;
    }
}
