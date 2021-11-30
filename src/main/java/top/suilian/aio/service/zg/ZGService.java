package top.suilian.aio.service.zg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.eg.depthReferToZg.EgDepthToZg;
import top.suilian.aio.service.zg.cancel.RunZGCancel;
import top.suilian.aio.service.zg.depthReferToZg.ZgDepthToZg;
import top.suilian.aio.service.zg.depthReferToZg.ZgRunDepthReferToZg;
import top.suilian.aio.service.zg.kline.RunZGKline;
import top.suilian.aio.service.zg.newKline.RunNewZgKline;


@Component
public class ZGService {
    @Autowired
    RunZGKline runZGKline;

    @Autowired
    ZgRunDepthReferToZg zgRunDepthReferToZg;

    @Autowired
    RunZGCancel runZGCancel;

    @Autowired
    RunNewZgKline runNewZgKline;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runZGKline.init(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
                runZGCancel.init(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.init(id);
                break;
            case 8:
                zgRunDepthReferToZg.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runZGKline.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
//                runZGCancel.stopWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                zgRunDepthReferToZg.stopWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case Constant.KEY_STRATEGY_KLINE:
                runZGKline.killWork(id);
                break;
            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:
//                runZGCancel.killWork(id);
                break;
            case Constant.KEY_RANDOM_DEPTH:
                zgRunDepthReferToZg.killWork(id);
                break;
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.killWork(id);
                break;
        }
    }
}
