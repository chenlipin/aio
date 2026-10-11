package top.suilian.aio.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Telegram指令专用客户端，设置有限超时，并避免异常中泄漏URL里的Bot Token。 */
@Service
public class TelegramApiClient {
    /** 实时校验发起人是否为本群群主或管理员；请求失败时不授予权限。 */
    public boolean isGroupAdministrator(String token, String chatId, long userId) {
        Map<String, String> params = new HashMap<>();
        params.put("chat_id", chatId);
        params.put("user_id", String.valueOf(userId));
        JSONObject member = call(token, "getChatMember", params, 15000).getJSONObject("result");
        String status = member == null ? null : member.getString("status");
        return "creator".equals(status) || "administrator".equals(status);
    }

    /** 获取Bot用户名，用于判断带@名称的指令是否发给当前Bot。 */
    public String getUsername(String token) {
        return call(token, "getMe", new HashMap<>(), 15000).getJSONObject("result").getString("username");
    }

    /** 检查是否存在Webhook，轮询服务不会自动删除用户已有的Webhook。 */
    public boolean hasWebhook(String token) {
        return !call(token, "getWebhookInfo", new HashMap<>(), 15000)
                .getJSONObject("result").getString("url").isEmpty();
    }

    /** 每次最多接收10条消息，服务端长轮询25秒，客户端读取超时35秒。 */
    public JSONArray getUpdates(String token, long offset) {
        Map<String, String> params = new HashMap<>();
        params.put("offset", String.valueOf(offset));
        params.put("timeout", "25");
        params.put("limit", "10");
        // Bot入群或权限变化可触发群发现，不必等到用户发送群指令。
        params.put("allowed_updates", "[\"message\",\"my_chat_member\"]");
        return call(token, "getUpdates", params, 35000).getJSONArray("result");
    }

    /** 直接回复发起查询的群，不经过主动告警的订阅过滤或通知冷却。 */
    public void reply(String token, String chatId, long messageId, String text) {
        Map<String, String> params = new HashMap<>();
        params.put("chat_id", chatId);
        params.put("text", text);
        JSONObject replyParameters = new JSONObject();
        replyParameters.put("message_id", messageId);
        replyParameters.put("allow_sending_without_reply", true);
        params.put("reply_parameters", replyParameters.toJSONString());
        call(token, "sendMessage", params, 15000);
    }

    /** 请求和响应均用UTF-8；外部错误只向调用者返回状态码，不返回Token或原始URL。 */
    private JSONObject call(String token, String method, Map<String, String> params, int readTimeout) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("https://api.telegram.org/bot" + token + "/" + method);
            request.setConfig(RequestConfig.custom().setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000).setSocketTimeout(readTimeout).build());
            List<BasicNameValuePair> form = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            request.setEntity(new UrlEncodedFormEntity(form, "UTF-8"));
            try (CloseableHttpResponse response = client.execute(request)) {
                JSONObject body = JSONObject.parseObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
                if (body == null || !body.getBooleanValue("ok")) {
                    throw new IllegalStateException("Telegram API错误，code="
                            + (body == null ? response.getStatusLine().getStatusCode() : body.getIntValue("error_code")));
                }
                return body;
            }
        } catch (Exception e) {
            // 不附带原始异常，网络库的异常消息可能包含带Token的请求URL。
            throw new IllegalStateException("Telegram " + method + " 请求失败（" + e.getClass().getSimpleName() + "）");
        }
    }
}
