package com.example.picturebackend.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.converter.UserConverter;
import com.example.picturebackend.user.model.dto.UserLoginRequest;
import com.example.picturebackend.user.model.dto.UserRegisterRequest;
import com.example.picturebackend.user.model.vo.LoginUserVO;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.user.service.UserService;
import com.example.picturebackend.utils.JwtUtils;
import com.example.picturebackend.utils.PasswordUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String ACCOUNT_PATTERN = "^[a-zA-Z0-9_]+$";

    private final UserMapper userMapper;

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate redisTemplate;

    public UserServiceImpl(UserMapper userMapper, JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long userRegister(UserRegisterRequest request) {
        String account = request.getUserAccount();
        String password = request.getUserPassword();
        String checkPassword = request.getCheckPassword();

        if (StringUtils.isBlank(account) || StringUtils.isBlank(password) || StringUtils.isBlank(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (account.length() < 4 || account.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度应为 4-16 位");
        }
        if (password.length() < 8 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应为 8-32 位");
        }
        if (!account.matches(ACCOUNT_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUserAccount, account));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USER_EXIST);
        }

        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(PasswordUtils.hash(password));
        user.setUserName(UserConstant.DEFAULT_USER_PREFIX + randomSuffix());
        user.setUserRole(UserConstant.ROLE_USER);
        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest request) {
        String account = request.getUserAccount();
        String password = request.getUserPassword();

        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUserAccount, account));
        if (user == null) {
            log.warn("登录失败: 账号不存在 account={}", account);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PasswordUtils.check(password, user.getUserPassword())) {
            log.warn("登录失败: 密码错误 account={}", account);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generate(user.getId(), user.getUserRole());
        redisTemplate.opsForValue().set(
                UserConstant.LOGIN_USER_KEY_PREFIX + user.getId(),
                token,
                Duration.ofMillis(jwtUtils.getExpireMillis())
        );

        LoginUserVO vo = new LoginUserVO();
        vo.setToken(token);
        vo.setUser(UserConverter.toVO(user));
        return vo;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        String token = (String) request.getAttribute(UserConstant.CURRENT_USER_TOKEN_ATTR);
        if (userId == null) {
            return true;
        }
        Boolean deleted = redisTemplate.delete(UserConstant.LOGIN_USER_KEY_PREFIX + userId);
        if (token != null) {
            redisTemplate.opsForValue().set(
                    UserConstant.JWT_BLACKLIST_KEY_PREFIX + token,
                    "1",
                    Duration.ofMillis(jwtUtils.getExpireMillis())
            );
        }
        return Boolean.TRUE.equals(deleted) || token != null;
    }

    @Override
    public UserVO getLoginUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserConverter.toVO(user);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    private String randomSuffix() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(0, 100_000_000));
    }
}