package com.mt.agent.workflow.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mt.agent.workflow.api.entity.SysUser;
import com.mt.agent.workflow.api.mapper.SysUserMapper;
import com.mt.agent.workflow.api.service.SysUserService;
import com.mt.agent.workflow.api.util.MD5Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    
    @Override
    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        wrapper.eq(SysUser::getStatus, "0");
        SysUser user = getOne(wrapper);
        
        if (user != null && MD5Util.verify(password, user.getPassword())) {
            user.setPassword(null);
            return user;
        }
        return null;
    }
    
    @Override
    public IPage<SysUser> pageList(Page<SysUser> page, String username, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getId);
        
        IPage<SysUser> result = page(page, wrapper);
        result.getRecords().forEach(user -> user.setPassword(null));
        return result;
    }
    
    @Override
    public boolean addUser(SysUser user) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        user.setPassword(MD5Util.encrypt(user.getPassword()));
        user.setStatus("0");
        user.setDeleted(0);
        return save(user);
    }
    
    @Override
    public boolean updateUser(SysUser user) {
        SysUser existUser = getById(user.getId());
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!existUser.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, user.getUsername());
            wrapper.ne(SysUser::getId, user.getId());
            if (count(wrapper) > 0) {
                throw new RuntimeException("用户名已存在");
            }
        }
		
		LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
		uw.eq(SysUser::getId, user.getId());
		if (StringUtils.hasText(user.getUsername())) {
			uw.set(SysUser::getUsername, user.getUsername());
		}
		if (StringUtils.hasText(user.getStatus())) {
			uw.set(SysUser::getStatus, user.getStatus());
		}
		uw.set(SysUser::getRemark, user.getRemark());
		if (StringUtils.hasText(user.getPassword())) {
			uw.set(SysUser::getPassword, MD5Util.encrypt(user.getPassword()));
		}
		return update(uw);
    }
    
    @Override
    public boolean deleteUser(Long id) {
        return removeById(id);
    }
    
    @Override
    public boolean updateStatus(Long id, String status) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, id);
        wrapper.set(SysUser::getStatus, status);
        return update(wrapper);
    }
}