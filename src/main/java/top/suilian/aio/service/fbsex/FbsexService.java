package top.suilian.aio.service.fbsex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.fbsex.kline.RunFbsexKline;
import top.suilian.aio.service.fchain.cancel.RunFChainCancel;
import top.suilian.aio.service.fchain.kline.RunFChainKline;
import top.suilian.aio.service.fchain.randomDepth.RunFChainDepth;
@Component
public class FbsexService {
    @Autowired
    RunFbsexKline runFbsexKline;



    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFbsexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFbsexKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runFbsexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }
}
