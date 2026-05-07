package com.Zx1nggg.FAMS.modules.system.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.system.dto.LoginReqDTO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统认证中心：负责登录、注册、Token 签发等
 */
@RestController
@RequestMapping("/auth") // 统一路径为 /auth
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginReqDTO req) {
        // 1. 查询数据库是否存在该账号
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));

        if (user == null) {
            throw new BusinessException(400, "账号或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系系统管理员");
        }

        // 2. 校验密码
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "账号或密码错误");
        }

        // 3. 签发 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 4. 封装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", user.getRealName());
        userInfo.put("role", user.getUserType());
        data.put("user", userInfo);

        return Result.success(data);
    }

    /**
     * 用户注册接口 (预留)
     * 逻辑说明：接收注册信息，保存到 sys_user 表，赋予默认角色
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        // 校验账号是否已存在
        long count = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "该用户名已被占用");
        }

        // 设置默认状态和类型
        user.setStatus((byte) 1); // 默认启用
        user.setUserType("FARMER"); // 默认注册为养殖户端

        userService.save(user);
        return Result.success("注册成功，请登录");
    }
}
