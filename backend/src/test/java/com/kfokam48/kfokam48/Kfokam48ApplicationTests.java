package com.kfokam48.kfokam48;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte démarre : H2 en mémoire (profil test), migrations
 * Flyway réelles — jamais PostgreSQL, indisponible sur la CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class Kfokam48ApplicationTests {

	@Test
	void contextLoads() {
	}

}
