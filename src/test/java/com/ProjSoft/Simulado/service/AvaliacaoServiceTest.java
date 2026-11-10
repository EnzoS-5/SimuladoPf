package com.ProjSoft.Simulado.service;
import com.ProjSoft.Simulado.dto.AvaliacaoRequest;
import com.ProjSoft.Simulado.entity.Avaliacao;
import com.ProjSoft.Simulado.repository.AvaliacaoRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {
    @Mock AvaliacaoRepository repository;
    AvaliacaoService service;
    Instant now = Instant.parse("2026-11-10T18:00:00Z");
    @BeforeEach void setup() { service = new AvaliacaoService(repository, Clock.fixed(now, ZoneOffset.UTC)); }
    @Test void listaOrdenada() {
        var expected = List.of(new Avaliacao("a@x.com", "b@x.com", "Bom", BigDecimal.TEN, now));
        var sort = Sort.by(Sort.Direction.DESC, "dataAvaliacao", "id");
        when(repository.findAll(sort)).thenReturn(expected);
        assertSame(expected, service.listar());
        verify(repository).findAll(sort);
    }
    @Test void criaComIdentidadeEDataDoServidor() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.criar(new AvaliacaoRequest("PESSOA@empresa.com", " Bom trabalho ", new BigDecimal("8.50")), " ADMIN@empresa.com ");
        assertEquals("admin@empresa.com", result.getEmailAvaliador());
        assertEquals("pessoa@empresa.com", result.getEmailAvaliado());
        assertEquals("Bom trabalho", result.getComentarios());
        assertEquals(new BigDecimal("8.50"), result.getNota());
        assertEquals(now, result.getDataAvaliacao());
        verify(repository).save(result);
    }
    @Test void rejeitaAutoavaliacao() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(new AvaliacaoRequest("ADMIN@empresa.com", "Bom", BigDecimal.TEN), "admin@empresa.com"));
        verifyNoInteractions(repository);
    }
}
