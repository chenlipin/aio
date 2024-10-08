package top.suilian.aio.service.coinw;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.coinw.newKline.RuncoinwKline;
import top.suilian.aio.service.coinw.randomDepet.RunCoinWDeep;
import top.suilian.aio.service.coinw.replenish.RuncoinwReplenish;

@Component
public class CoinwService {
    @Autowired
    RuncoinwKline runcoinwKline;
    @Autowired
    RunCoinWDeep runCoinWDeep;

    @Autowired
    RuncoinwReplenish runcoinwReplenish;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runcoinwKline.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runCoinWDeep.init(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runcoinwReplenish.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runcoinwKline.stopWork(id);
                break;

            case Constant.KEY_RANDOM_DEPTH:
                runCoinWDeep.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runcoinwReplenish.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runcoinwKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                runcoinwReplenish.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runCoinWDeep.killWork(id);
                break;
        }
    }
}
