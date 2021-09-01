package top.suilian.aio.service.happycoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bilian.cancel.RunBiLianCacel;
import top.suilian.aio.service.bilian.kline.RunBiLianKline;
import top.suilian.aio.service.happycoin.kline.RunHappyCoinKline;
import top.suilian.aio.service.happycoin.newKline.RunNewHappyCoinKline;
import top.suilian.aio.service.happycoin.randomDepth.RunHappyCoinDepth;

@Component
public class HappyCoinService {


    @Autowired
    RunHappyCoinKline runHappyCoinKline;
    @Autowired
    RunHappyCoinDepth runHappyCoinDepth;
    @Autowired
    RunNewHappyCoinKline runNewHappyCoinKline;



    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHappyCoinKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runHappyCoinKline.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHappyCoinDepth.init(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHappyCoinKline.init(id);
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHappyCoinKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHappyCoinDepth.stopWork(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHappyCoinKline.stopWork(id);
                break;

        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHappyCoinKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHappyCoinDepth.killWork(id);
                break;
            case Constant.KEY_MANUAL_TRADE:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHappyCoinKline.killWork(id);
                break;

        }
    }
}
