package top.suilian.aio.service.gate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.gate.RandomDepth.RunGateDeep;
import top.suilian.aio.service.gate.newKline.RunGateKline;
import top.suilian.aio.service.gate.refToHot.GateRep2Hot;
import top.suilian.aio.service.gate.refToHot.RunGateRep2Hot;
import top.suilian.aio.service.gate.replenish.RunGteReplenish;

@Component
public class GateService {

    @Autowired
    RunGateKline runGateKline;
    @Autowired
    RunGteReplenish replenish;
    @Autowired
    RunGateRep2Hot gateRep2Hot;

    @Autowired
    RunGateDeep runGateDeep;


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
            case Constant.KEY_RANDOM_DEPTH:
                runGateDeep.init(id);
                break;
            case Constant.KEY_STRA_9:
                gateRep2Hot.init(id);
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
            case Constant.KEY_STRA_9:
                gateRep2Hot.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runGateDeep.stopWork(id);
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
            case Constant.KEY_STRA_9:
                gateRep2Hot.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runGateDeep.killWork(id);
                break;
        }
    }
}
