package top.suilian.aio.service.euex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.euex.cancel.RunEuexCacel;
import top.suilian.aio.service.euex.depthReferToZg.RunEuexDepthReferToZg;
import top.suilian.aio.service.euex.kline.RunEuexKline;
import top.suilian.aio.service.euex.klineReferToZg.RunEuexKlineReferToZg;


@Component
public class EuexService {

    @Autowired
    RunEuexKlineReferToZg runEuexKlineReferToZg;
    @Autowired
    RunEuexDepthReferToZg runEuexDepthReferToZg;
    @Autowired
    RunEuexKline runEuexKline;
    @Autowired
    RunEuexCacel runEuexCacel;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runEuexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runEuexKlineReferToZg.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runEuexDepthReferToZg.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runEuexCacel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
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
                runEuexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runEuexKlineReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runEuexDepthReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runEuexCacel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
