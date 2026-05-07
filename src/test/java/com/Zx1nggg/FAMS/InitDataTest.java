package com.Zx1nggg.FAMS;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InitDataTest {

    @Autowired
    private IUserService sysUserService;

    /**
     * 严谨的企业级测试数据初始化脚本
     * 运行此方法，将自动生成带有独立随机盐的 3 个测试用户
     */
    @Test
    public void initTestUsers() {
        // 1. 清空旧的测试数据 (防重复执行)
        sysUserService.remove(new QueryWrapper<>());

        // 2. 创建系统管理员 (Admin)
        User admin = new User();
        admin.setUsername("admin");
        // 🌟 核心：BCrypt.gensalt() 默认会生成包含 29 个字符的强随机盐，并与哈希值组合
        admin.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        admin.setRealName("系统管理员(总干事)");
        admin.setPhone("13800000001");
        admin.setUserType("ADMIN");
        admin.setStatus((byte) 1);
        sysUserService.save(admin);

        // 3. 创建养殖户 (Farmer)
        User farmer = new User();
        farmer.setUsername("farmer");
        // 每次调用 gensalt 都会重新生成盐，即使明文同为 123456，密文也绝对不同！
        farmer.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        farmer.setRealName("陈老农(基地负责人)");
        farmer.setPhone("13800000002");
        farmer.setUserType("FARMER");
        farmer.setFarmId(1L); // 假设 1 号养殖场已在数据库存在
        farmer.setStatus((byte) 1);
        sysUserService.save(farmer);

        // 4. 创建监管员 (Regulator)
        User leader = new User();
        leader.setUsername("leader");
        leader.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        leader.setRealName("海洋学院研究员");
        leader.setPhone("13800000003");
        leader.setUserType("REGULATOR");
        leader.setStatus((byte) 1);
        sysUserService.save(leader);

        // 打印出结果，让你亲眼见证 BCrypt 的魅力
        System.out.println("=====================================================");
        System.out.println("🎉 测试用户初始化成功！请观察下方密文，它们绝不相同：");
        System.out.println("Admin 密文:  " + admin.getPassword());
        System.out.println("Farmer 密文: " + farmer.getPassword());
        System.out.println("Leader 密文: " + leader.getPassword());
        System.out.println("=====================================================");
    }
}
