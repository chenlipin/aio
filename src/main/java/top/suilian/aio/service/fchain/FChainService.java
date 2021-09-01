package top.suilian.aio.service.fchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bihu.kline.RunBiHuKline;
import top.suilian.aio.service.fchain.cancel.RunFChainCancel;
import top.suilian.aio.service.fchain.kline.RunFChainKline;
import top.suilian.aio.service.fchain.newKline.RunNewFchainKline;
import top.suilian.aio.service.fchain.randomDepth.RunFChainDepth;

@Component
public class FChainService {
    @Autowired
    RunFChainKline runFChainKline;

    @Autowired
    RunFChainDepth runFChainDepth;

    @Autowired
    RunFChainCancel runFChainCancel;

    @Autowired
    RunNewFchainKline runNewFchainKline;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFChainKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runFChainCancel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFChainDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFchainKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFChainKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runFChainCancel.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFChainDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFchainKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFChainKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runFChainCancel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFChainDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFchainKline.killWork(id);
                break;
        }
    }
}
