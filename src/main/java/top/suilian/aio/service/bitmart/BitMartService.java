package top.suilian.aio.service.bitmart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bitmart.RandomDepth.RunBitMartRandomDepth;

import top.suilian.aio.service.bitmart.depthRefer.RunBitmartRefer;
import top.suilian.aio.service.bitmart.newKline.RunNewBitMartKline;

@Component
public class BitMartService {

    @Autowired
    RunNewBitMartKline runNewBitMartKline;
    @Autowired
    RunBitMartRandomDepth runBitMartRandomDepth;
    @Autowired
    RunBitmartRefer runBitmartRefer;

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
                runBitMartRandomDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitMartKline.init(id);
                break;
            case 8:
                runBitmartRefer.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBitMartRandomDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitMartKline.stopWork(id);
                break;
            case 8:
                runBitmartRefer.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runBitMartRandomDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewBitMartKline.killWork(id);
                break;
            case 8:
                runBitmartRefer.killWork(id);
                break;
        }
    }
}
