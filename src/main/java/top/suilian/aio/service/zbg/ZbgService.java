package top.suilian.aio.service.zbg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.zbg.newKline.RunNewZbgKline;

@Component
public class ZbgService {
    @Autowired
    RunNewZbgKline runNewZbgKline;
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
                runNewZbgKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZbgKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZbgKline.killWork(id);
//                runHotcoinRandomDepth.killWork(id+1);
                break;
        }
    }
}
