package top.suilian.aio.controller;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.suilian.aio.Util.CommonUtil;
import top.suilian.aio.Util.Constant;
import top.suilian.aio.model.RobotArgs;
import top.suilian.aio.model.request.OperationRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@Controller
@RequestMapping("/service")
public class ServiceController extends BaseController {
    private static Logger logger = Logger.getLogger(ServiceController.class);

    /**
     * 启动机器人
     *
     * @param operationRequest
     */
    @RequestMapping(value = "/start")
    @ResponseBody
    public void serviceStart(@RequestBody OperationRequest operationRequest) {
        logger.info("启动机器人===>" + operationRequest.toString());
        if (CommonUtil.getThreadByName(Constant.KEY_THREAD_KLINE + operationRequest.getId()) == null) {
            if (robotService.setRobotStatus(operationRequest.getId(), Constant.KEY_ROBOT_STATUS_RUN) > 0) {
                //判断交易所
                switch (operationRequest.getCoin()) {
                    case Constant.KEY_EXCHANGE_CEOHK:                //ceohk
                        ceohkService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_FUBT:                //fubt
                        fubtService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_RONANCE:                //ronance
                        ronanceService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BIKI:                //biki
                        bikiService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_IDCM:                //idcm
                        idcmService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BHEX:                //火币

                        break;
                    case Constant.KEY_EXCHANGE_BIHU:                //币虎
                        biHuService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_FIRSTV:              //firstv

                        break;
                    case Constant.KEY_EXCHANGE_FCHAIN:              //fchain
                        fchainService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_S:                   //S
                        sService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_LAEX:                //laex
                        laexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_ZG:                //zg
                        zgService.start(operationRequest.getId(), operationRequest.getType());
                        break;

                    case Constant.KEY_EXCHANGE_TEST:                //test
                        testService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_HKEX:                //hkex
                        hkexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_AUXSTO:                //auxsto
                        auxstoService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_GOKO:                //goko
                        gokoService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_9CCEX:                //9CCEX
                        biLianService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_HOTCOIN:                //hotcoin

                        hotCoinService.start(operationRequest.getId(), operationRequest.getType());
//                        RobotArgs isdeepRobot = robotArgsService.findOne(operationRequest.getId(), "isdeepRobot");
//                        if (isdeepRobot!=null && isdeepRobot.getValue().equals("1")){
//                            hotCoinService.start(operationRequest.getId()+1, 5);
//                        }
                        break;
                    case Constant.KEY_EXCHANGE_WBFEX:                //wbfex
                        wbfexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_LOEX:                //loex
                        loexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_HWANC:                //hwanc
                        hwancService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_GOLDEN:                //golden
                        goldenService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BITAI:                //bitai
                        biTaiService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_HOO:                //hoo
                        hooService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_QB:                //qb
                        qbService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_EUEX:                //euex
                        euexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_XOXOEX:                //xoxoex
                        xoxoexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_PCAS:                //pcas
                        pcasService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_EUEXREFERBIBOX:                //euex
                        euexReferBiboxService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BiSION:                //bision
                        bisionService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_FBSEX:                //fbsex
                        fbsexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_KCOIN:                //kcoin
                        kcoinService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_PICKCOIN:                //pickcoin
                        pickcoinService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BBKX:                //bbkx
                        bbkxService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_DIGIFINEX:                //digifinex
                        digifinexnService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_EG:                //eg
                        egService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_MXC:                //mxc
                        mxcService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_99EX:                //99ex
                        nine9ExService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_ASPROEX:                //asproex
                        asproexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BITVICTORY:                //bitvictory
                        bitvictoryService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_SENBIT:                //senbit
                        senbitService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_HAPPYCOIN:                //happycoin
                        happyCoinService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_EXXVIP:                //exxvip
                        exxvipService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_E9EX:                //e9ex
                        e9exService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_XUEBI:                //xuebi
                        xuebiService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_COINVV:                //coinvv
                        coinvvService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_PLAYCOIN:                //playcoin
                        playCoinService.start(operationRequest.getId(), operationRequest.getType());
                        break;


                    case Constant.KEY_EXCHANGE_BGO:                //BGOEX
                        bgoService.start(operationRequest.getId(), operationRequest.getType());

                    case Constant.KEY_EXCHANGE_BTHEX:                //bthex
                        bthexService.start(operationRequest.getId(), operationRequest.getType());
                    case Constant.KEY_EXCHANGE_GWET:                //playcoin
                        GwetService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BITMART:                 //bitmart
                        bitMartService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_COINSTORE:                 //coinstore
                        coinStoreService.start(operationRequest.getId(), operationRequest.getType());
                        break;

                    case Constant.KEY_EXCHANGE_KUCOIN:                 //kucoin
                        kucoinService.start(operationRequest.getId(), operationRequest.getType());
                        break;

                    case Constant.KEY_EXCHANGE_BITTEREX:             //bitterex
                        bitterexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_ZB:             //zb
                        zbService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_COINNOE:             //coinnoe
                        coinnoeService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_ZBG:             //zbg
                        zbgService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BKEX:             //bkex
                        bkexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_bithumb:             //bithhub
                        bithumbService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                        //
                    case Constant.KEY_EXCHANGE_SKIESEX:             //skiesex
                        skiesexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BITRUE:             //basic
                        bitureService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BIBOX:             //bibox
                        biboxService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_Citex:             //citex
                        citexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_LBANK:             //lbank
                        lbankService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_WHITEBIT:             //whitebit
                        whitebitService.start(operationRequest.getId(), operationRequest.getType());
                        break;

                    case Constant.KEY_EXCHANGE_IEX:             //iex
                        iexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_GATE:             //gate
                        gateService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BIFINANCE:             //bifinance
                        bifinanceService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_COINW:             //coinw
                        coinwService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BIAN:
                        bianService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_FELTPEX:
                        feltpexService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_XT:
                        xtService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                    case Constant.KEY_EXCHANGE_BIKA:
                        bikaService.start(operationRequest.getId(), operationRequest.getType());
                        break;
                }
                if (operationRequest.getCategory() == 1) {
                    insertRobotLog(operationRequest.getId(), "重启机器人", Constant.KEY_STATUS_RESTART);
                }
            }
        }
    }


