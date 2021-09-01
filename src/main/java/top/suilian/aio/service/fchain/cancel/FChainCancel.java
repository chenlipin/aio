package top.suilian.aio.service.fchain.cancel;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.CancelOrder;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.fchain.FChainParentService;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class FChainCancel extends FChainParentService {

    public FChainCancel(
            CancelExceptionService cancelExceptionService,
            CancelOrderService cancelOrderService,
            ExceptionMessageService exceptionMessageService,
            RobotArgsService robotArgsService,
            RobotLogService robotLogService,
            RobotService robotService,
            TradeLogService tradeLogService,
            HttpUtil httpUtil,
            RedisHelper redisHelper,
            int id
    ) {
        super.cancelExceptionService = cancelExceptionService;
        super.cancelOrderService = cancelOrderService;
        super.exceptionMessageService = exceptionMessageService;
        super.robotArgsService = robotArgsService;
        super.robotLogService = robotLogService;
        super.robotService = robotService;
        super.tradeLogService = tradeLogService;
        super.httpUtil = httpUtil;
        super.redisHelper = redisHelper;
        super.id = id;
        super.logger = getLogger(Constant.KEY_LOG_PATH_FCHAIN_CANCEL, id);
    }

    private boolean start = true;
    private Integer robotId;

    public void init() {
        logger.info("\r\n------------------------------{" + id + "} 开始------------------------------\r\n");
        if (start) {
            setParam();
            robotId = Integer.parseInt(exchange.get("robotId"));
            setParam(robotId);
            start = false;
        }
        List<CancelOrder> cancelOrderList = cancelOrderService.findByAll(Constant.KEY_EXCHANGE_FCHAIN, robotId);
        logger.info("撤销订单--->" + JSON.toJSONString(cancelOrderList));
        for (CancelOrder cancelOrder : cancelOrderList) {
            logger.info("------------------------------{" + cancelOrder.getOrderId() + "} 撤单开始------------------------------");
            try {
                //查询订单详情
                String orderDetial = selectOrder(cancelOrder.getOrderId());
                setTradeLog(robotId, orderDetial, 0);
                JSONObject orderDetialRes = judgeRes(orderDetial, "status", "selectOrder");
                Integer status = null;
                String remark = orderDetial;
                if ("CANCELED".equals(orderDetialRes.getString("status"))) {  //订单撤销
                    status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
                } else if ("FILLED".equals(orderDetialRes.getString("status"))) {  //订单完全成交
                    status = Constant.KEY_CANCEL_ORDER_STATUS_FILLED;
                } else if ("PENDING_CANCEL".equals(orderDetialRes.getString("status"))) {  //部分撤销
                    status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                } else {
                    sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                    String cancelDetail = cancelTrade(cancelOrder.getOrderId());
                    setTradeLog(robotId, cancelDetail, 0);
                    JSONObject cancelDetailRes = judgeRes(cancelDetail, "status", "cancelTrade");
                    if (cancelDetailRes.getString("status").equals("CANCELED")) {
                        status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                        remark = cancelDetail;
                    } else {
                        status = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
                        remark = cancelDetail;
                    }
                }
                if (status != null) {
                    addCancelNum(cancelOrder.getRobotId(), cancelOrder.getOrderId(), status, remark);
                }
            } catch (UnsupportedEncodingException e) {
                exceptionMessage = collectExceptionStackMsg(e);
                setExceptionMessage(id, exceptionMessage, Integer.parseInt(exchange.get("isMobileSwitch")));
            }
            logger.info("------------------------------{" + cancelOrder.getOrderId() + "} 撤单结束------------------------------");
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
        sleep(Constant.KEY_CANCEL_SLEEP_TIME, Integer.parseInt(exchange.get("isMobileSwitch")));
        clearLog();
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }


}
