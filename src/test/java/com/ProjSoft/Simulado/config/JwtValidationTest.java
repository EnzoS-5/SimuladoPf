package com.ProjSoft.Simulado.config;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class JwtValidationTest {
    static HttpServer server; static RSAKey key; static String issuer; static JwtDecoder decoder;
    @BeforeAll static void startIssuer() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        server.createContext("/", exchange -> {
            String payload = exchange.getRequestURI().getPath().equals("/jwks") ? new JWKSet(key.toPublicJWK()).toString()
                : "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "jwks\"}";
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start(); decoder = new SecurityConfig().jwtDecoder(issuer, "avaliacoes-api");
    }
    @AfterAll static void stop() { if (server != null) server.stop(0); }
    String sign(String iss, String aud, Instant expiry, RSAKey signingKey) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(iss).subject("auth0|test").audience(aud)
            .issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(expiry)).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(signingKey)); return jwt.serialize();
    }
    @Test void aceitaTokenValido() throws Exception {
        assertEquals("auth0|test", decoder.decode(sign(issuer, "avaliacoes-api", Instant.now().plusSeconds(300), key)).getSubject());
    }
    @Test void rejeitaAudienceAusente() {
        assertThrows(JwtException.class, () -> decoder.decode(sign(issuer, null, Instant.now().plusSeconds(300), key)));
    }
    @Test void rejeitaOutraAudiencia() {
        assertThrows(JwtException.class, () -> decoder.decode(sign(issuer, "outra-api", Instant.now().plusSeconds(300), key)));
    }
    @Test void rejeitaOutroEmissor() {
        assertThrows(JwtException.class, () -> decoder.decode(sign("https://outro.example/", "avaliacoes-api", Instant.now().plusSeconds(300), key)));
    }
    @Test void rejeitaExpirado() {
        assertThrows(JwtException.class, () -> decoder.decode(sign(issuer, "avaliacoes-api", Instant.now().minusSeconds(300), key)));
    }
    @Test void rejeitaOutraAssinatura() {
        assertThrows(JwtException.class, () -> decoder.decode(sign(issuer, "avaliacoes-api", Instant.now().plusSeconds(300), new RSAKeyGenerator(2048).generate())));
    }
    @Test void rolesPreservamScopes() {
        var jwt = Jwt.withTokenValue("x").header("alg", "RS256").subject("u").claim("scope", "read:avaliacoes").claim("roles", List.of("ADMIN", 42)).build();
        var authorities = new SecurityConfig().jwtAuthenticationConverter("roles").convert(jwt).getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertTrue(authorities.containsAll(List.of("ROLE_ADMIN", "SCOPE_read:avaliacoes"))); assertFalse(authorities.contains("ROLE_42"));
    }
    @Test void semRolesNaoGanhaPermissoes() {
        var jwt = Jwt.withTokenValue("x").header("alg", "RS256").subject("u").build();
        assertTrue(new SecurityConfig().jwtAuthenticationConverter("roles").convert(jwt).getAuthorities().isEmpty());
    }
}
