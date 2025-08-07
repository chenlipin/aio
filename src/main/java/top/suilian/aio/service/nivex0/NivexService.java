package top.suilian.aio.service.nivex0;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.mxc.depthRefer.RunMexRep2Hot;
import top.suilian.aio.service.mxc.kline.RunMxcKline;
import top.suilian.aio.service.mxc.randomDepth.RunMxcDeep;
import top.suilian.aio.service.mxc.replenish.RunMxcReplenish;
import top.suilian.aio.service.nine9ex.randomDepth.RunNine9exDepth;
import top.suilian.aio.service.nivex0.RandomDepth.RunNivexRandomDepth;
import top.suilian.aio.service.nivex0.newKline.RunNewNivexKline;

@Component
public class NivexService {

    @Autowired
    RunNewNivexKline runNewNivexKline;

    @Autowired
    RunNivexRandomDepth randomDepth;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewNivexKline.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                randomDepth.init(id);
                break;

            case 9:
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
                randomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewNivexKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:

                break;
            case 9:

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
                randomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewNivexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                break;
            case 9:

                break;
        }
    }
}
