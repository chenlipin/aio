package top.suilian.aio.service.hoo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bitai.kline.RunBiTaiKline;
import top.suilian.aio.service.hoo.depthReferToBitai.RunDepthReferBitai;
import top.suilian.aio.service.hoo.kline.RunHooKline;
import top.suilian.aio.service.hoo.klineReferBitai.RunKlineReferToBitai;

@Component
public class HooService {
    @Autowired
    RunHooKline runHooKline;


    @Autowired
    RunDepthReferBitai runDepthReferBitai;

    @Autowired
    RunKlineReferToBitai runKlineReferToBitai;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runHooKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToBitai.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferBitai.init(id);
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
                runHooKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToBitai.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferBitai.stopWork(id);

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
                runHooKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
                runKlineReferToBitai.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
                runDepthReferBitai.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
