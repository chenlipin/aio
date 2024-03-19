package top.suilian.aio.service.xt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.xt.replenish.RunXtReplenish;

@Component
public class XtService {
    @Autowired
    RunXtKline runXtKline;
    @Autowired
    RunXtReplenish replenish;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runXtKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.init(id);

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runXtKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.stopWork(id);
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runXtKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REPLENISH:
                replenish.killWork(id);
        }
    }
}
