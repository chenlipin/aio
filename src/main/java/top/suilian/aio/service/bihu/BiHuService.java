package top.suilian.aio.service.bihu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bihu.cancel.RunBiHuCacel;
import top.suilian.aio.service.bihu.kline.RunBiHuKline;
import top.suilian.aio.service.bihu.newKline.RunBihuNewKline;
import top.suilian.aio.service.bihu.randomDepth.RunBiHuRandomDepth;

@Component
public class BiHuService {
    @Autowired
    RunBiHuKline runBiHuKline;
    @Autowired
    RunBiHuCacel runBiHuCacel;
    @Autowired
    RunBiHuRandomDepth runBiHuRandomDepth;
    @Autowired
    RunBihuNewKline runBihuNewKline;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiHuKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runBiHuCacel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBiHuRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBihuNewKline.init(id);
                break;

        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiHuKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runBiHuCacel.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBiHuRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBihuNewKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiHuKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runBiHuCacel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBiHuRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBihuNewKline.killWork(id);
                break;
        }
    }
}
