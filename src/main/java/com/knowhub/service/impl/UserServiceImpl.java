package com.knowhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowhub.common.exception.BusinessException;
import com.knowhub.dto.LoginRequest;
import com.knowhub.dto.RegisterRequest;
import com.knowhub.entity.User;
import com.knowhub.mapper.UserMapper;
import com.knowhub.service.UserService;
import com.knowhub.util.JwtUtil;
import com.knowhub.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        String username = request.getUsername();
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. 密码 BCrypt 加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 保存用户
        User user = User.builder()
                .username(username)
                .password(encodedPassword)
                .build();
        userMapper.insert(user);

        log.info("用户注册成功: {}", request.getUsername());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        String username = request.getUsername();
        String password = request.getPassword();

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 生成 token 并返回
        String token = jwtUtil.generateToken(user.getId(), username);
        log.info("用户登录成功: {}", username);

        return new LoginResponse(token, username, user.getId());
    }
}
