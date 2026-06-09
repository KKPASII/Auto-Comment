package com.hamplz.autocomment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "review.queue.worker-enabled=false")
class AutocommentApplicationTests {

	@Test
	void contextLoads() {
	}

}
