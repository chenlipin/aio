package top.suilian.aio.service.hotcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;
import top.suilian.aio.service.hotcoin.depthReferToZg.RunDepthHotcoinReferToZg;
import top.suilian.aio.service.hotcoin.kline.RunHotCoinKline;
import top.suilian.aio.service.hotcoin.klineReferToZg.RunKlineHotcoinReferToZg;
import top.suilian.aio.service.hotcoin.newKline.RunNewHotcoinKline;

@Component
public class HotCoinService {
    @Autowired
    RunHotCoinKline runHotCoinKline;
    @Autowired
    RunKlineHotcoinReferToZg runKlineHotcoinReferToZg;
    @Autowired
    RunDepthHotcoinReferToZg runDepthHotcoinReferToZg;
    @Autowired
    RunNewHotcoinKline runNewHotcoinKline;
    @Autowired
    RunHotcoinRandomDepth runHotcoinRandomDepth;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHotCoinKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineHotcoinReferToZg.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthHotcoinReferToZg.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHotcoinRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHotcoinKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHotCoinKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineHotcoinReferToZg.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthHotcoinReferToZg.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHotcoinRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHotcoinKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHotCoinKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineHotcoinReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthHotcoinReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHotcoinRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHotcoinKline.killWork(id);
                runHotcoinRandomDepth.killWork(id+1);
                break;
        }
    }
}
