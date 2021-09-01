package top.suilian.aio.service.zgv1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.service.zgv1.newKline.RunNewZgKline;
import top.suilian.aio.Util.Constant;

/**
 * 20200514
 * zg切换成新接口
 */
@Component
public class ZGService {
    @Autowired
    RunNewZgKline runNewZgKline;


    /**
     * 开启机器人
     */
    public void start(Integer id,Integer type){
        switch(type){
            //新策略
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.init(id);
                break;

            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    /**
     *
     */
    public void stop(Integer id, Integer type){
        switch(type){
            //新策略
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.stopWork(id);
                break;

            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch(type){
            //新策略
            case Constant.KEY_STRATEGY_NEW_KLINE:
                runNewZgKline.killWork(id);
                break;

            case Constant.KEY_STRATEGY_REFERENCE:

                break;
            case Constant.KEY_STRATEGY_DEPTH:

                break;
            case Constant.KEY_STRATEGY_CANCEL:

                break;
            case Constant.KEY_RANDOM_DEPTH:

                break;
        }
    }
}
