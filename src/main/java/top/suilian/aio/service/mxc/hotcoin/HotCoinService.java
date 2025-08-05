package top.suilian.aio.service.mxc.hotcoin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.mxc.hotcoin.RandomDepth.RunHotcoinDeep;
import top.suilian.aio.service.mxc.hotcoin.RandomDepth.RunHotcoinRandomDepth;
import top.suilian.aio.service.mxc.hotcoin.depthReferToZg.RunDepthHotcoinReferToZg;
import top.suilian.aio.service.mxc.hotcoin.kline.RunHotCoinKline;
import top.suilian.aio.service.mxc.hotcoin.klineReferToZg.RunKlineHotcoinReferToZg;
import top.suilian.aio.service.mxc.hotcoin.newKline.RunNewHotcoinKline;
import top.suilian.aio.service.mxc.hotcoin.refToOk.RunhotcoinRep2Ok;
import top.suilian.aio.service.mxc.hotcoin.replenish.RunhotcoinReplenish;

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
    @Autowired
    RunHotcoinDeep runHotcoinDeep;
    @Autowired
    RunhotcoinReplenish replenish;
    @Autowired
    RunhotcoinRep2Ok runhotcoinRep2Ok;

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
                runHotcoinDeep.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHotcoinKline.init(id);

                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);
                break;
            case 9:
                runhotcoinRep2Ok.init(id);
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
                runHotcoinDeep.stopWork(id);
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
                runHotcoinDeep.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewHotcoinKline.killWork(id);
                runHotcoinDeep.killWork(id+1);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.killWork(id);
                break;
            case 9:
                runhotcoinRep2Ok.killWork(id);
                break;
        }
    }
}
