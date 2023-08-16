package top.suilian.aio.service.gate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.gate.newKline.RunGateKline;
import top.suilian.aio.service.gate.replenish.RunGteReplenish;

@Component
public class GateService {

    @Autowired
    RunGateKline runGateKline;
    @Autowired
    RunGteReplenish replenish;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runGateKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runGateKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runGateKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.killWork(id);
                break;
        }
    }
}
