package top.suilian.aio.service.bihu.cancel;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.CancelOrder;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.bihu.BiHuParentService;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class BiHuCancel extends BiHuParentService {
    public BiHuCancel(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_BIHU_CANCEL, id);
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
        List<CancelOrder> cancelOrderList = cancelOrderService.findByAll(Constant.KEY_EXCHANGE_BIHU, robotId);
        logger.info("撤销订单--->" + JSON.toJSONString(cancelOrderList));
        for (CancelOrder cancelOrder : cancelOrderList) {
            logger.info("------------------------------{" + cancelOrder.getOrderId() + "} 撤单开始------------------------------");
            try {
                //查询订单详情
                String orderDetial = selectOrder(cancelOrder.getOrderId());
                setTradeLog(id, "订单详情【" + cancelOrder.getOrderId() + "】" + orderDetial, 0);
                JSONObject orderDetialRes = judgeRes(orderDetial, "error", "selectOrder");
                Integer status = null;
                String remark = orderDetial;
                if (orderDetialRes.getInt("error") == 0) {
                    JSONObject data = orderDetialRes.getJSONObject("data");
                    if ("EX_ORDER_STATUS_CANCELED".equals(data.get("status"))) {  //订单撤销
                        status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
                    } else if ("EX_ORDER_STATUS_FILLED".equals(data.get("status"))) {  //订单完全成交
                        status = Constant.KEY_CANCEL_ORDER_STATUS_FILLED;
                    } else if ("EX_ORDER_STATUS_PARTIAL_CANCEL".equals(data.get("status"))) {  //部分撤销
                        status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                    } else {
                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String cancelDetail = cancelTrade(cancelOrder.getOrderId());
                        setTradeLog(id, "撤销订单【" + cancelOrder.getOrderId() + "】" + cancelDetail, 0);
                        JSONObject cancelDetailRes = judgeRes(cancelDetail, "error", "cancelTrade");
                        if (cancelDetailRes.getInt("error") == 0) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                            remark = cancelDetail;
                        } else {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
                            remark = cancelDetail;
                        }
                    }
                } else if (orderDetialRes.getString("msg").equals("订单不存在")) {       //订单不存在
                    status = Constant.KEU_CANCEL_ORDER_STATUS_UNKNOWN;
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
        clearLog();
        sleep(Constant.KEY_CANCEL_SLEEP_TIME, Integer.parseInt(exchange.get("isMobileSwitch")));
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }
}
