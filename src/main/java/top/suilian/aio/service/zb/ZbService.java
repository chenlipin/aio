package top.suilian.aio.service.zb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.hotcoin.RandomDepth.RunHotcoinRandomDepth;
import top.suilian.aio.service.hotcoin.depthReferToZg.RunDepthHotcoinReferToZg;
import top.suilian.aio.service.hotcoin.kline.RunHotCoinKline;
import top.suilian.aio.service.hotcoin.klineReferToZg.RunKlineHotcoinReferToZg;
import top.suilian.aio.service.zb.newKline.RunNewZbKline;

@Component
public class ZbService {
    @Autowired
    RunNewZbKline runNewZbKline;
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
                runNewZbKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type) {
        switch (type) {

            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZbKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZbKline.killWork(id);
//                runHotcoinRandomDepth.killWork(id+1);
                break;
        }
    }
}
