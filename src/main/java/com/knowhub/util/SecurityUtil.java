package com.knowhub.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    // 获取当前登录用户的 userId
    public static Long getCurrentUserId() {
        return (Long) getAuth().getPrincipal();
    }

    // 获取当前登录用户的 username
    public static String getCurrentUserName() {
        return (String) getAuth().getCredentials();
    }

    private static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
