package com.Zx1nggg.FAMS;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.scheduling.enabled=false")
class FamsApplicationTests {

    @org.springframework.test.context.DynamicPropertySource
    static void properties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID());
    }

	@Test
	void contextLoads() {
	}

}
