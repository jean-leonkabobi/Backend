package com.banksecurity.backend;

import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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

		// ✅ Utilisation de DateUtils.toZonedDateTime
		ZonedDateTime appStartTime = DateUtils.toZonedDateTime(LocalDateTime.now(), Constants.TIMEZONE);

		// ✅ Utilisation de Constants.APP_NAME et APP_VERSION
		log.info("{} v{} démarré avec succès à {}", Constants.APP_NAME, Constants.APP_VERSION, appStartTime);
	}
}