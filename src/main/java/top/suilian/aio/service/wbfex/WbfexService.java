package top.suilian.aio.service.wbfex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.wbfex.depthReferToLoex.RunDepthReferToLoex;
import top.suilian.aio.service.wbfex.kline.RunWbfexKline;
import top.suilian.aio.service.wbfex.klineReferToLoex.RunKlineReferToLoex;
import top.suilian.aio.service.wbfex.newKline.RunNewWbfexKline;
import top.suilian.aio.service.wbfex.randomDepth.RunWebfexRandomDepth;

@Component
public class WbfexService {
    @Autowired
    RunWbfexKline runWbfexKline;


    @Autowired
    RunDepthReferToLoex runDepthReferToLoex;

    @Autowired
    RunKlineReferToLoex runKlineReferToLoex;

    @Autowired
    RunNewWbfexKline runNewWbfexKline;

    @Autowired
    RunWebfexRandomDepth runWebfexRandomDepth;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runWbfexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToLoex.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferToLoex.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runWebfexRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWbfexKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runWbfexKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToLoex.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferToLoex.stopWork(id);

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runWebfexRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWbfexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runWbfexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToLoex.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferToLoex.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runWebfexRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWbfexKline.killWork(id);
                break;
        }
    }
}
