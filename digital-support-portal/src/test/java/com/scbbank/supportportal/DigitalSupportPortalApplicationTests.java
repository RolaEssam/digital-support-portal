package com.scbbank.supportportal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"auth.encryption.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
})
@ActiveProfiles("local")
class DigitalSupportPortalApplicationTests {

	@Test
	void contextLoads() {
	}
}
