package top.suilian.aio.service.idcm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bihu.kline.RunBiHuKline;
import top.suilian.aio.service.fchain.kline.RunFChainKline;
import top.suilian.aio.service.idcm.cancel.RunIdcmCancel;
import top.suilian.aio.service.idcm.kline.RunIdcmKline;
import top.suilian.aio.service.idcm.randomDepth.RunIdcmRandomDepth;

@Component
public class IdcmService {
    @Autowired
    RunIdcmKline runIdcmKline;
    @Autowired
    RunIdcmCancel runIdcmCancel;
    @Autowired
    RunIdcmRandomDepth runIdcmRandomDepth;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runIdcmKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runIdcmCancel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runIdcmRandomDepth.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runIdcmKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runIdcmCancel.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runIdcmRandomDepth.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runIdcmKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runIdcmCancel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runIdcmRandomDepth.killWork(id);
                break;
        }
    }
}
