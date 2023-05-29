package top.suilian.aio.service.bifinance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bifinance.kline.RunbifinanceKline;

@Component
public class BifinanceService {
    @Autowired
    RunbifinanceKline runbifinanceKline;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runbifinanceKline.init(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runbifinanceKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_CANCEL:
                break;

        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runbifinanceKline.killWork(id);
                break;
        }
    }
}
