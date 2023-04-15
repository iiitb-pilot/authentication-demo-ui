package io.mosip.authentication.usecase.config;

import org.springframework.context.annotation.Configuration;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

@Configuration
public class AppConfig {

	public static Logger getLogger(Class<?> className) {
		return Logfactory.getSlf4jLogger(className);
	}
	
}
