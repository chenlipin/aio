package top.suilian.aio.service.bitbaby;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bitbaby.RandomDepth.RunBitbabyRandomDepth;
import top.suilian.aio.service.bitbaby.newKline.RunNewBitbabyKline;
import top.suilian.aio.service.bitbaby.replenish.RunBitbabyReplenish;
import top.suilian.aio.service.weex.refToOk.RunWeexRep2Ok;

@Component
public class BitbabyService {
    @Autowired
    RunNewBitbabyKline runNewBitbabyKline;

    @Autowired
    RunBitbabyReplenish replenish;


    @Autowired
    RunBitbabyRandomDepth randomDepth;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                randomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitbabyKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);
                break;

        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                randomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.stopWork(id);
                break;

        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                randomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitbabyKline.killWork(id);
//                runBitterRandomDepth.killWork(id+1);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.killWork(id);
                break;
        }
    }
}
