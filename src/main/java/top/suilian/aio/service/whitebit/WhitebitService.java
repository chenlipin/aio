package top.suilian.aio.service.whitebit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.whitebit.newKline.RunNewWhitebitKline;
import top.suilian.aio.service.whitebit.replenish.RunWhitebitReplenish;

@Component
public class WhitebitService {
    @Autowired
    RunNewWhitebitKline runNewWhitebitKline;
    @Autowired
    RunWhitebitReplenish runWhitebitReplenish;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWhitebitKline.init(id);
                break;
            case 8:
                runWhitebitReplenish.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWhitebitKline.stopWork(id);
                break;
            case 8:
                runWhitebitReplenish.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewWhitebitKline.killWork(id);
//                runHotcoinRandomDepth.killWork(id+1);
                break;
            case 8:
                runWhitebitReplenish.killWork(id);
                break;
        }
    }
}
