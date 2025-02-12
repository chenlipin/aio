package top.suilian.aio.service.arbisooNew;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.arbisooNew.newKline.RunArbisooNewKline;

@Component
public class ArbisooNewService {
    @Autowired
    RunArbisooNewKline runArbisooNewKline;





    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runArbisooNewKline.init(id);
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runCoinWDeep.init(id);
//                break;
//            case Constant.KEY_STRATEGY_REPLENISH:
//                runcoinwReplenish.init(id);
//                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runArbisooNewKline.stopWork(id);
                break;

//            case Constant.KEY_RANDOM_DEPTH:
//                runCoinWDeep.stopWork(id);
//                break;
//            case Constant.KEY_STRATEGY_REPLENISH:
//                runcoinwReplenish.stopWork(id);
//                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runArbisooNewKline.killWork(id);
                break;
//            case Constant.KEY_STRATEGY_REPLENISH:
//                runcoinwReplenish.killWork(id);
//                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runCoinWDeep.killWork(id);
//                break;
        }
    }
}
