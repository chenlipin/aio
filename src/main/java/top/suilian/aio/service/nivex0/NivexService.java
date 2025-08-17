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
import top.suilian.aio.service.nivex0.refToHot.RunNivexRep2Hot;
import top.suilian.aio.service.nivex0.replenish.NivexReplenish;
import top.suilian.aio.service.nivex0.replenish.RunNivexReplenish;

@Component
public class NivexService {

    @Autowired
    RunNewNivexKline runNewNivexKline;

    @Autowired
    RunNivexRandomDepth randomDepth;

    @Autowired
    RunNivexReplenish replenish;

    @Autowired
    RunNivexRep2Hot nivexRep2Hot;


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
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);
                break;

            case 9:
                nivexRep2Hot.init(id);
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
                nivexRep2Hot.stopWork(id);
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
                replenish.killWork(id);
                break;
            case 9:
                nivexRep2Hot.killWork(id);
                break;

        }
    }
}
