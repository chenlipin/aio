package top.suilian.aio.service.coinvv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.coinvv.kline.RunCoinvvKline;
import top.suilian.aio.service.coinvv.newKline.RunNewCoinvvKline;
import top.suilian.aio.service.e9ex.kline.RunE9exKline;

@Component
public class CoinvvService {
    @Autowired
    RunCoinvvKline runCoinvvKline;
    @Autowired
    RunNewCoinvvKline runNewCoinvvKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runCoinvvKline.init(id);
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
                runNewCoinvvKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runCoinvvKline.stopWork(id);
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
                runNewCoinvvKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runCoinvvKline.killWork(id);
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
                runNewCoinvvKline.killWork(id);
                break;
        }
    }
}