    /**
     * 停止机器人
     *
     * @param operationRequest
     */
    @RequestMapping(value = "/stop")
    @ResponseBody
    public void serviceStop(@RequestBody OperationRequest operationRequest) {
        logger.info("停止机器人===>" + operationRequest.toString());
        if (commonUtil.getThreadByName(Constant.KEY_THREAD_KLINE + operationRequest.getId()) != null) {
            //判断交易所
            switch (operationRequest.getCoin()) {
                case Constant.KEY_EXCHANGE_CEOHK:                //ceohk
                    ceohkService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FUBT:                //fubt
                    fubtService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_RONANCE:                //ronance
                    ronanceService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIKI:                //biki
                    bikiService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_IDCM:                //idcm
                    idcmService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BHEX:                //火币

                    break;
                case Constant.KEY_EXCHANGE_BIHU:                //币虎
                    biHuService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FIRSTV:              //firstv

                    break;
                case Constant.KEY_EXCHANGE_FCHAIN:              //fchain
                    fchainService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_S:                   //S
                    sService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LAEX:                //laex
                    laexService.stop(operationRequest.getId(), operationRequest.getType());

                    break;
                case Constant.KEY_EXCHANGE_ZG:                //laex

                    break;
                case Constant.KEY_EXCHANGE_TEST:                //test
                    testService.stop(operationRequest.getId(), operationRequest.getType());

                    break;
                case Constant.KEY_EXCHANGE_HKEX:                //hkex
                    hkexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_AUXSTO:                //auxsto
                    auxstoService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GOKO:                //goko
                    gokoService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_9CCEX:                //9CCEX
                    biLianService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HOTCOIN:                //hotcoin
                    hotCoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_WBFEX:                //wbfex
                    wbfexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LOEX:                //loex
                    loexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HWANC:                //hwanc
                    hwancService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GOLDEN:                //golden
                    goldenService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITAI:                //bitai
                    biTaiService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HOO:                //hoo
                    hooService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_QB:                //qb
                    qbService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EUEX:                //qb
                    euexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PCAS:                //pcas
                    pcasService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EUEXREFERBIBOX:                //euex
                    euexReferBiboxService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_XOXOEX:                //xoxoex
                    xoxoexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BiSION:                //bision
                    bisionService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FBSEX:                //xoxoex
                    fbsexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_KCOIN:                //kcoin
                    kcoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PICKCOIN:                //pickcoin
                    pickcoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BBKX:                //bbkx
                    bbkxService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_DIGIFINEX:                //digifinex
                    digifinexnService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EG:                //eg
                    egService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_MXC:                //mxc
                    mxcService.stop(operationRequest.getId(), operationRequest.getType());
                    break;

                case Constant.KEY_EXCHANGE_99EX:                //99ex
                    nine9ExService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ASPROEX:                //asproex
                    asproexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITVICTORY:                //bitvictory
                    bitvictoryService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_SENBIT:                //senbit
                    senbitService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HAPPYCOIN:                //happycoin
                    happyCoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EXXVIP:                //exxvip
                    exxvipService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_E9EX:                //e9ex
                    e9exService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PLAYCOIN:                //playcoin
                    playCoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BGO:                //BGOEX
                    bgoService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BTHEX:                //bthex
                    bthexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITMART:                //bitmart
                    bitMartService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINSTORE:                //cpoinstore
                    coinStoreService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_KUCOIN:                 //kucoin
                    kucoinService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITTEREX:                 //bitterex
                    bitterexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ZB:             //zb
                    zbService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINNOE:             //coinnoe
                    coinnoeService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ZBG:             //zbg
                    zbgService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BKEX:             //bkex
                    bkexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_bithumb:             //bithhub
                    bithumbService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_SKIESEX:             //skiesex
                    skiesexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BASIC:             //basic
                    basicService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITRUE:             //basic
                    bitureService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIBOX:             //bibox
                    biboxService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_Citex:             //citex
                    citexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LBANK:             //lbank
                    lbankService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_WHITEBIT:             //whitebit
                    whitebitService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GATE:             //gate
                    gateService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIFINANCE:             //gate
                    bifinanceService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINW:             //coinw
                    coinwService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_OK:             //ok
                    okService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIAN:
                    bianService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FELTPEX:
                    feltpexService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_XT:
                    xtService.stop(operationRequest.getId(), operationRequest.getType());
                    break;

                case Constant.KEY_EXCHANGE_BIKA:
                    bikaService.stop(operationRequest.getId(), operationRequest.getType());
                    break;
            }
        } else {
            robotService.stopRobot(operationRequest.getId());
        }
        insertRobotLog(operationRequest.getId(), "停止机器人", Constant.KEY_STATUS_STOP);
    }

