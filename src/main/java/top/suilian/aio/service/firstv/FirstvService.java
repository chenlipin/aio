//package top.suilian.aio.service.firstv;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import top.suilian.aio.Util.Constant;
//import top.suilian.aio.service.fchain.kline.RunFChainKline;
//import top.suilian.aio.service.firstv.kline.RunFirstvKline;
//
//@Component
//public class FirstvService {
//    @Autowired
//    RunFirstvKline runFirstvKline;
//
//
//    /**
//     * 开启机器人
//     *
//     * @param id
//     * @param type
//     */
//    public void start(Integer id, Integer type) {
//        switch (type) {
//            case Constant.KEY_STRATEGY_KLINE:
//                runFirstvKline.init(id);
//                break;
//            case Constant.KEY_STRATEGY_DEPTH:
//
//                break;
//        }
//    }
//
//    public void stop(Integer id, Integer type){
//        switch (type) {
//            case Constant.KEY_STRATEGY_KLINE:
//                runFirstvKline.stopWork(id);
//                break;
//            case Constant.KEY_STRATEGY_DEPTH:
//
//                break;
//        }
//    }
//
//    public void kill(Integer id, Integer type){
//        switch (type) {
//            case Constant.KEY_STRATEGY_KLINE:
//                runFirstvKline.killWork(id);
//                break;
//            case Constant.KEY_STRATEGY_DEPTH:
//
//                break;
//        }
//    }
//}
