package top.suilian.aio.service.poloniex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bika.deepChange.RunBikaDeep;
import top.suilian.aio.service.bika.replenish.RunBikaReplenish;
import top.suilian.aio.service.poloniex.newKline.RunNewPoloniexKline;
import top.suilian.aio.service.poloniex.refToHot.RunPoloniexRep2Hot;

@Component
public class PoloniexService {
    @Autowired
    RunNewPoloniexKline runNewPoloniexKline;
    //    @Autowired
//    RunBitterRandomDepth runBitterRandomDepth;
    @Autowired
    RunPoloniexRep2Hot runPoloniexRep2Hot;


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
                runNewPoloniexKline.init(id);
                break;
            case Constant.KEY_STRA_9:
                runPoloniexRep2Hot.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.stopWork(id);
//                break;
            case Constant.KEY_STRA_9:
                runPoloniexRep2Hot.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_CANCEL:
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.killWork(id);
//                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewPoloniexKline.killWork(id);
//                runBitterRandomDepth.killWork(id+1);
                break;
            case Constant.KEY_STRA_9:
                runPoloniexRep2Hot.killWork(id);
                break;
        }
    }
}
