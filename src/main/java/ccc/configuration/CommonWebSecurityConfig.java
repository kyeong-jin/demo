package ccc.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import ccc.handler.LoginFailureHandler;
import ccc.handler.LoginSuccessHandler;
import ccc.login.service.impl.LoginService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class CommonWebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private LoginService loginService;
	
	public static void main(String[] args) {
		String str = new BCryptPasswordEncoder().encode("test");
		System.out.println(str);
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		http.csrf().disable().headers().disable()
			.authorizeRequests();
		
		http
			.authorizeRequests()
			.antMatchers("/main/**")
			.hasAnyRole("ADMIN");
		
		http.authorizeRequests().anyRequest().authenticated();
		
		http.formLogin()
			.permitAll()
			.successHandler(loginSuccessHandler())
			.failureHandler(loginFailureHandler())
        .and()
        	.logout()
        	.invalidateHttpSession(false);
		
		http.sessionManagement()
			.maximumSessions(1)
			.maxSessionsPreventsLogin(true);
	}
	
	@Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(loginService).passwordEncoder(passwordEncoder());
    }
	
	@Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return new LoginFailureHandler();
    }
	
	@Bean
	public AuthenticationSuccessHandler loginSuccessHandler() {
		return new LoginSuccessHandler();
	}
	
}
