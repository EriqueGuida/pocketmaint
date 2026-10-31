package br.com.pocketmaint;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Teste de contexto Spring Boot requer container MySQL ativo")
@SpringBootTest
class PocketMaintApplicationTests {

	@Test
	void contextLoads() {
	}

}
