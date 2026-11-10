package com.ProjSoft.Simulado.controller;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=${IT_DB_URL}", "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=${IT_DB_USER}", "spring.datasource.password=${IT_DB_PASSWORD}"
})
class AvaliacaoPostgresIT extends AvaliacaoControllerIntegrationTest {}