    /**
     * 强杀机器人
     *
     * @param operationRequest
     */
    @RequestMapping(value = "/kill")
    @ResponseBody
    public void serviceKill(@RequestBody OperationRequest operationRequest) {
        logger.info("强杀机器人===>" + operationRequest.toString());
        if (CommonUtil.getThreadByName(Constant.KEY_THREAD_KLINE + operationRequest.getId()) != null) {
            //判断交易所
            switch (operationRequest.getCoin()) {
                case Constant.KEY_EXCHANGE_CEOHK:                //ceohk
                    ceohkService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FUBT:                //fubt
                    fubtService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_RONANCE:                //ronance
                    ronanceService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIKI:                //biki
                    bikiService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_IDCM:                //idcm
                    idcmService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BHEX:                //火币

                    break;
                case Constant.KEY_EXCHANGE_BIHU:                //币虎
                    biHuService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FIRSTV:              //firstv

                    break;
                case Constant.KEY_EXCHANGE_FCHAIN:              //fchain
                    fchainService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_S:                   //S
                    sService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LAEX:                //laex
                    laexService.kill(operationRequest.getId(), operationRequest.getType());

                    break;
                case Constant.KEY_EXCHANGE_ZG:                //laex
                    zgService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_TEST:                //test
                    testService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HKEX:                //hkex
                    hkexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_AUXSTO:                //auxsto
                    auxstoService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GOKO:                //goko
                    gokoService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_9CCEX:                //9CCEX
                    biLianService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HOTCOIN:                //xoxoex
                    hotCoinService.kill(operationRequest.getId(), operationRequest.getType());
                    if (operationRequest.getType()==7){
                        hotCoinService.kill(operationRequest.getId()+1, 5);
                    }

                    break;
                case Constant.KEY_EXCHANGE_WBFEX:                //wbfex
                    wbfexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LOEX:                //loex
                    loexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HWANC:                //hwanc
                    hwancService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GOLDEN:                //golden
                    goldenService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITAI:                //bitai
                    biTaiService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HOO:                //hoo
                    hooService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_QB:                //qb
                    qbService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EUEX:                //qb
                    euexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PCAS:                //pcas
                    pcasService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EUEXREFERBIBOX:                //euex
                    euexReferBiboxService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_XOXOEX:                //xoxoex
                    xoxoexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BiSION:                //bision
                    bisionService.kill(operationRequest.getId(), operationRequest.getType());
                    break;

                case Constant.KEY_EXCHANGE_FBSEX:                //fbsex
                    fbsexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_KCOIN:                //kcoin
                    kcoinService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PICKCOIN:                //pickcoin
                    pickcoinService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BBKX:                //bbkx
                    bbkxService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_DIGIFINEX:                //digifinex
                    digifinexnService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EG:                //digifinex
                    egService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_MXC:                //mxc
                    mxcService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_99EX:                //99ex
                    nine9ExService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ASPROEX:                //asproex
                    asproexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITVICTORY:                //bitvictory
                    bitvictoryService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_SENBIT:                //senbit
                    senbitService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_HAPPYCOIN:                //happycoin
                    happyCoinService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_EXXVIP:                //exxvip
                    exxvipService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_E9EX:                //e9ex
                    e9exService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINVV:                //coinvv
                    coinvvService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_PLAYCOIN:                //playcoin
                    playCoinService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BGO:                //BGOEX
                    bgoService.kill(operationRequest.getId(), operationRequest.getType());
                case Constant.KEY_EXCHANGE_BTHEX:                //bthex
                    bthexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITMART:                //bitmart
                    bitMartService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINSTORE:                //bitmart
                    coinStoreService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_KUCOIN:                 //kucoin
                    kucoinService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITTEREX:                 //bitterex
                    bitterexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ZB:             //zb
                    zbService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINNOE:             //coinnoe
                    coinnoeService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_ZBG:             //zbg
                    zbgService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BKEX:             //bkex
                    bkexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_bithumb:             //bithhub
                    bithumbService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BASIC:             //basic
                    basicService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BITRUE:             //basic
                    bitureService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIBOX:             //bibox
                    biboxService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_Citex:             //citex
                    citexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_LBANK:             //lbank
                    lbankService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_WHITEBIT:             //whitebit
                    whitebitService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_IEX:             //iex
                    iexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_GATE:             //gate
                    gateService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIFINANCE:             //gate
                    bifinanceService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_COINW:             //coinw
                    coinwService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_OK:             //ok
                    okService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_BIAN:
                    bianService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_FELTPEX:
                    feltpexService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
                case Constant.KEY_EXCHANGE_XT:
                    xtService.kill(operationRequest.getId(), operationRequest.getType());
                    break;

                case Constant.KEY_EXCHANGE_BIKA:
                    bikaService.kill(operationRequest.getId(), operationRequest.getType());
                    break;
            }
        } else {
            robotService.stopRobot(operationRequest.getId());
        }
        if (operationRequest.getCategory() == 1) {
            insertRobotLog(operationRequest.getId(), "停止机器人", Constant.KEY_STATUS_KILL);
        }
    }


    @RequestMapping(value = "/test1")
    @ResponseBody
    public void test() {
        System.out.println(robotService.findById(1).getName());
    }

    @RequestMapping(value = "/test")
    @ResponseBody
    public Map<String, String> test(@RequestBody OperationRequest operationRequest) {
        Thread thread = CommonUtil.getThreadByName(Constant.KEY_THREAD_KLINE + operationRequest.getId());
        Map<String, String> map = new HashMap<String, String>();
        if (thread == null) {
            map.put("status", "203");
            map.put("msg", "not Thread");
        } else {
            map.put("status", "200");
            map.put("msg", thread.getName());
        }
        return map;
    }

}
