package top.suilian.aio.service.bitai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bitai.kline.RunBiTaiKline;
import top.suilian.aio.service.bitai.newKline.RunNewBitaiKline;
import top.suilian.aio.service.bitai.randomDepth.RunBitaiRandomDepth;

@Component
public class BiTaiService {
    @Autowired
    RunBiTaiKline runBiTaiKline;
    @Autowired
    RunNewBitaiKline runNewBitaiKline;
    @Autowired
    RunBitaiRandomDepth runBitaiRandomDepth;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiTaiKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBitaiRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitaiKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiTaiKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBitaiRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitaiKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBiTaiKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBitaiRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitaiKline.killWork(id);
                break;
        }
    }
}
