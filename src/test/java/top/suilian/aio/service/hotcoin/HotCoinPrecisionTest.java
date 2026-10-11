package top.suilian.aio.service.hotcoin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import top.suilian.aio.BeanContext;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.service.RobotArgsService;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** 全部使用模拟HTTP和数据库参数，不连接交易所、不下单。 */
public class HotCoinPrecisionTest {
    private ApplicationContext previousContext;
    private HotCoinParentService service;

    /** 仅满足旧服务构造时的Bean查询，外部依赖全部由mock提供。 */
    @Before
    public void setUp() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(BeanContext.class, "applicationContext");
        new BeanContext().setApplicationContext(mock(ApplicationContext.class));
        // 未提供模拟响应时立即失败，绝不能退回静态HTTP方法。
        service = spy(new HotCoinParentService() {
            @Override
            protected String requestHotcoinSymbols() {
                throw new AssertionError("测试不得访问真实热币接口");
            }
        });
        service.exchange.put("market", " BTC_USDC ");
        service.exchange.put("pricePrecision", "8");
        service.exchange.put("amountPrecision", "6");
        service.exchange.put("minOrderAmount", "0.01");
        // 旧运行时别名不能作为数据库回退来源。
        service.exchange.put("minTradeLimit", "999");
    }

    /** 恢复静态上下文，避免影响其他单元测试。 */
    @After
    public void tearDown() {
        new BeanContext().setApplicationContext(previousContext);
    }

    /** 官方有效字段必须覆盖配置，并同步给读取exchange的策略。 */
    @Test
    public void prefersOfficialRulesAndMatchesExactSymbol() {
        doReturn("{\"code\":200,\"data\":[{\"symbol\":\"btc_usdt\",\"pricePrecision\":9},{\"symbol\":\"btc_usdc\",\"pricePrecision\":2,\"amountPrecision\":5,\"minOrderCount\":0}]}").when(service).requestHotcoinSymbols();
        service.setPrecision();
        assertEquals("2", service.precision.get("pricePrecision"));
        assertEquals("5", service.precision.get("amountPrecision"));
        assertEquals("0", service.precision.get("minTradeLimit"));
        assertEquals("2", service.exchange.get("pricePrecision"));
        assertEquals("5", service.exchange.get("amountPrecision"));
        verify(service).requestHotcoinSymbols();
    }

    /** 0代表整数精度，不应误判为缺失；整数形式的小数可规范化。 */
    @Test
    public void zeroPrecisionIsValid() {
        rules("\"pricePrecision\":0,\"amountPrecision\":\"5.0\",\"minOrderCount\":\"0.001\"");
        service.setPrecision();
        assertEquals("0", service.precision.get("pricePrecision"));
        assertEquals("5", service.precision.get("amountPrecision"));
        assertEquals("0.001", service.precision.get("minTradeLimit"));
    }

    /** 部分字段缺失时仅回退缺失字段，不能丢弃有效官方精度。 */
    @Test
    public void missingFieldFallsBackIndependently() {
        rules("\"pricePrecision\":2,\"amountPrecision\":null");
        service.setPrecision();
        assertEquals("2", service.precision.get("pricePrecision"));
        assertEquals("6", service.precision.get("amountPrecision"));
        assertEquals("0.01", service.precision.get("minTradeLimit"));
    }

    /** 无效精度包括负数、非整数、非数字及超出整数范围，均回退配置。 */
    @Test
    public void invalidOfficialValuesFallBack() {
        for (String value : Arrays.asList("-1", "2.5", "\"bad\"", "\"\"", "2147483648", "true")) {
            rules("\"pricePrecision\":" + value + ",\"amountPrecision\":5,\"minOrderCount\":0");
            service.setPrecision();
            assertEquals("8", service.precision.get("pricePrecision"));
            assertEquals("5", service.precision.get("amountPrecision"));
        }
    }

    /** 网络异常、错误状态和未匹配到交易对均使用配置而不猜测值。 */
    @Test
    public void failedOrUnmatchedQueriesUseConfiguration() {
        doReturn("{\"code\":500}", "{\"code\":200,\"data\":[{\"symbol\":\"eth_usdc\"}]}", "invalid-json").when(service).requestHotcoinSymbols();
        for (int i = 0; i < 3; i++) {
            service.setPrecision();
            assertEquals("8", service.precision.get("pricePrecision"));
            assertEquals("6", service.precision.get("amountPrecision"));
        }
        doThrow(new IllegalStateException("mock network failure")).when(service).requestHotcoinSymbols();
        service.setPrecision();
        assertEquals("8", service.precision.get("pricePrecision"));
    }

    /** 官方查询曾成功也不能把旧官方值当作原始配置回退。 */
    @Test
    public void failedRefreshUsesOriginalConfiguration() {
        rules("\"pricePrecision\":2,\"amountPrecision\":5,\"minOrderCount\":0");
        service.setPrecision();
        doReturn("{\"code\":500}").when(service).requestHotcoinSymbols();
        service.setPrecision();
        assertEquals("8", service.precision.get("pricePrecision"));
        assertEquals("6", service.exchange.get("amountPrecision"));
        assertEquals("0.01", service.exchange.get("minTradeLimit"));
    }

    /** 重载参数后回退到新配置，而不是上一次初始化保存的旧配置。 */
    @Test
    public void parameterReloadResetsConfigurationFallback() {
        rules("\"pricePrecision\":2,\"amountPrecision\":5,\"minOrderCount\":0");
        service.setPrecision();
        service.robotArgsService = mock(RobotArgsService.class);
        when(service.robotArgsService.findAll(service.id)).thenReturn(Arrays.asList(arg("pricePrecision", "4"), arg("amountPrecision", "3"), arg("minOrderAmount", "1")));
        service.setParam();
        doReturn("{\"code\":500}").when(service).requestHotcoinSymbols();
        service.setPrecision();
        assertEquals("4", service.precision.get("pricePrecision"));
        assertEquals("3", service.precision.get("amountPrecision"));
        assertEquals("1", service.precision.get("minTradeLimit"));
    }

    /** 官方必须读取minOrderCount而非同名minOrderAmount，回退必须读取数据库minOrderAmount。 */
    @Test
    public void mapsOfficialCountToConfiguredMinOrderAmount() {
        rules("\"pricePrecision\":2,\"amountPrecision\":5,\"minOrderCount\":0.25,\"minOrderAmount\":15");
        service.setPrecision();
        assertEquals("0.25", service.precision.get("minTradeLimit"));
        assertEquals("0.25", service.exchange.get("minTradeLimit"));
        rules("\"pricePrecision\":2,\"amountPrecision\":5,\"minOrderAmount\":15");
        service.setPrecision();
        assertEquals("0.01", service.precision.get("minTradeLimit"));
        assertEquals("0.01", service.exchange.get("minOrderAmount"));
    }

    /** 配置也无效时明确拒绝解析，不能返回null导致后续下单异常。 */
    @Test(expected = IllegalStateException.class)
    public void missingOfficialAndInvalidConfigurationFailsClosed() {
        service.exchange.put("pricePrecision", "bad");
        rules("\"amountPrecision\":5,\"minOrderCount\":0");
        service.setPrecision();
    }

    /** 模拟官方接口记录，参数不包含真实账户信息。 */
    private void rules(String fields) {
        doReturn("{\"code\":200,\"data\":[{\"symbol\":\"btc_usdc\"," + fields + "}]}").when(service).requestHotcoinSymbols();
    }

    /** 构造数据库中的普通参数，用于参数重载测试。 */
    private RobotArgs arg(String name, String value) {
        RobotArgs arg = new RobotArgs();
        arg.setVariable(name);
        arg.setValue(value);
        return arg;
    }
}
