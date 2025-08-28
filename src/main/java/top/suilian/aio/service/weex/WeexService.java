package top.suilian.aio.service.weex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.huobi.refToHot.RunHuobiRep2Hot;
import top.suilian.aio.service.weex.newKline.RunNewWeexKline;

@Component
public class WeexService {
    @Autowired
    RunNewWeexKline runNewWeexKline;

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
                runNewWeexKline.init(id);
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
                runNewWeexKline.killWork(id);
//                runBitterRandomDepth.killWork(id+1);
                break;
            case Constant.KEY_STRA_9:
                runHuobiRep2Hot.killWork(id);
                break;
        }
    }
}
