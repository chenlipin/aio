package top.suilian.aio.service.loex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.loex.RandomDepth.RunLoexRandomDepth;
import top.suilian.aio.service.loex.depthReferToWbfex.RunLoexDepthReferToWbfex;
import top.suilian.aio.service.loex.kline.RunLoexKline;
import top.suilian.aio.service.loex.klineReferToWbfex.RunLoexKlineReferToWbfex;
import top.suilian.aio.service.loex.newKline.RunNewLoexKline;

@Component
public class LoexService {
    @Autowired
    RunLoexKline runLoexKline;
    @Autowired
    RunNewLoexKline runNewLoexKline;
    @Autowired
    RunLoexRandomDepth runLoexRandomDepth;
    @Autowired
    RunLoexKlineReferToWbfex runLoexKlineReferToWbfex;
    @Autowired
    RunLoexDepthReferToWbfex runLoexDepthReferToWbfex;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runLoexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runLoexKlineReferToWbfex.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runLoexDepthReferToWbfex.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runLoexRandomDepth.init(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewLoexKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runLoexKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runLoexKlineReferToWbfex.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runLoexDepthReferToWbfex.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runLoexRandomDepth.stopWork(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewLoexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runLoexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runLoexKlineReferToWbfex.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runLoexDepthReferToWbfex.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runLoexRandomDepth.killWork(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewLoexKline.killWork(id);
                break;
        }
    }
}
