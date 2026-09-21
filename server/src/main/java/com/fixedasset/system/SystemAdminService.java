package com.fixedasset.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.system.dto.RoleUpdateRequest;
import com.fixedasset.system.dto.UserSaveRequest;
import com.fixedasset.system.dto.UserView;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SystemAdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemAdminService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysPermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserView> users() {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery().orderByAsc(SysUser::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    @OperationLog(module = "用户角色", action = "新增用户")
    public UserView createUser(UserSaveRequest request) {
        requireUniqueUsername(request.username(), null);
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(
                request.password() == null || request.password().isBlank() ? "123456" : request.password()));
        user.setRealName(request.realName());
        user.setEmployeeId(request.employeeId());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setStatus(request.status() == null ? "ACTIVE" : request.status());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        replaceUserRole(user.getId(), request.roleId());
        return toView(user);
    }

    @Transactional
    @OperationLog(module = "用户角色", action = "编辑用户")
    public UserView updateUser(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        requireUniqueUsername(request.username(), id);
        user.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setRealName(request.realName());
        user.setEmployeeId(request.employeeId());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setStatus(request.status() == null ? user.getStatus() : request.status());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        replaceUserRole(id, request.roleId());
        return toView(user);
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
    }

    public List<SysPermission> permissions() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getId));
    }

    @Transactional
    @OperationLog(module = "用户角色", action = "配置角色权限")
    public SysRole updateRole(Long id, RoleUpdateRequest request) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        role.setName(request.name());
        role.setCode(request.code());
        role.setDescription(request.description());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
        if (request.permissionIds() != null) {
            request.permissionIds().stream().distinct().forEach(permissionId -> {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(id);
                relation.setPermissionId(permissionId);
                rolePermissionMapper.insert(relation);
            });
        }
        return role;
    }

    public List<Long> rolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream()
                .map(SysRolePermission::getPermissionId)
                .toList();
    }

    private UserView toView(SysUser user) {
        SysUserRole relation = userRoleMapper.selectOne(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, user.getId())
                .last("LIMIT 1"));
        SysRole role = relation == null ? null : roleMapper.selectById(relation.getRoleId());
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                role == null ? null : role.getId(),
                role == null ? null : role.getName(),
                user.getEmployeeId(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus());
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private void requireUniqueUsername(String username, Long excludeId) {
        long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("登录账号已存在");
        }
    }

    private void replaceUserRole(Long userId, Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException("角色不存在");
        }
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }
}
