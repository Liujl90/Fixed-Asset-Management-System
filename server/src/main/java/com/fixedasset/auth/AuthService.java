package com.fixedasset.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.auth.dto.LoginRequest;
import com.fixedasset.auth.dto.LoginResponse;
import com.fixedasset.auth.dto.ProfileUpdateRequest;
import com.fixedasset.auth.dto.UserProfileResponse;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.security.AuthenticatedUser;
import com.fixedasset.security.JwtService;
import com.fixedasset.security.SecurityUtils;
import com.fixedasset.system.entity.SysPermission;
import com.fixedasset.system.entity.SysRole;
import com.fixedasset.system.entity.SysRolePermission;
import com.fixedasset.system.entity.SysUser;
import com.fixedasset.system.entity.SysUserRole;
import com.fixedasset.system.mapper.SysPermissionMapper;
import com.fixedasset.system.mapper.SysRoleMapper;
import com.fixedasset.system.mapper.SysRolePermissionMapper;
import com.fixedasset.system.mapper.SysUserMapper;
import com.fixedasset.system.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysPermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        // 登录不区分“账号不存在”和“密码错误”，避免账号枚举。
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(4011, "账号或密码错误");
        }
        AuthenticatedUser authenticatedUser = toAuthenticatedUser(user);
        return new LoginResponse(jwtService.generateToken(authenticatedUser), toProfile(user, authenticatedUser));
    }

    public UserProfileResponse currentProfile() {
        SysUser user = requireCurrentUser();
        return toProfile(user, SecurityUtils.currentUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(ProfileUpdateRequest request) {
        SysUser user = requireCurrentUser();
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        userMapper.updateById(user);
        AuthenticatedUser refreshed = new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmployeeId(),
                SecurityUtils.currentUser().roles(),
                SecurityUtils.currentUser().permissions());
        return toProfile(user, refreshed);
    }

    private SysUser requireCurrentUser() {
        SysUser user = userMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BusinessException(4040, "用户不存在");
        }
        return user;
    }

    private AuthenticatedUser toAuthenticatedUser(SysUser user) {
        // 用户、角色、权限关系在登录时一次性展开并写入 JWT。
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysRole> roles = roleIds.isEmpty()
                ? List.of()
                : roleMapper.selectBatchIds(roleIds);

        List<Long> permissionIds = roleIds.isEmpty()
                ? List.of()
                : rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                                .in(SysRolePermission::getRoleId, roleIds))
                        .stream()
                        .map(SysRolePermission::getPermissionId)
                        .distinct()
                        .toList();

        Set<String> permissions = permissionIds.isEmpty()
                ? Set.of()
                : permissionMapper.selectBatchIds(permissionIds).stream()
                        .map(SysPermission::getCode)
                        .collect(Collectors.toSet());

        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmployeeId(),
                roles.stream().map(SysRole::getCode).collect(Collectors.toSet()),
                permissions);
    }

    private UserProfileResponse toProfile(SysUser user, AuthenticatedUser authenticatedUser) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getEmployeeId(),
                user.getStatus(),
                authenticatedUser.roles(),
                authenticatedUser.permissions());
    }
}
