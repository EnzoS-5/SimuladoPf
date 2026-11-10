package com.ProjSoft.Simulado;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
@SpringBootTest
class SimuladoApplicationTests {
    @MockBean JwtDecoder decoder;
    @Test void contextLoads() {}
}
