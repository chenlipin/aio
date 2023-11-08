package top.suilian.aio.service.bian;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.service.bian.newKline.RunBianKline;

@Component
public class BianService {
    @Autowired
    RunBianKline runBianKline;

    /**
     * 开启机器人
     *
     * @param id
     * @param type
     */
    public void start(Integer id, Integer type) {
        switch (type) {
            case 10:
                runBianKline.init(id);
                break;
        }
    }

    public void stop(Integer id, Integer type){
        switch (type) {
            case 10:
                runBianKline.stopWork(id);
                break;
        }
    }

    public void kill(Integer id, Integer type){
        switch (type) {
            case 10:
                runBianKline.killWork(id);
                break;
        }
    }
}
