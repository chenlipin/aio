package top.suilian.aio.service.digifinex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.digifinex.kline.RunDigifinexKline;
import top.suilian.aio.service.digifinex.newKline.RunNewDigfinexKline;

@Component
public class DigifinexService {
    @Autowired
    RunNewDigfinexKline runNewDigfinexKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewDigfinexKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewDigfinexKline.stopWork(id);
                break;

        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewDigfinexKline.killWork(id);
                break;

        }
    }
}
