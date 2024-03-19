package top.suilian.aio.service.bika;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bika.newKline.RunNewBikaKline;
import top.suilian.aio.service.bika.replenish.RunBikaReplenish;

@Component
public class BikaService {
    @Autowired
    RunNewBikaKline runNewBitterexKline;
//    @Autowired
//    RunBitterRandomDepth runBitterRandomDepth;
    @Autowired
    RunBikaReplenish replenish;

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
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.init(id);
//                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitterexKline.init(id);
                break;

            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.stopWork(id);
//                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitterexKline.stopWork(id);
                break;

            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_CANCEL:
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.killWork(id);
//                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitterexKline.killWork(id);
//                runBitterRandomDepth.killWork(id+1);
                break;

            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.killWork(id);
                break;
        }
    }
}
