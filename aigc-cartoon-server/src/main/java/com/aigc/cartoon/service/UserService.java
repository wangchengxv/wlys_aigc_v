package com.aigc.cartoon.service;

import com.aigc.cartoon.entity.User;
import java.util.List;

/**
 * 用户Service接口
 */
public interface UserService {
    
    /**
     * 获取所有用户
     */
    List<User> list();
    
    /**
     * 根据ID获取用户
     */
    User getById(Long id);
    
    /**
     * 创建用户
     */
    boolean save(User user);
    
    /**
     * 更新用户
     */
    boolean updateById(User user);
    
    /**
     * 删除用户
     */
    boolean removeById(Long id);
}
