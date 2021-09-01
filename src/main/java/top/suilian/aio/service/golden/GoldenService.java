package top.suilian.aio.service.golden;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.golden.cancel.RunGoldenCancel;
import top.suilian.aio.service.golden.kline.RunGoldenKline;

@Component
public class GoldenService {
    @Autowired
    RunGoldenKline runGoldenKline;
    @Autowired
    RunGoldenCancel runGoldenCancel;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runGoldenKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runGoldenCancel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runGoldenKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runGoldenCancel.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runGoldenKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runGoldenCancel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }
}


