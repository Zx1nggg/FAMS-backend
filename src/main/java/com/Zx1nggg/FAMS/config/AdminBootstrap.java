package com.Zx1nggg.FAMS.config;

import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 新空库首次部署时显式启用，不提供默认密码，也不覆盖任何现有账号。 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    @Value("${BOOTSTRAP_ADMIN_PHONE:}") private String phone;
    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") private String password;
    public AdminBootstrap(UserMapper users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }
    @Override public void run(ApplicationArguments args) {
        if (users.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserType, "ADMIN")) > 0) return;
        if (!phone.matches("^1[3-9]\\d{9}$") || password.length() < 6
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("首次管理员初始化需要有效的 BOOTSTRAP_ADMIN_PHONE 和 BOOTSTRAP_ADMIN_PASSWORD 环境变量");
        }
        User user = new User(); user.setPhone(phone); user.setPassword(encoder.encode(password));
        user.setUsername("管理员"); user.setRealName("管理员"); user.setUserType("ADMIN"); user.setStatus((byte) 1); user.setAuthVersion(0L);
        users.insert(user);
    }
}
