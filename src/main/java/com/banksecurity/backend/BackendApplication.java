package com.banksecurity.backend;

import com.banksecurity.backend.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		// ✅ Utilisation de Constants.TIMEZONE
		TimeZone.setDefault(TimeZone.getTimeZone(Constants.TIMEZONE));

		SpringApplication.run(BackendApplication.class, args);

		// ✅ Utilisation de Constants.APP_NAME et APP_VERSION
		log.info("{} v{} démarré avec succès", Constants.APP_NAME, Constants.APP_VERSION);
	}
}