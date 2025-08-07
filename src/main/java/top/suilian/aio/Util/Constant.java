package top.suilian.aio.Util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class Constant {
    //region    机器人操作状态
    public static final int KEY_STATUS_UPDATE_ARGS = 0;   //修改策略
    public static final int KEY_STATUS_STOP = 1;          //停止机器人
    public static final int KEY_STATUS_RESTART = 2;       //重启机器人
    public static final int KEY_STATUS_KILL = 3;          //强杀机器人
    //endregion


    //region    机器人状态
    public static final int KEY_ROBOT_STATUS_FREE = 0;      //空闲中
    public static final int KEY_ROBOT_STATUS_RUN = 1;       //运行中
    public static final int KEY_ROBOT_STATUS_STOP = 2;      //停止中
    public static final int KEY_ROBOT_STATUS_OUT = 3;       //已退出
    public static final int KEY_ROBOT_STATUS_STOPPED = 4;   //被停止
    public static final int KEY_ROBOT_STATUS_ERROR = 5;     //策略有错误
    public static final int KEY_ROBOT_STATUS_OVERDUE = 7;   //到期
    //endregion


    //region    交易所


    public static final int KEY_EXCHANGE_CEOHK = 1;         //CEOHK
    public static final int KEY_EXCHANGE_FUBT = 2;          //FUBT
    public static final int KEY_EXCHANGE_RONANCE = 3;       //RONANCE
    public static final int KEY_EXCHANGE_BIKI = 4;          //BIKI
    public static final int KEY_EXCHANGE_IDCM = 5;          //IDCM
    public static final int KEY_EXCHANGE_BHEX = 6;          //BHEX
    public static final int KEY_EXCHANGE_BIHU = 7;          //币虎
    public static final int KEY_EXCHANGE_FIRSTV = 8;        //FIRSTV
    public static final int KEY_EXCHANGE_FCHAIN = 9;        //FCHAIN
    public static final int KEY_EXCHANGE_S = 10;            //S-EXCHANGE
    public static final int KEY_EXCHANGE_LAEX = 11;         //LAEX
    public static final int KEY_EXCHANGE_ZG = 12;         //zg
    public static final int KEY_EXCHANGE_TEST = 13;         //test
    public static final int KEY_EXCHANGE_HKEX = 14;         //hkex
    public static final int KEY_EXCHANGE_AUXSTO = 15;         //auxsto
    public static final int KEY_EXCHANGE_GOKO = 16;         //goko
    public static final int KEY_EXCHANGE_9CCEX = 17;         //9ccex
    public static final int KEY_EXCHANGE_HOTCOIN = 18;         //HOTCOIN
    public static final int KEY_EXCHANGE_WBFEX = 19;         //WBFEX
    public static final int KEY_EXCHANGE_LOEX = 20;         //WBFEX
    public static final int KEY_EXCHANGE_GOLDEN = 21;       //golden
    public static final int KEY_EXCHANGE_HWANC = 22;         //HWANC
    public static final int KEY_EXCHANGE_BITAI = 24;         //bitai
    public static final int KEY_EXCHANGE_HOO = 25;         //hoo
    public static final int KEY_EXCHANGE_QB= 26;         //qb
    public static final int KEY_EXCHANGE_EUEX= 27;         //euex
    public static final int KEY_EXCHANGE_XOXOEX=28;        //xoxex
    public static final int KEY_EXCHANGE_PCAS=29;        //pcas
    public static final int KEY_EXCHANGE_EUEXREFERBIBOX=30;        //euex
    public static final int KEY_EXCHANGE_FBSEX=31;        //fbfex
    public static final int KEY_EXCHANGE_BiSION=32;        //bision
    public static final int KEY_EXCHANGE_KCOIN=33;         //kcoin
    public static final int KEY_EXCHANGE_PICKCOIN=34;         //pickcoin
    public static final int KEY_EXCHANGE_BBKX=35;              //bbkx
    public static final int KEY_EXCHANGE_DIGIFINEX=36;       //digifinex
    public static final int KEY_EXCHANGE_EG=37;       //eg
    public static final int KEY_EXCHANGE_MXC=38;     //mxc
    public static final int KEY_EXCHANGE_99EX=39;     //99ex
    public static final int KEY_EXCHANGE_ASPROEX=40;  //asproex
    public static final int KEY_EXCHANGE_BITVICTORY=41;  //bitvictory
    public static final int KEY_EXCHANGE_SENBIT=42;  //senbit
    public static final int KEY_EXCHANGE_HAPPYCOIN=43;   //happycoin
    public static final int KEY_EXCHANGE_EXXVIP=44;   //exxvip
    public static final int KEY_EXCHANGE_E9EX=45;   //e9ex
    public static final int KEY_EXCHANGE_XUEBI=46;   //xuebi
    public static final int KEY_EXCHANGE_COINVV=47;   //coinvv
    public static final int KEY_EXCHANGE_PLAYCOIN=48;   //playcoin


    public static final int KEY_EXCHANGE_BGO=49; //bgoex

    public static final int KEY_EXCHANGE_BTHEX=50;   //bthex

    public static final int KEY_EXCHANGE_GWET=51;   //GWET

    public static final int KEY_EXCHANGE_BITMART=52;   //BITMART


    public static final int KEY_EXCHANGE_COINSTORE=53;   //COINSTORE

    public static final int KEY_EXCHANGE_KUCOIN=54;   //KUCOIN

    public static final int KEY_EXCHANGE_BITTEREX=55;   //bitterex

    public static final int KEY_EXCHANGE_ZB=56;   //zb

    public static final int KEY_EXCHANGE_COINNOE=57;   //coinnoe

    public static final int KEY_EXCHANGE_ZBG=58;   //zbg

    public static final int KEY_EXCHANGE_BKEX=59;   //bkex

    public static final int KEY_EXCHANGE_bithumb=60;//bithumb

    public static final int KEY_EXCHANGE_SKIESEX=61; //skiesex


    public static final int KEY_EXCHANGE_BASIC=62; //basic


    public static final int KEY_EXCHANGE_BITRUE=63; //bitrue

    public static final int KEY_EXCHANGE_BIBOX=64; //bibox

    public static final int KEY_EXCHANGE_Citex=65; //citex

    public static final int KEY_EXCHANGE_LBANK=66; //LBANK


    public static final int KEY_EXCHANGE_WHITEBIT=67; //whitebit

    public static final int KEY_EXCHANGE_IEX=68; //iex

    public static final int KEY_EXCHANGE_GATE=69; //gate

    public static final int KEY_EXCHANGE_BIFINANCE=70; //bifinance

    public static final int KEY_EXCHANGE_COINW=71; //coinw

    public static final int KEY_EXCHANGE_OK=72; //ok

    public static final int KEY_EXCHANGE_BIAN=73; //bian

    public static final int KEY_EXCHANGE_FELTPEX=74; //FELTPEX

    public static final int KEY_EXCHANGE_XT=75; //XT

    public static final int KEY_EXCHANGE_BIKA=76; //BIKA

    public static final int KEY_EXCHANGE_4E=77; //eeee

    public static final int KEY_EXCHANGE_POLONIEX=78; //poloniex

    public static final int KEY_EXCHANGE_HUOBI=79; //huobi

    public static final int KEY_EXCHANGE_SUPEREX=80; //SUPEREX

    public static final int KEY_EXCHANGE_ARBISOO=81; //arbisoo

    public static final int KEY_EXCHANGE_ARBISOO_NEW=82; //arbisoo_new

    public static final int KEY_EXCHANGE_Nivex_NEW=83; //Nivex

    //endregion




    //region    日志路径

    /**
     * LAEX 日志路径
     */
    public static final String KEY_LOG_PATH_BTHEX_KLINE = "bthex/kline";           //laex Kline 日志路径
    public static final String KEY_LOG_PATH_BTHEX_DEPTH = "bthex/depth";           //laex Depth 日志路径
    public static final String KEY_LOG_PATH_BTHEX_CANCEL = "bthex/cancel";         //laex Cancel 日志路径

    /**
     * golden 日志路径
     */
    public static final String KEY_LOG_PATH_BGO_KLINE = "bgo/kline";           //bgo Kline 日志路径
    public static final String KEY_LOG_PATH_BGO_DEPTH = "bgo/depth";           //bgo Depth 日志路径
    public static final String KEY_LOG_PATH_BGO_CANCEL = "bgo/cancel";         //bgo Cancel 日志路径



    /**
     * golden 日志路径
     */
    public static final String KEY_LOG_PATH_BITAI_KLINE = "bitai/kline";           //bitai Kline 日志路径
    public static final String KEY_LOG_PATH_BITAI_DEPTH = "bitai/depth";           //bitai Depth 日志路径
    public static final String KEY_LOG_PATH_BITAI_CANCEL = "bitai/cancel";         //bitai Cancel 日志路径

    /**
     * golden 日志路径
     */
    public static final String KEY_LOG_PATH_GOLDEN_KLINE = "golden/kline";           //9ccex Kline 日志路径
    public static final String KEY_LOG_PATH_GOLDEN_DEPTH = "golden/depth";           //9ccex Depth 日志路径
    public static final String KEY_LOG_PATH_GOLDEN_CANCEL = "golden/cancel";         //9ccex Cancel 日志路径

    /**
     * 9ccex 日志路径
     */
    public static final String KEY_LOG_PATH_9CCEX_KLINE = "9ccex/kline";           //9ccex Kline 日志路径
    public static final String KEY_LOG_PATH_9CCEX_DEPTH = "9ccex/depth";           //9ccex Depth 日志路径
    public static final String KEY_LOG_PATH_9CCEX_CANCEL = "9ccex/cancel";         //9ccex Cancel 日志路径


    /**
     * 币虎日志路径
     */
    public static final String KEY_LOG_PATH_BIHU_KLINE = "bihu/kline";           //币虎 Kline 日志路径
    public static final String KEY_LOG_PATH_BIHU_DEPTH = "bihu/depth";           //币虎 Depth 日志路径
    public static final String KEY_LOG_PATH_BIHU_CANCEL = "bihu/cancel";         //币虎 Cancel 日志路径
    //endregion


    /**
     * HKEX日志路径
     */
    public static final String KEY_LOG_PATH_HKEX_KLINE = "hkex/kline";           //HKEX Kline 日志路径
    public static final String KEY_LOG_PATH_HKEX_DEPTH = "hkex/depth";           //HKEX Depth 日志路径
    public static final String KEY_LOG_PATH_HKEX_CANCEL = "hkex/cancel";         //HKEX Cancel 日志路径


    /**
     * WBFEX 日志路径
     */
    public static final String KEY_LOG_PATH_WBFEX_KLINE = "wbfex/kline";           //WBFEX Kline 日志路径
    public static final String KEY_LOG_PATH_WBFEX_DEPTH = "wbfex/depth";           //WBFEX Depth 日志路径
    public static final String KEY_LOG_PATH_WBFEX_CANCEL = "wbfex/cancel";         //WBFEX Cancel 日志路径
    public static final String KEY_LOG_PATH_WBFEX_REFER_DEPTH = "wbfex/referDepth";           //WBFEX referDepth 日志路径
    public static final String KEY_LOG_PATH_WBFEX_REFER_KLINE = "wbfex/referKline";           //WBFEX referKline 日志路径


    /**
     * loex 日志路径
     */
    public static final String KEY_LOG_PATH_LOEX_KLINE = "loex/kline";           //loex Kline 日志路径
    public static final String KEY_LOG_PATH_LOEX_DEPTH = "loex/depth";           //loex Depth 日志路径
    public static final String KEY_LOG_PATH_LOEX_CANCEL = "loex/cancel";         //loex Cancel 日志路径
    public static final String KEY_LOG_PATH_LOEX_REFER_KLINE="loex/referKline";
    public static final String KEY_LOG_PATH_LOEX_REFER_DEPTH="loex/referDepth";

    /**
     * LAEX 日志路径
     */
    public static final String KEY_LOG_PATH_LAEX_KLINE = "laex/kline";           //laex Kline 日志路径
    public static final String KEY_LOG_PATH_LAEX_DEPTH = "laex/depth";           //laex Depth 日志路径
    public static final String KEY_LOG_PATH_LAEX_CANCEL = "laex/cancel";         //laex Cancel 日志路径


    /**
     * hotcoin 日志路径
     */
    public static final String KEY_LOG_PATH_HOTCOIN_KLINE = "hotcoin/kline";           //HOTCOIN Kline 日志路径
    public static final String KEY_LOG_PATH_HOTCOIN_REFER_DEPTH = "hotcoin/referDepth";           //HOTCOIN 对标深度日志路径
    public static final String KEY_LOG_PATH_HOTCOIN_REFER_KLINE = "hotcoin/referKline";         //HOTCOIN 对标k线日志路径
    public static final String KEY_LOG_PATH_HOTCOIN_DEPTH="hotcoin/depth";



    public static final String KEY_LOG_PATH_Nivex_DEPTH="nivex/depth";
    public static final String KEY_LOG_PATH_Nivex_kline="nivex/kline";


    public static final String KEY_LOG_PATH_BIAN_KLINE = "bian/kline";

    /**
     * AUXSTO日志路径
     */
    public static final String KEY_LOG_PATH_AUXSTO_KLINE = "auxsto/kline";           //AUXSTO Kline 日志路径
    public static final String KEY_LOG_PATH_AUXSTO_DEPTH = "auxsto/depth";           //AUXSTO Depth 日志路径
    public static final String KEY_LOG_PATH_AUXSTO_CANCEL = "auxsto/cancel";         //AUXSTO Cancel 日志路径


    /**
     * goko日志路径
     */
    public static final String KEY_LOG_PATH_GOKO_KLINE = "goko/kline";           //goko Kline 日志路径
    public static final String KEY_LOG_PATH_GOKO_DEPTH = "goko/depth";           //goko Depth 日志路径
    public static final String KEY_LOG_PATH_GOKO_CANCEL = "goko/cancel";         //goko Cancel 日志路径


    /**
     * 测试日志路径
     */
    public static final String KEY_LOG_PATH_TEST_KLINE = "test/kline";           //币虎 Kline 日志路径
    public static final String KEY_LOG_PATH_TEST_DEPTH = "test/depth";           //币虎 Depth 日志路径
    public static final String KEY_LOG_PATH_TEST_CANCEL = "test/cancel";


    /**
     * IDCM日志路径
     */
    public static final String KEY_LOG_PATH_IDCM_KLINE = "idcm/kline";           //IDCM日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_IDCM_DEPTH = "idcm/depth";           //IDCM日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_IDCM_CANCEL = "idcm/cancel";         //IDCM日志路径 Cancel 日志路径


    /**
     * fchain日志路径
     */
    public static final String KEY_LOG_PATH_FCHAIN_KLINE = "fchain/kline";           //fchain日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_FCHAIN_DEPTH = "fchain/depth";           //fchain日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_FCHAIN_CANCEL = "fchain/cancel";         //fchain日志路径 Cancel 日志路径

    /**
     * hwanc 日志路径
     */
    public static final String KEY_LOG_PATH_HWANC_KLINE = "hwanc/kline";           //hwanc 日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_HWANC_DEPTH = "hwanc/depth";           //hwanc 日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_HWANC_CANCEL = "hwanc/cancel";         //hwanc 日志路径 Cancel 日志路径


    /**
     * S日志路径
     */
    public static final String KEY_LOG_PATH_S_KLINE = "s/kline";           //s日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_S_DEPTH = "s/depth";           //s日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_S_CANCEL = "s/cancel";         //s日志路径 Cancel 日志路径

    /**
     * fubt日志路径
     */
    public static final String KEY_LOG_PATH_FUBT_KLINE = "fubt/kline";           //fubt日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_FUBT_DEPTH = "fubt/depth";           //fubt日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_FUBT_CANCEL = "fubt/cancel";         //fubt日志路径 Cancel 日志路径


    /**
     * ceohk日志路径
     */
    public static final String KEY_LOG_PATH_CEOHK_KLINE = "ceohk/kline";           //ceohk日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_CEOHK_DEPTH = "ceohk/depth";           //ceohk日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_CEOHK_CANCEL = "ceohk/cancel";         //ceohk日志路径 Cancel 日志路径


    /**
     * biki日志路径
     */
    public static final String KEY_LOG_PATH_BIKI_KLINE = "biki/kline";           //biki日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_BIKI_DEPTH = "biki/depth";           //biki日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_BIKI_CANCEL = "biki/cancel";         //biki日志路径 Cancel 日志路径

    /**
     * ronance日志路径
     */
    public static final String KEY_LOG_PATH_RONANCE_KLINE = "ronance/kline";           //ronance日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_RONANCE_DEPTH = "ronance/depth";           //ronance日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_RONANCE_CANCEL = "ronance/cancel";         //ronance日志路径 Cancel 日志路径


    /**
     * zg日志路径
     */
    public static final String KEY_LOG_PATH_ZG_KLINE = "zg/kline";           //zg日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_ZG_DEPTH = "zg/depth";           //zg日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_ZG_CANCEL = "zg/cancel";         //zg日志路径 Cancel 日志路径
    /**
     * hoo日志路径
     */
    public static final String KEY_LOG_PATH_HOO_REFER_KLINE = "hoo/kline";           //ho日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_HOO_REFER_DEPTH = "hoo/depth";           //ho日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_HOO_CANCEL = "hoo/cancel";         //ho日志路径 Cancel 日志路径

    /**
     * qb日志路径
     */
    public static final String KEY_LOG_PATH_QB_REFER_KLINE = "qb/kline";           //qb日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_QB_REFER_DEPTH = "qb/depth";           //qb日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_QB_CANCEL = "qb/cancel";         //qb日志路径 Cancel 日志路径

    /**
     * euex日志路径
     */
    public static final String KEY_LOG_PATH_EUEX_REFER_KLINE = "euex/kline";           //euex日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_EUEX_REFER_DEPTH = "euex/depth";           //euex日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_EUEX_KLINE = "euex/ownkline";           //euex日志路径 自己刷K线 日志路径
    public static final String KEY_LOG_PATH_EUEX_CANCEL = "euex/cancel";         //euex日志路径 Cancel 日志路径


    /**
     * golden 日志路径
     */
    public static final String KEY_LOG_PATH_XOXOEX_KLINE = "xoxoex/kline";           // Kline 日志路径
    public static final String KEY_LOG_PATH_XOXOEX_DEPTH = "xoxoex/depth";           //XOXOEX Depth 日志路径
    public static final String KEY_LOG_PATH_XOXOEX_CANCEL = "xoxoex/cancel";         //XOXOEX Cancel 日志路径
    /**
     * pcas 日志路径
     */
    public static final String KEY_LOG_PATH_PCAS_REFER_KLINE = "pcas/kline";           //pcas日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_PCAS_REFER_DEPTH = "pcas/depth";           //pcas日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_PCAS_KLINE = "pcas/ownkline";           //pcas日志路径 自己刷K线 日志路径
    public static final String KEY_LOG_PATH_PCAS_CANCEL = "pcas/cancel";         //pcas日志路径 Cancel 日志路径

    /**
<
     * euex对标bibox 日志路径
     */
    public static final String KEY_LOG_PATH_EUEXREFERBIBOX_REFER_KLINE = "euexreferbibox/kline";           //euex日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_EUEXREFERBIBOX_REFER_DEPTH = "euexreferbibox/depth";           //euex日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_EUEXREFERBIBOX_KLINE = "euexreferbibox/ownkline";           //euex日志路径 自己刷K线 日志路径
    public static final String KEY_LOG_PATH_EUEXREFERBIBOX_CANCEL = "euexreferbibox/cancel";         //euex日志路径 Cancel 日志路径


    /**
     * bision 日志路径
     */
    public static final String KEY_LOG_PATH_BISION_LINE = "bision/ownkline";           //bision日志路径 自己刷K线 日志路径

    public static final String KEY_LOG_PATH_BKEX_LINE = "bkex/ownkline";           //bision日志路径 自己刷K线 日志路径
    /**
     * fbsex 日志路径
     */
    public static final String KEY_LOG_PATH_FBSEX_REFER_KLINE = "fbsex/kline";           //fbsex日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_FBSEX_REFER_DEPTH = "fbfex/depth";           //fbsex日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_FBSEX_KLINE = "fbsex/ownkline";           //fbsex日志路径 自己刷K线 日志路径
    public static final String KEY_LOG_PATH_FBFEX_CANCEL = "fbfex/cancel";         //fbsex日志路径 Cancel 日志路径

    /**
     * kcoin日志路径
     */
    public static final String KEY_LOG_PATH_KCOIN_KLINE = "kcoin/kline";           //kcoin日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_KCOIN_DEPTH = "kcoin/depth";           //kcoin日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_KCOIN_CANCEL = "kcoin/cancel";         //kcoin日志路径 Cancel 日志路径

    /**
     * kcoin日志路径
     */
    public static final String KEY_LOG_PATH_PICKCOIN_KLINE = "pickcoin/kline";           //pickcoin日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_PICKCOIN_DEPTH = "pickcoin/depth";           //pickcoin日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_PICKCOIN_CANCEL = "pickcoin/cancel";         //pickcoin日志路径 Cancel 日志路径

    /**
     * bbkx日志路径
     */
    public static final String KEY_LOG_PATH_BBKX_KLINE = "bbkx/kline";           //bbkx日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_BBKX_DEPTH = "bbkx/depth";           //bbkx日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_BBKX_CANCEL = "bbkx/cancel";         //bbkx日志路径 Cancel 日志路径


    public static final String KEY_LOG_PATH_DIGIFINEX_KLINE = "digifinex/kline";           //digifinex Kline 日志路径
    public static final String KEY_LOG_PATH_DIGIFINEX_DEPTH = "digifinex/depth";           //digifinex Depth 日志路径
    public static final String KEY_LOG_PATH_DIGIFINEX_CANCEL = "digifinex/cancel";         //digifinex Cancel 日志路径

    /**
     * eg 日志路径
     */
    public static final String KEY_LOG_PATH_EG_REFER_KLINE = "eg/kline";           //eg日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_EG_REFER_DEPTH = "eg/depth";           //eg日志路径 Depth 日志路径
    public static final String KEY_LOG_PATH_EG_KLINE = "eg/ownkline";           //eg日志路径 自己刷K线 日志路径
    public static final String KEY_LOG_PATH_EG_CANCEL = "eg/cancel";         //eg日志路径 Cancel 日志路径
    public static final String KEY_LOG_PATH_EG_DEPTH = "eg/depth";
    /**
     * Mxc日志路径
     */
    public static final String KEY_LOG_PATH_MXC_KLINE = "mxc/kline";           //mxc日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_MXC_DEPTH = "mxc/depth";
    public static final String KEY_LOG_PATH_MXC_REPLENLISGH = "mxc/replenish";


    public static final String KEY_LOG_PATH_GTE_REPLENLISGH = "gate/replenish";
    /**
     * 99ex日志路径
     */
    public static final String KEY_LOG_PATH_99EX_KLINE = "99ex/kline";           //99ex日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_99EX_DEPTH = "99ex/depth";
    /**
     * asproex日志路径
     */
    public static final String KEY_LOG_PATH_ASPROEX_KLINE = "asproex/kline";           //asproex日志路径 Kline 日志路径


    /**
     * bitvictory日志路径
     */
    public static final String KEY_LOG_PATH_BITVICTORY_KLINE = "bitvictory/kline";           //bitvictory日志路径 Kline 日志路径


    /**
     * senbit日志路径
     */
    public static final String KEY_LOG_PATH_SENBIT_KLINE = "senbit/kline";           //senbit日志路径 Kline 日志路径


    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_HAPPYCOIN_KLINE = "happycoin/kline";           //happycoin日志路径 Kline 日志路径
    public static final String KEY_LOG_PATH_HAPPYCOIN_DEPTH="happycoin/depth";
    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_EXXVIP_KLINE = "exxvip/kline";           //exxvip日志路径 Kline 日志路径


    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_E9EX_KLINE = "e9ex/kline";           //e9ex日志路径 Kline 日志路径

    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_XUEBI_KLINE = "xuebi/kline";           //e9ex日志路径 Kline 日志路径


    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_COINVV_KLINE = "coinvv/kline";           //e9ex日志路径 Kline 日志路径


    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_PLAYCOIN_KLINE = "playcoin/kline";           //playcoin日志路径 Kline 日志路径

    /**
     * gwet日志路径
     */
    public static final String KEY_LOG_PATH_GWET_KLINE = "gwet/kline";           //gwet日志路径 Kline 日志路径


    /**
     * happycoin日志路径
     */
    public static final String KEY_LOG_PATH_BTXEX_KLINE = "bthex/kline";           //playcoin日志路径 Kline 日志路径

    public static final String KEY_LOG_PATH_ZBG_KLINE = "zbg/kline";           //zbg日志路径 Kline 日志路径

    public static final String KEY_LOG_PATH_COINW_KLINE = "coinw/kline";

    /**
     * bitmart 日志路径
     */
    public static final String KEY_LOG_PATH_BITMART_KLINE = "bitmart/newkline";           //bitmart Kline 日志路径
    public static final String KEY_LOG_PATH_BITMART_REFER_DEPTH = "bitmart/referDepth";           //bitmart 对标深度日志路径
    public static final String KEY_LOG_PATH_BITMART_REFER_KLINE = "bitmart/referKline";         //bitmart 对标k线日志路径
    public static final String KEY_LOG_PATH_BITMART_DEPTH="bitmart/depth";
    /**
     * coinstore 日志路径
     */
    public static final String KEY_LOG_PATH_CPINSTORE_KLINE = "coinstore/newkline";


    public static final String KEY_LOG_PATH_BIBOX_KLINE = "bibox/newkline";

    public static final String KEY_LOG_PATH_CL_REFER_DEPTH ="basic/refer";

    public static final String KEY_LOG_PATH_bithhub_kilne="bithhub/kline";
    //region    redis
    public static final String KEY_ROBOT_ARG = "KEY_ROBOT_ARG_";             //机器人参数
    public static final String KEY_ROBOT = "KEY_ROBOT_";             //机器人
    public static final String KEY_ROBOT_COINS = "KEY_ROBOT_COINS_";             //机器人交易对
    public static final String KEY_ROBOT_BALANCE = "WEB_ROBOT_BALANCE_";             //机器人余额

    public static final String KEY_ROBOT_CLEAR_LOG = "KEY_ROBOT_CLEAR_LOG_";             //机器人删除日志时间
    //endregion


    //region    策略类型
    public static final int KEY_STRATEGY_KLINE = 1;          //买一卖一中间刷K线
    public static final int KEY_STRATEGY_REFERENCE = 2;      //刷K线 对标平台
    public static final int KEY_STRATEGY_DEPTH = 3;          //对标深度
    public static final int KEY_STRATEGY_CANCEL = 4;         //撤单
    public static final int KEY_RANDOM_DEPTH = 5;            //随机挂深度
    public static final int KEY_MANUAL_TRADE = 6;            //手动挂单
    public static final int KEY_STRATEGY_NEW_KLINE = 7;            //K线新策略
    public static final int KEY_STRATEGY_REPLENISH = 8;            //补单

    public static final int KEY_STRA_9 = 9;            //补单


    //endregion

    //region    撤单相关
    public static final int KEY_CANCEL_ORDER_STATUS_CANCELLED = 0;      //订单撤销成功
    public static final int KEY_CANCEL_ORDER_STATUS_FAILED = 1;         //订单撤单失败
    public static final int KEU_CANCEL_ORDER_STATUS_UNKNOWN = 2;        //订单不存在
    public static final int KEY_CANCEL_ORDER_STATUS_CANCELLED_BY_THREAD = 3;     //被撤单线程撤单成功
    public static final int KEY_CANCEL_ORDER_STATUS_FILLED = 4;                     //订单完全成交
    public static final int KEY_CANCEL_SLEEP_TIME = 1000 * 60;        //平台接口异常时间，发送短信


    public static final int KEY_CANCEL_ORDER_TYPE_QUANTIFICATION = 1;               //量化撤单
    public static final int KEY_CANCEL_ORDER_TYPE_DEPTH = 2;                        //深度撤单
    public static final int KEY_CANCEL_ORDER_TYPE_REFER_DEPTH = 3;               //对标深度
    public static final int KEY_CANCEL_ORDER_TYPE_REFER_KLINE = 4;               //对标k线


    //endregion

    //region    线程名称
    public static final String KEY_THREAD_KLINE = "KEY_THREAD_KLINE_";             //kline 线程
    public static final String KEY_THREAD_REFER_KLINE = "KEY_THREAD_REFER_KLINE_";             //refer kline 线程
    public static final String KEY_THREAD_REFER_DEPTH = "KEY_THREAD_REFER_DEPTH_";             //refer depth 线程

    public static final String KEY_THREAD_DEPTH = "KEY_THREAD_DEPTH_";             //深度  线程
    public static final String KEY_THREAD_CANCEL = "KEY_THREAD_CANCEL_";           //撤单  线程
    //endregion

    //region    发送短信相关
    public static final int KEY_SNS_INTERFACE_ERROR_TIME = 5 * 1000 * 60;        //平台接口异常时间，发送短信
    public static final int KEY_SNS_SMALL_INTERVAL_TIME = 5 * 1000 * 60;        //区间过小时间，发送短信

    public static final int KEY_SMS_SMALL_INTERVAL = 1;                      //区间过小
    public static final int KEY_SMS_INSUFFICIENT = 2;                        //余额不足
    public static final int KEY_SMS_INTERFACE_ERROR = 3;                     //平台接口异常
    public static final int KEY_SMS_CANCEL_MAX_STOP = 4;                     //撤单达到上限，停止机器人
    public static final List<String> KEY_SMS_MOBLIES = Arrays.asList(        //我们自己手机号
    );
    //endregion


    public static final long KEY_CLEAR_LOG = 1000 * 60 * 60 * 24 * 3;        //清除改时间之前日志
    public static final long KEY_CLEAR_START_LOG = 1000 * 60 * 60 * 12;        //开始清除改时间之前日志
    public static final long KEY_BALACE_TIME = 3 * 60 * 1000;        //清除改时间之前日志
}


