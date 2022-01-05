package top.suilian.aio.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.suilian.aio.Util.CommonUtil;
import top.suilian.aio.model.RobotLog;
import top.suilian.aio.redis.RedisHelper;
import top.suilian.aio.redis.RedisStringExecutor;
import top.suilian.aio.service.RobotArgsService;
import top.suilian.aio.service.RobotLogService;
import top.suilian.aio.service.RobotService;
import top.suilian.aio.service.asproex.AsproexService;
import top.suilian.aio.service.auxsto.AuxstoService;
import top.suilian.aio.service.bbkx.BbkxService;
import top.suilian.aio.service.bgoex.BgoService;
import top.suilian.aio.service.bihu.BiHuService;
import top.suilian.aio.service.biki.BikiService;
import top.suilian.aio.service.bilian.BiLianService;
import top.suilian.aio.service.bision.BisionService;
import top.suilian.aio.service.bitai.BiTaiService;
import top.suilian.aio.service.bithumb.BithumbService;
import top.suilian.aio.service.bitmart.BitMartService;
import top.suilian.aio.service.bitterex.BitterexService;
import top.suilian.aio.service.bitvictory.BitvictoryService;
import top.suilian.aio.service.bkex.coinnoe.BkexService;
import top.suilian.aio.service.bthex.BthexService;
import top.suilian.aio.service.ceohk.CeohkService;
import top.suilian.aio.service.coinnoe.CoinnoeService;
import top.suilian.aio.service.coinstore.CoinStoreService;
import top.suilian.aio.service.coinvv.CoinvvService;
import top.suilian.aio.service.digifinex.DigifinexService;
import top.suilian.aio.service.e9ex.E9exService;
import top.suilian.aio.service.eg.EgService;
import top.suilian.aio.service.euex.EuexService;
import top.suilian.aio.service.euexReferbibox.EuexReferBiboxService;
import top.suilian.aio.service.exxvip.ExxvipService;
import top.suilian.aio.service.fbsex.FbsexService;
import top.suilian.aio.service.fchain.FChainService;
import top.suilian.aio.service.fubt.FubtService;
import top.suilian.aio.service.goko.GokoService;
import top.suilian.aio.service.golden.GoldenService;
import top.suilian.aio.service.gwet.GwetService;
import top.suilian.aio.service.happycoin.HappyCoinService;
import top.suilian.aio.service.hkex.HkexService;
import top.suilian.aio.service.hoo.HooService;
import top.suilian.aio.service.hotcoin.HotCoinService;
import top.suilian.aio.service.hwanc.HwancService;
import top.suilian.aio.service.idcm.IdcmService;
import top.suilian.aio.service.kcoin.KcoinService;
import top.suilian.aio.service.kucoin.KucoinParentService;
import top.suilian.aio.service.kucoin.KucoinService;
import top.suilian.aio.service.laex.LaexService;
import top.suilian.aio.service.loex.LoexService;
import top.suilian.aio.service.mxc.MxcService;
import top.suilian.aio.service.nine9ex.Nine9ExService;
import top.suilian.aio.service.pcas.PcasService;
import top.suilian.aio.service.pickcoin.PickcoinService;
import top.suilian.aio.service.playcoin.PlayCoinService;
import top.suilian.aio.service.qb.QbService;
import top.suilian.aio.service.ronance.RonanceService;
import top.suilian.aio.service.s.SService;
import top.suilian.aio.service.senbit.SenbitService;
import top.suilian.aio.service.skiesex.SkiesexService;
import top.suilian.aio.service.test.TestService;
import top.suilian.aio.service.wbfex.WbfexService;
import top.suilian.aio.service.xoxoex.XoxoexService;
import top.suilian.aio.service.xuebi.XuebiService;
import top.suilian.aio.service.zb.ZbService;
import top.suilian.aio.service.zbg.ZbgService;
import top.suilian.aio.service.zg.ZGService;
import top.suilian.aio.service.zg.ZGService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Component
public class BaseController {
    @Resource
    protected HttpServletRequest request;
    @Autowired
    RobotService robotService;
    @Autowired
    RobotArgsService robotArgsService;
    @Autowired
    RedisStringExecutor redisStringExecutor;
    @Autowired
    RedisHelper redisHelper;
    @Autowired
    RobotLogService robotLogService;
    @Autowired
    CommonUtil commonUtil;

    //region    机器人启动服务
    @Autowired
    BiHuService biHuService;
    @Autowired
    FChainService fchainService;
    @Autowired
    BikiService bikiService;
    @Autowired
    CeohkService ceohkService;
    @Autowired
    FubtService fubtService;
    @Autowired
    IdcmService idcmService;
    @Autowired
    RonanceService ronanceService;
    @Autowired
    SService sService;
    @Autowired
    ZGService zgService;
    @Autowired
    TestService testService;
    @Autowired
    HkexService hkexService;
    @Autowired
    AuxstoService auxstoService;

    @Autowired
    GokoService gokoService;
    @Autowired
    BiLianService biLianService;
    @Autowired
    HotCoinService hotCoinService;

    @Autowired
    LaexService laexService;

    @Autowired
    WbfexService wbfexService;

    @Autowired
    LoexService loexService;

    @Autowired
    HwancService hwancService;

    @Autowired
    GoldenService goldenService;

    @Autowired
    BiTaiService biTaiService;
    @Autowired
    HooService hooService;
    @Autowired
    QbService qbService;
    @Autowired
    EuexService euexService;
    @Autowired
    XoxoexService xoxoexService;
    @Autowired
    PcasService pcasService;
    @Autowired
    EuexReferBiboxService euexReferBiboxService;
    @Autowired
    BisionService bisionService;
    @Autowired
    FbsexService fbsexService;
    @Autowired
    KcoinService kcoinService;
    @Autowired
    PickcoinService pickcoinService;
    @Autowired
    BbkxService bbkxService;
    @Autowired
    DigifinexService digifinexnService;
    @Autowired
    EgService egService;
    @Autowired
    MxcService mxcService;
    @Autowired
    Nine9ExService nine9ExService;
    @Autowired
    AsproexService asproexService;
    @Autowired
    BitvictoryService bitvictoryService;
    @Autowired
    SenbitService senbitService;
    @Autowired
    HappyCoinService happyCoinService;
    @Autowired
    ExxvipService exxvipService;
    @Autowired
    E9exService e9exService;
    @Autowired
    XuebiService xuebiService;
    @Autowired
    CoinvvService coinvvService;
    @Autowired
    PlayCoinService playCoinService;


    @Autowired
    BgoService bgoService;

    @Autowired
    BthexService bthexService;

    @Autowired
    BitMartService bitMartService;

    @Autowired
    CoinStoreService coinStoreService;

    @Autowired
    KucoinService kucoinService;
    @Autowired
    BitterexService bitterexService;

    @Autowired
    ZbService zbService;
    @Autowired
    CoinnoeService coinnoeService;
    @Autowired
    ZbgService zbgService;
    @Autowired
    BkexService bkexService;
    @Autowired
    BithumbService bithumbService;
    @Autowired
    SkiesexService skiesexService;
    @Autowired
    GwetService GwetService;

    //endregion

    public int insertRobotLog(Integer robotId, String remark, Integer status) {
        RobotLog robotLog = new RobotLog();
        robotLog.setRobotId(robotId);
        robotLog.setRemark(remark);
        robotLog.setStatus(status);
        robotLog.setActive(1);
        robotLog.setDeleted(0);
        return robotLogService.insert(robotLog);
    }
}
