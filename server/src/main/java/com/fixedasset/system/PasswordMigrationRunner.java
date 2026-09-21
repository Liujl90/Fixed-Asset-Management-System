package com.fixedasset.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.system.entity.SysUser;
import com.fixedasset.system.mapper.SysUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Flyway 种子数据使用 INIT: 前缀标记初始密码，启动后升级为 BCrypt。
        userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .likeRight(SysUser::getPassword, "INIT:"))
                .forEach(user -> {
                    user.setPassword(passwordEncoder.encode("123456"));
                    userMapper.updateById(user);
                });
    }
}
