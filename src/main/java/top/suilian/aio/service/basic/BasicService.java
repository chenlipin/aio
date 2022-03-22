package top.suilian.aio.service.basic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.service.basic.depthRefer.BasicRunDepthRefer;
import top.suilian.aio.service.basic.newKline.RunNewBasicKline;
import top.suilian.aio.service.zg.cancel.RunZGCancel;
import top.suilian.aio.service.zg.kline.RunZGKline;
import top.suilian.aio.service.zg.newKline.RunNewZgKline;


@Component
public class BasicService {
    @Autowired
    BasicRunDepthRefer  basicRunDepthRefer;
    @Autowired
    RunNewBasicKline runNewBasicKline;
    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case 9:
                basicRunDepthRefer.init(id);
                break;
            case 8:
                runNewBasicKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case 9:
                basicRunDepthRefer.stopWork(id);
                break;
            case 8:
                runNewBasicKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case 9:
                basicRunDepthRefer.killWork(id);
                break;
            case 8:
                runNewBasicKline.killWork(id);
                break;
        }
    }
}
