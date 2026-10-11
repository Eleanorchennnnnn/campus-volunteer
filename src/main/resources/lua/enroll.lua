-- 抢报名 Lua 脚本：把"校验名额 + 防重复 + 扣名额 + 记录用户"合并为一个原子操作
-- （课程 seckill.lua 的校园版，Redis单线程执行脚本期间不会插入其他命令）
--
-- KEYS[1] 名额key：enroll:stock:{activityId}
-- KEYS[2] 报名用户集合key：enroll:user:{activityId}
-- ARGV[1] 活动ID
-- ARGV[2] 用户ID
--
-- 返回值：0-成功  1-名额已满  2-重复报名  3-名额数据未初始化

local stockKey = KEYS[1]
local enrollKey = KEYS[2]
local userId = ARGV[2]

-- 名额key必须已初始化（由Java侧 ensureRedisState 兜底保证）
if (redis.call('exists', stockKey) == 0) then
    return 3
end

-- 1.校验名额是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- 2.校验是否重复报名（一人一活动）
if (redis.call('sismember', enrollKey, userId) == 1) then
    return 2
end

-- 3.扣减名额
redis.call('incrby', stockKey, -1)

-- 4.记录报名用户
redis.call('sadd', enrollKey, userId)

return 0
