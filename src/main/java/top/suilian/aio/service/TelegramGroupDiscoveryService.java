package top.suilian.aio.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.suilian.aio.dao.TelegramChannelMapper;

/** 从Bot收到的群消息发现候选群，仅记录基础身份，绝不自动开放查询权限。 */
@Service
public class TelegramGroupDiscoveryService {
    @Autowired
    private TelegramChannelMapper channelMapper;

    /** 私聊和频道不收录；数据库失败不确认更新，轮询按原有机制重试。 */
    @Transactional(rollbackFor = Exception.class)
    public void observe(JSONObject update) {
        JSONObject message = update.getJSONObject("message");
        JSONObject chat = message == null ? null : message.getJSONObject("chat");
        // Bot加入群或取得管理员权限时立即收录；离开、被踢事件不能新增候选。
        if (chat == null) {
            JSONObject membership = update.getJSONObject("my_chat_member");
            JSONObject member = membership == null ? null : membership.getJSONObject("new_chat_member");
            String status = member == null ? null : member.getString("status");
            boolean joined = "member".equals(status) || "administrator".equals(status) || "creator".equals(status)
                    || ("restricted".equals(status) && member.getBooleanValue("is_member"));
            if (!joined) return;
            chat = membership.getJSONObject("chat");
        }
        if (chat == null || !("group".equals(chat.getString("type")) || "supergroup".equals(chat.getString("type")))) return;
        String chatId = chat.getString("id");
        String name = chat.getString("title");
        if (chatId == null || !chatId.matches("-[1-9][0-9]{0,18}") || name == null || name.trim().isEmpty()) return;
        name = name.trim();
        // 现有群配置字段最多100字符，不让超长群名阻塞全部消息接收。
        if (name.length() > 100) name = name.substring(0, Character.isHighSurrogate(name.charAt(99)) ? 99 : 100);
        if (channelMapper.lockGroupDiscovery() == null) throw new IllegalStateException("未找到群发现事务锁记录");
        channelMapper.insertDiscoveredGroup(chatId, name);
        channelMapper.updateDiscoveredGroupName(chatId, name);
    }
}
