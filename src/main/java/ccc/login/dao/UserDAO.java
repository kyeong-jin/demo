package ccc.login.dao;

import org.apache.ibatis.annotations.Mapper;

import ccc.login.model.UserVO;

@Mapper
public interface UserDAO {
	
	UserVO findByUserId(String userId);
}
