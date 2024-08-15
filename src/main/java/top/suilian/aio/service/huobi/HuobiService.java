package top.suilian.aio.service.huobi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.huobi.newKline.RunNewHuobiKline;
import top.suilian.aio.service.huobi.refToHot.RunHuobiRep2Hot;

@Component
public class HuobiService {
    @Autowired
    RunNewHuobiKline runNewHuobiKline;
    //    @Autowired
//    RunBitterRandomDepth runBitterRandomDepth;
    @Autowired
    RunHuobiRep2Hot runHuobiRep2Hot;


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
                runNewHuobiKline.init(id);
                break;
            case Constant.KEY_STRA_9:
                runHuobiRep2Hot.init(id);
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
                runHuobiRep2Hot.stopWork(id);
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
                runNewHuobiKline.killWork(id);
//                runBitterRandomDepth.killWork(id+1);
                break;
            case Constant.KEY_STRA_9:
                runHuobiRep2Hot.killWork(id);
                break;
        }
    }
}
