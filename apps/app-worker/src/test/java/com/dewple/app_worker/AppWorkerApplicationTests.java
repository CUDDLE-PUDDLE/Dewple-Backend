package com.dewple.app_worker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Worker context는 외부 인프라(DB, SQS 등) 연결이 필요하므로 CI에서 비활성화")
class AppWorkerApplicationTests {

	@Test
	void contextLoads() {
	}

}
