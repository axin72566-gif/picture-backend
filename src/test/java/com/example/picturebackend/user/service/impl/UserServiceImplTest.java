package com.example.picturebackend.user.service.impl;

import com.example.picturebackend.common.ErrorCode;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.user.entity.User;
import com.example.picturebackend.user.mapper.UserMapper;
import com.example.picturebackend.user.model.dto.UserLoginRequest;
import com.example.picturebackend.user.model.dto.UserRegisterRequest;
import com.example.picturebackend.user.model.vo.LoginUserVO;
import com.example.picturebackend.user.model.vo.UserVO;
import com.example.picturebackend.utils.JwtUtils;
import com.example.picturebackend.utils.PasswordUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtils.getExpireMillis()).thenReturn(86_400_000L);

        userService = new UserServiceImpl(userMapper, jwtUtils, redisTemplate);
    }

    @Test
    @DisplayName("注册: 成功")
    void userRegister_success() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("alice01");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            u.setCreateTime(LocalDateTime.now());
            u.setUpdateTime(LocalDateTime.now());
            return 1;
        });

        long id = userService.userRegister(req);

        assertEquals(1L, id);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals("alice01", saved.getUserAccount());
        assertNotNull(saved.getUserPassword());
        assertTrue(PasswordUtils.check("password123", saved.getUserPassword()));
        assertEquals(UserConstant.ROLE_USER, saved.getUserRole());
        assertTrue(saved.getUserName().startsWith(UserConstant.DEFAULT_USER_PREFIX));
    }

    @Test
    @DisplayName("注册: 账号已存在")
    void userRegister_duplicateAccount() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("alice01");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");

        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(req));
        assertEquals(ErrorCode.USER_EXIST.getCode(), ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册: 密码不一致")
    void userRegister_passwordMismatch() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("alice01");
        req.setUserPassword("password123");
        req.setCheckPassword("password999");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(req));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册: 账号含特殊字符")
    void userRegister_accountInvalid() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("ali ce!");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(req));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("注册: 账号过短")
    void userRegister_accountTooShort() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("ab");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userRegister(req));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("登录: 成功,返回 token 与 VO")
    void userLogin_success() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("alice01");
        req.setUserPassword("password123");

        User dbUser = new User();
        dbUser.setId(42L);
        dbUser.setUserAccount("alice01");
        dbUser.setUserPassword(PasswordUtils.hash("password123"));
        dbUser.setUserName("user_00000001");
        dbUser.setUserRole(UserConstant.ROLE_USER);

        when(userMapper.selectOne(any())).thenReturn(dbUser);
        when(jwtUtils.generate(42L, UserConstant.ROLE_USER)).thenReturn("jwt-token-xxx");

        LoginUserVO vo = userService.userLogin(req);

        assertNotNull(vo);
        assertEquals("jwt-token-xxx", vo.getToken());
        assertEquals(42L, vo.getUser().getId());
        assertEquals("alice01", vo.getUser().getUserAccount());
        verify(valueOperations).set(eq(UserConstant.LOGIN_USER_KEY_PREFIX + 42L), eq("jwt-token-xxx"), any(Duration.class));
    }

    @Test
    @DisplayName("登录: 账号不存在")
    void userLogin_userNotFound() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("ghost");
        req.setUserPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userLogin(req));
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("登录: 密码错误")
    void userLogin_passwordError() {
        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("alice01");
        req.setUserPassword("wrong-password");

        User dbUser = new User();
        dbUser.setId(42L);
        dbUser.setUserAccount("alice01");
        dbUser.setUserPassword(PasswordUtils.hash("password123"));

        when(userMapper.selectOne(any())).thenReturn(dbUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.userLogin(req));
        assertEquals(ErrorCode.PASSWORD_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("退出登录: 删除登录 key + 写入黑名单")
    void userLogout_success() {
        when(request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR)).thenReturn(42L);
        when(request.getAttribute(UserConstant.CURRENT_USER_TOKEN_ATTR)).thenReturn("jwt-token-xxx");
        when(redisTemplate.delete(UserConstant.LOGIN_USER_KEY_PREFIX + 42L)).thenReturn(true);

        boolean ok = userService.userLogout(request);

        assertTrue(ok);
        verify(redisTemplate).delete(UserConstant.LOGIN_USER_KEY_PREFIX + 42L);
        verify(valueOperations).set(
                eq(UserConstant.JWT_BLACKLIST_KEY_PREFIX + "jwt-token-xxx"),
                eq("1"),
                any(Duration.class)
        );
    }

    @Test
    @DisplayName("退出登录: 未登录直接返回 true")
    void userLogout_notLogin() {
        when(request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR)).thenReturn(null);

        boolean ok = userService.userLogout(request);

        assertTrue(ok);
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("获取当前登录用户: 成功")
    void getLoginUser_success() {
        when(request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR)).thenReturn(42L);
        User dbUser = new User();
        dbUser.setId(42L);
        dbUser.setUserAccount("alice01");
        dbUser.setUserName("alice");
        dbUser.setUserRole(UserConstant.ROLE_USER);
        when(userMapper.selectById(42L)).thenReturn(dbUser);

        UserVO vo = userService.getLoginUser(request);

        assertNotNull(vo);
        assertEquals(42L, vo.getId());
        assertEquals("alice01", vo.getUserAccount());
    }

    @Test
    @DisplayName("获取当前登录用户: 未登录")
    void getLoginUser_notLogin() {
        when(request.getAttribute(UserConstant.CURRENT_USER_ID_ATTR)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
        assertEquals(ErrorCode.NOT_LOGIN.getCode(), ex.getCode());
        verify(userMapper, never()).selectById(any());
    }
}