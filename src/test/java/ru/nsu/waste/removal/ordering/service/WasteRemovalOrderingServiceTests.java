package ru.nsu.waste.removal.ordering.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:contextdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"app.jobs.user-action-event-processor.enabled=false"
})
class WasteRemovalOrderingServiceTests {

	@Test
	void contextLoads() {
	}

}
