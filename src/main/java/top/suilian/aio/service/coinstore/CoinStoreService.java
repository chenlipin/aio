package top.suilian.aio.service.coinstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.coinstore.newKline.RunNewCoinStoreKline;

@Component
public class CoinStoreService {

    @Autowired
    RunNewCoinStoreKline runNewCoinStoreKline;


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
            case Constant.KEY_RANDOM_DEPTH:
                runNewCoinStoreKline.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinStoreKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runNewCoinStoreKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinStoreKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runNewCoinStoreKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinStoreKline.killWork(id);
                break;
        }
    }
}
