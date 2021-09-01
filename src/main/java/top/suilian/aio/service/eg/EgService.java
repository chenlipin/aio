package top.suilian.aio.service.eg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.eg.depthReferToZg.EgRunDepthReferToZg;
import top.suilian.aio.service.eg.kline.RunEgKline;
import top.suilian.aio.service.eg.klineReferToZg.EgRunKlineReferToZg;
import top.suilian.aio.service.eg.newKline.NewEgKline;
import top.suilian.aio.service.eg.newKline.RunNewEgKline;
import top.suilian.aio.service.eg.randomDepth.EgDepth;
import top.suilian.aio.service.eg.randomDepth.RunEgDepth;

@Component
public class EgService {
    @Autowired
    RunEgKline runEgKline;

    @Autowired
    EgRunKlineReferToZg runKlineReferToZg;
    @Autowired
    EgRunDepthReferToZg runDepthReferToZg;
    @Autowired
    RunNewEgKline runNewEgKline;
    @Autowired
    RunEgDepth runEgDepth;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runEgKline.init(id);
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
                runEgDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewEgKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runEgKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runEgDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewEgKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runEgKline.killWork(id);
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
                runEgDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewEgKline.killWork(id);
                break;
        }
    }
}
