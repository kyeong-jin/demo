package ccc.configuration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@PropertySource("classpath:/jdbc.properties")
@MapperScan(basePackages = {"*.**.dao.**"})
public class DatabaseConfig {
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}

	@Bean
	public DataSource dataSource() {
		DataSource dataSource = new HikariDataSource(hikariConfig());
		log.info("datasource : {}", dataSource);
		return dataSource;
	}
	
	@Bean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		sqlSessionFactoryBean.setDatabaseIdProvider(databaseIdProvider());
		PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();
		InputStream fins = getClass().getClassLoader().getResourceAsStream("jdbc.properties"); 
		Properties prop = new Properties();
		prop.load(fins);
		sqlSessionFactoryBean.setConfigLocation(pmrpr.getResource("classpath:/mybatis/mybatis-config.xml"));
		sqlSessionFactoryBean.setConfigurationProperties(prop);
		
		Resource[] resourceCommon = applicationContext.getResources("classpath:/mybatis/mapper/**/*.xml");
		
		List<Resource> resourceList = new ArrayList<>();
		for (int i = 0; i < resourceCommon.length; i++) {
			resourceList.add(resourceCommon[i]);
		}
		Resource[] resources = new Resource[resourceList.size()];
		int ind = 0;
		for (Resource res : resourceList) {
			resources[ind++] = res;
		}
		sqlSessionFactoryBean.setMapperLocations(resources);
		return sqlSessionFactoryBean.getObject();
	}
	
	@Bean
	public VendorDatabaseIdProvider databaseIdProvider() {
		VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
		Properties properties = new Properties();
		properties.put("MySQL", "mysql");
		databaseIdProvider.setProperties(properties);
		return databaseIdProvider;
	} 

}
