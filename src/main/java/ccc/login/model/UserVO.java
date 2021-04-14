package ccc.login.model;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude={"empPw"})
public class UserVO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4853251119735714653L;
	
	private String empId;
	private String empPw;
	private String empName;
	private List<GrantedAuthority> authorities;
}
