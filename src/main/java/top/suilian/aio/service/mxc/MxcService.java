package top.suilian.aio.service.mxc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.mxc.depthRefer.RunMexRep2Hot;
import top.suilian.aio.service.mxc.kline.RunMxcKline;
import top.suilian.aio.service.mxc.newKline.RunNewMxcKline;
import top.suilian.aio.service.mxc.randomDepth.RunMxcDeep;
import top.suilian.aio.service.mxc.replenish.RunMxcReplenish;

@Component
public class MxcService {
    @Autowired
    RunMxcKline runMxcKline;
    @Autowired
    RunNewMxcKline runNewMxcKline;
    @Autowired
    RunMxcDeep runMxcDepth;
    @Autowired
    RunMxcReplenish runMxcReplenish;
    @Autowired
    RunMexRep2Hot mxcRefer;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runMxcKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runMxcDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewMxcKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runMxcReplenish.init(id);
                break;
            case 9:
                mxcRefer.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runMxcKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runMxcDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewMxcKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runMxcReplenish.stopWork(id);
                break;
            case 9:
                mxcRefer.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runMxcKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runMxcDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewMxcKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runMxcReplenish.killWork(id);
                break;
            case 9:
                mxcRefer.killWork(id);
                break;
        }
    }
}
