package top.suilian.aio.Util;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import top.suilian.aio.model.request.SmsSendRequest;

import java.math.BigDecimal;

@Component
public class CommonUtil {

    /**
     * 通过线程名称获得线程
     *
     * @param threadName
     * @return
     */
    public static Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 发送短信给我们自己
     *
     * @param msg
     */
    public static void sendSms(String msg) {
        for (String moblie : Constant.KEY_SMS_MOBLIES) {
            SmsSendRequest smsSingleRequest1 = new SmsSendRequest(msg, moblie);
            String requestJson1 = JSON.toJSONString(smsSingleRequest1);
            ChuangLanSmsUtil.sendSmsByPost(requestJson1);
        }
    }
    public static void main(String[] args) {
        BigDecimal divide = new BigDecimal("1.08013").divide(new BigDecimal("0.000039206"),20,BigDecimal.ROUND_HALF_UP);
        BigDecimal divide1 = new BigDecimal("1.08003").divide(divide,9,BigDecimal.ROUND_HALF_UP);
        System.out.println(divide);
    }
}
