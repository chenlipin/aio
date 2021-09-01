package top.suilian.aio.service.pcas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.pcas.depthReferToHuobi.RunPcasDepthReferToHuobi;
import top.suilian.aio.service.pcas.kline.RunPcasKline;
import top.suilian.aio.service.pcas.klineReferToHuobi.RunPcasKlineReferToHuobi;

@Component
public class PcasService {
    @Autowired
    RunPcasKline runPcasKline;

    @Autowired
    RunPcasKlineReferToHuobi runPcasKlineReferToHuobi;
    @Autowired
    RunPcasDepthReferToHuobi runPcasDepthReferToHuobi;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runPcasKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runPcasKlineReferToHuobi.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runPcasDepthReferToHuobi.init(id);

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runPcasKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runPcasKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runPcasKlineReferToHuobi.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runPcasDepthReferToHuobi.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
