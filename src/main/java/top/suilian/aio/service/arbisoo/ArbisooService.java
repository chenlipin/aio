package top.suilian.aio.service.arbisoo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.arbisoo.newKline.RunArbisooKline;
import top.suilian.aio.service.coinw.randomDepet.RunCoinWDeep;
import top.suilian.aio.service.coinw.replenish.RuncoinwReplenish;

@Component
public class ArbisooService {
    @Autowired
    RunArbisooKline runArbisooKline;





    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runArbisooKline.init(id);
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
                runArbisooKline.stopWork(id);
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
                runArbisooKline.killWork(id);
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
