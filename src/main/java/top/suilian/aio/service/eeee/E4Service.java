package top.suilian.aio.service.eeee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.bika.deepChange.RunBikaDeep;
import top.suilian.aio.service.bika.replenish.RunBikaReplenish;
import top.suilian.aio.service.eeee.deepChange.RunEe4Deep;
import top.suilian.aio.service.eeee.newKline.RunNew4EKline;

@Component
public class E4Service {
    @Autowired
    RunNew4EKline runNew4EKline;
    @Autowired
    RunEe4Deep runEe4Deep;


    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_CANCEL:
                break;
//            case Constant.KEY_RANDOM_DEPTH:
//                runBitterRandomDepth.init(id);
//                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNew4EKline.init(id);
            case 9:
                runEe4Deep.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {

            case Constant.KEY_STRATEGY_CANCEL:
                break;

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNew4EKline.stopWork(id);
                break;
            case 9:
                runEe4Deep.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case 9:
                runEe4Deep.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNew4EKline.killWork(id);
                break;

        }
    }
}
