package com.knowhub.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int limit() default 10;    // 每秒最多请求次数
    String key() default "";   // 限流 key 前缀
}