package com.aigc.cartoon.service.impl;

import com.aigc.cartoon.entity.User;
import com.aigc.cartoon.mapper.UserMapper;
import com.aigc.cartoon.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    @Override
    public List<User> list() {
        return userMapper.selectList(null);
    }
    
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Override
    public boolean save(User user) {
        return userMapper.insert(user) > 0;
    }
    
    @Override
    public boolean updateById(User user) {
        return userMapper.updateById(user) > 0;
    }
    
    @Override
    public boolean removeById(Long id) {
        return userMapper.deleteById(id) > 0;
    }
}
