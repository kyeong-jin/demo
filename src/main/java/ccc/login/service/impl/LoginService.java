package ccc.login.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ccc.login.UserPrincipal;
import ccc.login.dao.UserDAO;
import ccc.login.model.UserVO;

@Service
public class LoginService implements UserDetailsService {
	
	@Autowired
	private UserDAO userDAO;

	@Override
	@Bean
	@Profile("user")
	public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
		UserVO user = userDAO.findByUserId(username);
		List<GrantedAuthority> userAuthRoles = new ArrayList<>();
		userAuthRoles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		user.setAuthorities(userAuthRoles);
		
		return new UserPrincipal(user);
	}
}
