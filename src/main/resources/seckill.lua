--1.参数列表
--1.1优惠券id
local voucherId=ARGV[1]
--1.2用户id
local userId=ARGV[2]
--1.3订单id
local orderId=ARGV[3]

--2.数据key
--2.1库存key
local stockKey = 'seckill:stock:'..voucherId
--2.2订单key
local orderKey = 'seckill:order:'..voucherId

--3.脚本业务
--3.1 判断库存是否充足 get stockKey
if(tonumber( redis.call('get',stockKey))<=0)then
    --3.2库存不足
    return 1
end
--3.2判断用户是否下单
if(redis.call('sismember',orderKey,userId)==1) then
    --3.3存在说明重复下单
    return 2
end

--3.4 扣库存
redis.call('incrby',stockKey,-1)
--3.5 下单
redis.call('sadd',orderKey,userId)
--3.6 发送消息到队列中,XADD stream.orders * k1 v1 k2 v2
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',orderId)
return 0