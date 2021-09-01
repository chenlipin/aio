package top.suilian.aio.service.playcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.playcoin.kline.RunPlayCoinKline;
import top.suilian.aio.service.playcoin.newKline.RunNewPlayCoinKline;

@Component
public class PlayCoinService {
    @Autowired
    RunPlayCoinKline runXoxoexKline;
    @Autowired
    RunNewPlayCoinKline runNewPlayCoinKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.init(id);
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
                runNewPlayCoinKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.stopWork(id);
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
                runNewPlayCoinKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.killWork(id);
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
                runNewPlayCoinKline.killWork(id);
                break;
        }
    }
}
