package ccc.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nz.net.ultraq.thymeleaf.LayoutDialect;

@Configuration
public class WebMvcConfig {

	@Bean
	public LayoutDialect layoutDialect() {
	    return new LayoutDialect();
	}
}
