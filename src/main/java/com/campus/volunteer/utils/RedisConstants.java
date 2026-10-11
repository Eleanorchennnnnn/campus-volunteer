package com.campus.volunteer.utils;

/**
 * Redis key 与 TTL 常量（已全部切换为校园平台业务语义）
 */
public class RedisConstants {
    // ---------- 登录会话 ----------
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    // ---------- 活动缓存（第四步启用）----------
    /** 缓存穿透：空值标记的TTL（分钟） */
    public static final Long CACHE_NULL_TTL = 2L;
    /** 活动详情缓存基础TTL（分钟），实际写入时再加随机偏移防雪崩 */
    public static final Long CACHE_ACTIVITY_TTL = 30L;
    public static final String CACHE_ACTIVITY_KEY = "cache:activity:";
    /** 活动分类列表缓存（分钟） */
    public static final String CACHE_ACTIVITY_TYPE_KEY = "cache:activity:type:list";
    public static final Long CACHE_ACTIVITY_TYPE_TTL = 1440L;
    /** 缓存重建互斥锁 */
    public static final String LOCK_ACTIVITY_KEY = "lock:activity:";
    public static final Long LOCK_ACTIVITY_TTL = 10L;

    // ---------- 后续步骤预留 ----------
    /** 报名名额预扣（第五步：抢报名） */
    public static final String ENROLL_STOCK_KEY = "enroll:stock:";
    /** 已报名用户集合（一人一活动，Lua校验用） */
    public static final String ENROLL_USER_KEY = "enroll:user:";
    /** 帖子点赞（Set，一人一赞） */
    public static final String POST_LIKED_KEY = "post:liked:";
    /** 关注推送（Feed收件箱，ZSet） */
    public static final String FEED_KEY = "feed:";
    /** 附近活动（GEO） */
    public static final String ACTIVITY_GEO_KEY = "activity:geo:";
    /** 活动现场签到（BitMap，key=活动场次，offset=用户ID，第六步启用） */
    public static final String ACTIVITY_SIGN_KEY = "sign:activity:";
    /** 志愿时长榜（ZSet，member=用户ID，score=累计小时，第六步启用） */
    public static final String RANK_KEY = "volunteer:rank";
}
