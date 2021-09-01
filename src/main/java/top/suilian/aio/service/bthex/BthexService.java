package top.suilian.aio.service.bthex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bthex.newKline.RunNewBthexKline;
import top.suilian.aio.service.loex.RandomDepth.RunLoexRandomDepth;
import top.suilian.aio.service.loex.depthReferToWbfex.RunLoexDepthReferToWbfex;
import top.suilian.aio.service.loex.kline.RunLoexKline;
import top.suilian.aio.service.loex.klineReferToWbfex.RunLoexKlineReferToWbfex;

@Component
public class BthexService {

    @Autowired
    RunNewBthexKline runNewBthexKline;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
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
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
              runNewBthexKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
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
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBthexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
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
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBthexKline.killWork(id);
                break;
        }
    }
}
