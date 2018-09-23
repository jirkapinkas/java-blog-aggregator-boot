package cz.jiripinkas.jba;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import cz.jiripinkas.jba.service.ConfigurationInterceptor;

@Configuration
public class WebXmlSpringBoot implements WebMvcConfigurer {

	@Autowired
	private ConfigurationInterceptor configurationInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(configurationInterceptor);
	}

}
