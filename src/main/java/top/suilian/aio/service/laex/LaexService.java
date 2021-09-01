package top.suilian.aio.service.laex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.laex.depthReferToZg.RunDepthReferToZg;
import top.suilian.aio.service.laex.kline.RunLaexKline;
import top.suilian.aio.service.laex.klineReferToZg.RunKlineReferToZg;

@Component
public class LaexService {
    @Autowired
    RunLaexKline runLaexKline;

    @Autowired
    RunKlineReferToZg runKlineReferToZg;
    @Autowired
    RunDepthReferToZg runDepthReferToZg;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runLaexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToZg.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferToZg.init(id);

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
                runLaexKline.stopWork(id);
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
                runLaexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
