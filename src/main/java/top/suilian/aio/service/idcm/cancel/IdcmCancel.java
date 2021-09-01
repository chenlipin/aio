package top.suilian.aio.service.idcm.cancel;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.CancelOrder;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.idcm.IdcmParentService;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class IdcmCancel extends IdcmParentService {
    public IdcmCancel(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_IDCM_CANCEL, id);
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
        List<CancelOrder> cancelOrderList = cancelOrderService.findByAll(Constant.KEY_EXCHANGE_IDCM, robotId);
        logger.info("撤销订单--->" + JSON.toJSONString(cancelOrderList));
        try {
            for (CancelOrder cancelOrder : cancelOrderList) {
                logger.info("------------------------------{" + cancelOrder.getOrderId() + "} 撤单开始------------------------------");
                try {
                    //查询订单详情
                    String orderDetial = selectOrder(cancelOrder.getOrderId());
                    Integer status = null;
                    String remark = orderDetial;
                    JSONObject jsonObject = judgeRes(orderDetial, "result", "selectOrder");
                    if (jsonObject != null && jsonObject.getInt("result") == 1) {
                        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
                        int orderStatus = data.getInt("status");
                        if (orderStatus == 2) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_FILLED;
                            setTradeLog(id, "订单详情[订单已成交]=>" + orderDetial, 0);
                        } else if (orderStatus == -2) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
                            setTradeLog(id, "订单详情[订单已撤销]=>" + orderDetial, 0);
                        } else if (orderStatus == -1) {
                            status = Constant.KEU_CANCEL_ORDER_STATUS_UNKNOWN;
                            setTradeLog(id, "订单详情[订单不存在]=>" + orderDetial, 0);
                        } else {
                            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                            String cancelDetail = cancelTrade(cancelOrder.getOrderId(), data.getInt("side"));
                            JSONObject cancelDetailRes = judgeRes(cancelDetail, "result", "cancelTrade");
                            if (cancelDetailRes.getInt("result") == 1) {
                                status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                                remark = cancelDetail;
                                setTradeLog(id, "撤单成功=>" + cancelDetail, 0, "05cbc8");
                            } else {
                                status = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
                                remark = cancelDetail;
                                setTradeLog(id, "撤单失败=>" + cancelDetail, 0, "ff6224");
                            }
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
                Thread.sleep(2000);
            }
            Thread.sleep(Constant.KEY_CANCEL_SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clearLog();
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }

}
