package com.mt.agent.workflow.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mt.agent.workflow.api.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    
    SysUser login(String username, String password);
    
    IPage<SysUser> pageList(Page<SysUser> page, String username, String status);
    
    boolean addUser(SysUser user);
    
    boolean updateUser(SysUser user);
    
    boolean deleteUser(Long id);
    
    boolean updateStatus(Long id, String status);
}