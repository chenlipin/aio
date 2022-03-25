package top.suilian.aio.service.hoo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;
import top.suilian.aio.service.hoo.kline.RunHooKline;

@Component
public class HooService {
    @Autowired
    RunHooKline runHooKline;
    @Autowired
    RunHooRandomDepth runHooRandomDepth;



    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runHooKline.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHooRandomDepth.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runHooKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHooRandomDepth.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runHooKline.killWork(id);
                runHooRandomDepth.killWork(id+1);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
            case Constant.KEY_RANDOM_DEPTH:
                runHooRandomDepth.killWork(id);
                break;
        }
    }
}
