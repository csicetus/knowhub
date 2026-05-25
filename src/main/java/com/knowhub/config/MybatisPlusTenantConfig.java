package com.knowhub.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.knowhub.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class MybatisPlusTenantConfig {

    // 不需要租户过滤的表
    private final List<String> IGNORE_TABLES = Arrays.asList(
            "user",     // 登录时查这张表，还不知道 userId
            "message"   // 通过 conversationId 关联，不直接用 userId 过滤
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                new TenantLineHandler() {
                    @Override
                    public Expression getTenantId() {
                        try {
                            Long userId = SecurityUtil.getCurrentUserId();
                            return new LongValue(userId != null ? userId : 0L);
                        } catch (Exception e) {
                            return new LongValue(0L);  // 未登录时返回0，查不到任何数据
                        }
                    }

                    // 更新默认插件租户字段 tenant_id 为 user_id
                    @Override
                    public String getTenantIdColumn() {
                        return "user_id";
                    }

                    @Override
                    public boolean ignoreTable(String tableName) {
                        // 返回 true 表示这张表跳过租户过滤
                        return IGNORE_TABLES.contains(tableName);
                    }
                }
        ));

        return interceptor;
    }
}
