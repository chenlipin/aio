package top.suilian.aio.service.bgoex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bgoex.newKline.RunNewBgoKline;
@Component
public class BgoService {
    @Autowired
    RunNewBgoKline runNewBgoKline;

    /**
     *
     */

    public void start(Integer id,Integer type){
        switch (type){
            case Constant.KEY_STRATEGY_KLINE:

                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBgoKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:

                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBgoKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:

                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBgoKline.killWork(id);
                break;
        }
    }
}
