package com.ProjSoft.Simulado.controller;

import com.ProjSoft.Simulado.dto.AvaliacaoRequest;
import com.ProjSoft.Simulado.entity.Avaliacao;
import com.ProjSoft.Simulado.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {
    private final AvaliacaoService service;
    private final String emailClaim;

    public AvaliacaoController(AvaliacaoService service,
            @Value("${app.auth0.email-claim}") String emailClaim) {
        this.service = service;
        this.emailClaim = emailClaim;
    }

    @GetMapping
    public List<Avaliacao> listar() { return service.listar(); }

    @PostMapping
    public ResponseEntity<Avaliacao> criar(@Valid @RequestBody AvaliacaoRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        Object claim = jwt.getClaims().get(emailClaim);
        if (!(claim instanceof String email) || email.isBlank()) {
            throw new AccessDeniedException("Token sem e-mail do avaliador. Configure a Action no Auth0.");
        }
        Avaliacao criada = service.criar(request, email);
        return ResponseEntity.created(URI.create("/avaliacoes/" + criada.getId())).body(criada);
    }
}
