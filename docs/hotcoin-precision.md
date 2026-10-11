# 热币交易精度

各热币策略初始化加载参数后，统一通过HotCoinParentService.setPrecision查询公开接口`/v1/common/symbols`，按market的小写a_b交易对匹配symbol（匹配时忽略大小写与输入首尾空格）。不需要API Key或签名。

- 价格小数位优先使用pricePrecision；不可用时使用配置pricePrecision。
- 数量小数位优先使用amountPrecision；不可用时使用配置amountPrecision。
- 最小币数量优先使用官方minOrderCount，不可用时使用数据库参数minOrderAmount。策略内部保留minTradeLimit别名供既有数量校验读取，不从该别名取配置回退值，也不读取官方minOrderAmount。不按价格乘数量计算金额。

每个字段独立回退。精度须为非负整数，0小数位合法；字段缺失、null、空串、非数字、负数或非整数均不可用。请求异常、响应异常或未匹配到交易对时使用原始配置。官方值仅覆盖运行时参数，不写回数据库；原始配置单独保存，避免再次查询失败时错误回退到旧官方值。

数据库参数重载后清除旧精度，下次使用重新解析；正常运行不逐笔请求规则接口。官方和配置都没有有效值时明确报错，不能猜测精度继续下单。

部署aio并重新加载机器人后生效，无需修改表结构。测试使用独立请求入口返回固定响应，不调用真实账户或下单接口。
