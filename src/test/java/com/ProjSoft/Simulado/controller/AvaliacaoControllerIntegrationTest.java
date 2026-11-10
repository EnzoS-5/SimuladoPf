package com.ProjSoft.Simulado.controller;
import com.ProjSoft.Simulado.entity.Avaliacao;
import com.ProjSoft.Simulado.repository.AvaliacaoRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class AvaliacaoControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AvaliacaoRepository repository;
    @MockBean JwtDecoder decoder;
    static final String BODY = "{\"emailAvaliado\":\"pessoa@empresa.com\",\"comentarios\":\"Bom trabalho\",\"nota\":8.5}";
    @BeforeEach void setup() {
        repository.deleteAll(); reset(decoder);
        token("admin", "ADMIN", "admin@empresa.com");
        token("user", "USER", "user@empresa.com");
        token("guest", "GUEST", "guest@empresa.com");
    }
    void token(String value, String role, String email) {
        var builder = Jwt.withTokenValue(value).header("alg", "RS256").subject("auth0|" + value).claim("https://social-insper.com/roles", java.util.List.of(role));
        if (email != null) builder.claim("https://social-insper.com/email", email);
        when(decoder.decode(value)).thenReturn(builder.build());
    }
    @Test void semTokenNaoLista() throws Exception {
        mvc.perform(get("/avaliacoes")).andExpect(status().isUnauthorized());
    }
    @Test void semTokenNaoCadastra() throws Exception {
        mvc.perform(post("/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        assertEquals(0, repository.count());
    }
    @Test void tokenInvalidoRecebe401() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("Inválido"));
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void userNaoCadastra() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer user").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        assertEquals(0, repository.count());
    }
    @Test void perfilDesconhecidoNaoLista() throws Exception {
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer guest")).andExpect(status().isForbidden());
    }
    @Test void adminCadastraEPersiste() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/avaliacoes/[0-9]+")))
            .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.emailAvaliador").value("admin@empresa.com"))
            .andExpect(jsonPath("$.emailAvaliado").value("pessoa@empresa.com"))
            .andExpect(jsonPath("$.comentarios").value("Bom trabalho"))
            .andExpect(jsonPath("$.nota").value(8.5)).andExpect(jsonPath("$.dataAvaliacao").isNotEmpty());
        assertEquals("admin@empresa.com", repository.findAll().getFirst().getEmailAvaliador());
        assertNotNull(repository.findAll().getFirst().getDataAvaliacao());
    }
    @ParameterizedTest @ValueSource(strings = {"admin", "user"})
    void ambosPerfisListam(String token) throws Exception {
        repository.save(new Avaliacao("a@x.com", "b@x.com", "Antiga", java.math.BigDecimal.ONE, Instant.parse("2026-01-01T00:00:00Z")));
        repository.save(new Avaliacao("a@x.com", "c@x.com", "Recente", java.math.BigDecimal.TEN, Instant.parse("2026-02-01T00:00:00Z")));
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].comentarios").value("Recente"));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "10"})
    void aceitaLimites(String nota) throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("8.5", nota))).andExpect(status().isCreated());
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "10.01", "null", "1.234"})
    void rejeitaNotasInvalidas(String nota) throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("8.5", nota))).andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }
    @Test void rejeitaEmailInvalido() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("pessoa@empresa.com", "invalido"))).andExpect(status().isBadRequest());
    }
    @Test void rejeitaComentarioVazio() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("Bom trabalho", " "))).andExpect(status().isBadRequest());
    }
    @Test void rejeitaAvaliadorOuDataForjados() throws Exception {
        var forged = BODY.replace("}", ",\"emailAvaliador\":\"outra@empresa.com\",\"dataAvaliacao\":\"2000-01-01T00:00:00Z\"}");
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(forged)).andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }
    @Test void rejeitaAutoavaliacao() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("pessoa@empresa.com", "admin@empresa.com"))).andExpect(status().isBadRequest());
    }
    @Test void exigeEmailDoToken() throws Exception {
        token("noemail", "ADMIN", null);
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer noemail").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }
    @Test void permitePreflightConfigurado() throws Exception {
        mvc.perform(options("/avaliacoes").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test void rejeitaOutraOrigem() throws Exception {
        mvc.perform(options("/avaliacoes").header("Origin", "https://outra.example").header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
}
