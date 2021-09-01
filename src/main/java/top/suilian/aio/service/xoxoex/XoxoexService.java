package top.suilian.aio.service.xoxoex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.xoxoex.kline.RunXoxoexKline;
import top.suilian.aio.service.xoxoex.newKline.RunNewXoxoexKline;
import top.suilian.aio.service.xoxoex.randomDepth.RunXoxoexDepth;

@Component
public class XoxoexService {
    @Autowired
    RunXoxoexKline runXoxoexKline;
    @Autowired
    RunNewXoxoexKline runNewXoxoexKline;
    @Autowired
    RunXoxoexDepth runXoxoexDepth;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runXoxoexDepth.init(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewXoxoexKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runXoxoexDepth.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewXoxoexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runXoxoexKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runXoxoexDepth.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewXoxoexKline.killWork(id);
                break;
        }
    }
}
