package top.suilian.aio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.CancelOrder;

public interface CancelOrderMapper {
    int deleteByPrimaryKey(Integer cancelOrderId);

    int insert(CancelOrder record);

    CancelOrder selectByPrimaryKey(Integer cancelOrderId);

    CancelOrder findByOrderId(@Param("robotId") Integer robotId, @Param("orderId") String orderId);

    List<CancelOrder> selectAll();

    int updateByPrimaryKey(CancelOrder record);

    List<CancelOrder> findByAll(Integer exchange, Integer robotId, Integer status);

    int updateStatus(Integer robotId, String orderId, Integer status);
}