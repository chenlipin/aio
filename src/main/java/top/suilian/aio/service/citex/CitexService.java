package top.suilian.aio.service.citex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.citex.kline.RunCitexKline;
import top.suilian.aio.service.hoo.RandomDepth.RunHooRandomDepth;

@Component
public class CitexService {
    @Autowired
    RunCitexKline runCitexKline;




    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runCitexKline.init(id);
                break;

        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runCitexKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runCitexKline.killWork(id);
                break;
        }
    }
}
