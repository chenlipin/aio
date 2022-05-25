package top.suilian.aio.service.bibox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bibox.kline.RunBiboxKline;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;

@Component
public class BiboxService {
    @Autowired
    RunBiboxKline runBiboxKline;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBiboxKline.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBiboxKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runBiboxKline.killWork(id);
                break;

        }
    }
}
