package com.ProjSoft.Simulado.service;

import com.ProjSoft.Simulado.dto.AvaliacaoRequest;
import com.ProjSoft.Simulado.entity.Avaliacao;
import com.ProjSoft.Simulado.repository.AvaliacaoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AvaliacaoService {
    private final AvaliacaoRepository repository;
    private final Clock clock;

    public AvaliacaoService(AvaliacaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Avaliacao> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "dataAvaliacao", "id"));
    }

    @Transactional
    public Avaliacao criar(AvaliacaoRequest request, String emailAvaliador) {
        String avaliador = emailAvaliador.trim().toLowerCase(Locale.ROOT);
        String avaliado = request.emailAvaliado().trim().toLowerCase(Locale.ROOT);
        if (avaliador.equals(avaliado)) {
            throw new IllegalArgumentException("Avalie outra pessoa; autoavaliação não é permitida.");
        }
        return repository.save(new Avaliacao(avaliador, avaliado, request.comentarios().trim(),
                request.nota(), Instant.now(clock)));
    }
}
