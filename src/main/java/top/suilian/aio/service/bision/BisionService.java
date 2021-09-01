package top.suilian.aio.service.bision;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bision.kline.RunBisionKline;

@Component
public class BisionService {
    @Autowired
    RunBisionKline runBisionKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBisionKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
           //     runPcasKlineReferToHuobi.init(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
           //     runPcasDepthReferToHuobi.init(id);

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBisionKline.stopWork(id);
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

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runBisionKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:
          //      runPcasKlineReferToHuobi.killWork(id);
                break;
            case Constant.KEY_STRATEGY_DEPTH:
            //    runPcasDepthReferToHuobi.killWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
        }
    }
}
