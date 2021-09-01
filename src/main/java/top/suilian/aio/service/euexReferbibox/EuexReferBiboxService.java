package top.suilian.aio.service.euexReferbibox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.euexReferbibox.depthReferToBibox.RunEuexReferDepthToBibox;
import top.suilian.aio.service.euexReferbibox.kline.RunEuexReferBiboxKline;
import top.suilian.aio.service.euexReferbibox.klineReferToBibox.RunEuexKlineReferToBibox;


@Component
public class EuexReferBiboxService {

    @Autowired
    RunEuexKlineReferToBibox runEuexKlineReferToBibox;
    @Autowired
    RunEuexReferDepthToBibox runEuexReferDepthToBibox;
    @Autowired
    RunEuexReferBiboxKline runEuexReferBiboxKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runEuexReferBiboxKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runEuexKlineReferToBibox.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runEuexReferDepthToBibox.init(id);

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
                runEuexReferBiboxKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runEuexKlineReferToBibox.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runEuexReferDepthToBibox.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
