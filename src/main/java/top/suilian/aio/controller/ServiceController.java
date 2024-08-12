package top.suilian.aio.controller;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.suilian.aio.service.hotcoin.HotCoinParentService;
import top.suilian.aio.service.hotcoin.Vedit2;

import java.io.UnsupportedEncodingException;

@CrossOrigin
@Controller
@RequestMapping("/service")
public class ServiceController extends BaseController {
    private static Logger logger = Logger.getLogger(ServiceController.class);

@Autowired
    HotCoinParentService hotCoinParentService;







    @PostMapping(value = "/getBalance")
    @ResponseBody
    public Object setBalance(@RequestBody ApiReq req) {
        JSONObject maps = null;
        try {
            logger.info("getBalance:"+ com.alibaba.fastjson.JSONObject.toJSONString(req));
            maps = hotCoinParentService.setBalanceRedis(req.getApiKey(),req.getTpass());
        } catch (UnsupportedEncodingException e) {
           return null;
        }
        return maps;
    }

    @PostMapping(value = "/getBalanceV1")
    @ResponseBody
    public JSONObject setBalancev2(@RequestBody Vedit2 req) {
        logger.info("getBalance22:"+ com.alibaba.fastjson.JSONObject.toJSONString(req));
        JSONObject maps = null;
        maps = hotCoinParentService.setBalancev2(req);

        return maps;
    }

    @PostMapping(value = "/getBalanceV3")
    @ResponseBody
    public JSONObject setBalancev3(@RequestBody Vedit2 req) {
        logger.info("getBalance33:"+com.alibaba.fastjson.JSONObject.toJSONString(req));
        JSONObject maps = null;
        maps = hotCoinParentService.setBalancev3(req);

        return maps;
    }

    @GetMapping(value = "/redisUpdateTXERYT")
    @ResponseBody
    public JSONObject redisUpdate() {
        logger.info("getBalance33:");
        JSONObject maps = null;
        maps = hotCoinParentService.redisUpdateTXERYT();

        return maps;
    }
}
