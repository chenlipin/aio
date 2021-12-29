package top.suilian.aio.service.kucoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.kucoin.newKline.RunNewKucoinKline;
import top.suilian.aio.service.nine9ex.kline.RunNine9ExKline;
import top.suilian.aio.service.nine9ex.randomDepth.RunNine9exDepth;

@Component
public class KucoinService {
    @Autowired
    RunNewKucoinKline runNewKucoinKline;
//    @Autowired
//    RunNine9exDepth runNine9exDepth;
//    @Autowired
//    RunNewKucoinKline runNewKucoinKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
//                runPickcoinKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
//                runNine9exDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewKucoinKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
//                runPickcoinKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
//                runNine9exDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewKucoinKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
//                runPickcoinKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
//                runNine9exDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewKucoinKline.killWork(id);
                break;
        }
    }
}
