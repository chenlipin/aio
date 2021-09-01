package top.suilian.aio.service.zg.cancel;

import com.alibaba.fastjson.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.Util.HttpUtil;
import top.suilian.aio.model.CancelOrder;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.service.*;
import top.suilian.aio.service.zg.ZGParentService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

public class ZGCancel extends ZGParentService {

    public ZGCancel(
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
        super.logger = getLogger(Constant.KEY_LOG_PATH_ZG_CANCEL, id);
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
        List<CancelOrder> cancelOrderList = cancelOrderService.findByAll(Constant.KEY_EXCHANGE_ZG, robotId);
        logger.info("撤销订单--->" + JSON.toJSONString(cancelOrderList));
        for (CancelOrder cancelOrder : cancelOrderList) {
            logger.info("------------------------------{" + cancelOrder.getOrderId() + "} 撤单开始------------------------------");
            try {


                String orderId = cancelOrder.getOrderId().split("_")[0];
                BigDecimal orderAmount = new BigDecimal(cancelOrder.getOrderId().split("_")[1]);


                String str = selectOrder(orderId);
                setTradeLog(robotId, str, 0);

                JSONObject jsonObject = judgeRes(str, "code", "selectOrder");
                Integer status = null;
                String remark = str;

                if (jsonObject != null && jsonObject.getInt("code") == 0) {

                    BigDecimal twoAmount = BigDecimal.ZERO;


                    JSONObject jsonArray = jsonObject.getJSONObject("result");

                    String records = jsonArray.getString("records");


                    if (records == null || records.equals("null")) {

                        //订单为未成交 或者 订单被撤销  进行撤单操作


                        sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                        String cancelDetail = cancelTrade(orderId);
                        setTradeLog(robotId, cancelDetail, 0);
                        JSONObject cancelDetailRes = judgeRes(cancelDetail, "code", "cancelTrade");
                        if (cancelDetailRes.getInt("code") == 0) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                            remark = cancelDetail;
                        } else if (cancelDetailRes.getInt("code") == 10) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
                            remark = cancelDetail;
                        } else {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
                            remark = cancelDetail;
                        }

                    } else {
                        JSONArray recordsArray = JSONArray.fromObject(records);

                        for (int i = 0; i < recordsArray.size(); i++) {
                            JSONObject everyOrder = recordsArray.getJSONObject(i);
                            BigDecimal everyOrderAmount = new BigDecimal(everyOrder.getString("amount"));
                            twoAmount = twoAmount.add(everyOrderAmount);
                        }


                        int result = orderAmount.compareTo(twoAmount);


                        if (result == 0) {
                            status = Constant.KEY_CANCEL_ORDER_STATUS_FILLED;
                        } else {
                            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
                            String cancelDetail = cancelTrade(orderId);
                            setTradeLog(robotId, cancelDetail, 0);
                            JSONObject cancelDetailRes = judgeRes(cancelDetail, "code", "cancelTrade");
                            if (cancelDetailRes.getInt("code") == 0) {
                                status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD;
                                remark = cancelDetail;
                            } else if (cancelDetailRes.getInt("code") == 10) {
                                status = Constant.KEY_CANCEL_ORDER_STATUS_CANCELLED;
                                remark = cancelDetail;
                            } else {
                                status = Constant.KEY_CANCEL_ORDER_STATUS_FAILED;
                                remark = cancelDetail;
                            }
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
            sleep(2000, Integer.parseInt(exchange.get("isMobileSwitch")));
        }
        sleep(Constant.KEY_CANCEL_SLEEP_TIME, Integer.parseInt(exchange.get("isMobileSwitch")));
        clearLog();
        logger.info("\r\n------------------------------{" + id + "} 结束------------------------------\r\n");
    }


}
