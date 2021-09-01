package top.suilian.aio.service.fubt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bihu.kline.RunBiHuKline;
import top.suilian.aio.service.fchain.kline.RunFChainKline;
import top.suilian.aio.service.fubt.RandomDepth.RunFubtRandomDepth;
import top.suilian.aio.service.fubt.cancel.RunFubtCancel;
import top.suilian.aio.service.fubt.kline.RunFubtKline;
import top.suilian.aio.service.fubt.newKline.RunNewFubtKline;

@Component
public class FubtService {
    @Autowired
    RunFubtKline runFubtKline;
    @Autowired
    RunFubtCancel runFubtCancel;
    @Autowired
    RunNewFubtKline runNewFubtKline;
    @Autowired
    RunFubtRandomDepth runFubtRandomDepth;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFubtKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runFubtCancel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFubtRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFubtKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFubtKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFubtRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFubtKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFubtKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:
                runFubtRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewFubtKline.killWork(id);
                break;
        }
    }
}
