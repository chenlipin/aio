package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.dao.CancelOrderMapper;
import top.suilian.aio.model.CancelOrder;

import java.util.List;

@Service("cancelOrder")
public class CancelOrderService {
    @Autowired
    CancelOrderMapper cancelOrderMapper;

    public CancelOrder findById(Integer id){
        return cancelOrderMapper.selectByPrimaryKey(id);
    }

    public CancelOrder findbyOrderId(Integer robotId, String orderId){
        return cancelOrderMapper.findByOrderId(robotId, orderId);
    }

    public List<CancelOrder> findByAll(Integer exchange, Integer robotId){
        return cancelOrderMapper.findByAll(exchange, robotId, Constant.KEY_CANCEL_ORDER_STATUS_FAILED);
    }

    public int update(CancelOrder cancelOrder){
        return cancelOrderMapper.updateByPrimaryKey(cancelOrder);
    }

    public int insert(CancelOrder cancelOrder){
        return cancelOrderMapper.insert(cancelOrder);
    }
}
