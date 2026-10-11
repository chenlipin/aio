package top.suilian.aio.dao;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.TelegramChannel;

import java.util.List;

/**
 * Telegram接收目标查询接口。
 */
public interface TelegramChannelMapper {
    /** 与mmweb创建流程共用稳定行锁，避免群发现和手动绑定同时插入重复chat_id。 */
    Integer lockGroupDiscovery();
    /** 发现群仅保存为停用候选，不创建任何机器人绑定。 */
    int insertDiscoveredGroup(@Param("chatId") String chatId, @Param("name") String name);
    /** 只刷新未绑定候选的名称，保留管理员已配置的绑定名称与启用状态。 */
    int updateDiscoveredGroupName(@Param("chatId") String chatId, @Param("name") String name);
    /**
     * 按typename查询启用的接收目标，同类型的机器人记录共用群绑定。
     */
    List<TelegramChannel> selectEnabledByTypenameAndNoticeType(@Param("typename") String typename, @Param("noticeType") String noticeType);

    /** 按发起指令的chat_id查启用的绑定，不受主动通知类型过滤影响。 */
    TelegramChannel selectEnabledBindingByChatId(@Param("chatId") String chatId);

    /** 只更新当前启用群的指定绑定，不修改绑定启用状态或通知类型。 */
    int updateNoticeEnabled(@Param("channelId") Integer channelId, @Param("typename") String typename,
                            @Param("noticeEnabled") int noticeEnabled);
}
