package top.suilian.aio.service.iex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.iex.kline.RunIexKline;

@Component
public class IexService {
    @Autowired
    RunIexKline runIexKline;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runIexKline.init(id);
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runIexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runIexKline.killWork(id);
                break;
        }
    }
}
