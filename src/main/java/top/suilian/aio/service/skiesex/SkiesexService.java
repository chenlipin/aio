package top.suilian.aio.service.skiesex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.skiesex.newKline.RunNewSkiesexKline;

@Component
public class SkiesexService {
    @Autowired
    RunNewSkiesexKline runNewCoinnoeKline;
//    @Autowired
//    RunHotcoinRandomDepth runHotcoinRandomDepth;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinnoeKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinnoeKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewCoinnoeKline.killWork(id);
//                runHotcoinRandomDepth.killWork(id+1);
                break;
        }
    }
}
