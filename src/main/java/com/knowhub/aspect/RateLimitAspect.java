package com.knowhub.aspect;

import com.knowhub.common.exception.BusinessException;
import com.knowhub.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.knowhub.aspect.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        Long userId = SecurityUtil.getCurrentUserId();
        String rateLimitKey = "rate:limit:" + rateLimit.key() + ":" + userId;

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.limit(), 1, RateIntervalUnit.SECONDS);

        if (!rateLimiter.tryAcquire()) {
            log.warn("限流触发: userId={}, key={}", userId, rateLimitKey);
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        }

        return joinPoint.proceed();
    }
}
