package com.ProjSoft.Simulado.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 254) private String emailAvaliador;
    @Column(nullable = false, length = 254) private String emailAvaliado;
    @Column(nullable = false, length = 4000) private String comentarios;
    @Column(nullable = false, precision = 4, scale = 2) private BigDecimal nota;
    @Column(nullable = false) private Instant dataAvaliacao;

    protected Avaliacao() {}
    public Avaliacao(String emailAvaliador, String emailAvaliado, String comentarios,
                     BigDecimal nota, Instant dataAvaliacao) {
        this.emailAvaliador = emailAvaliador;
        this.emailAvaliado = emailAvaliado;
        this.comentarios = comentarios;
        this.nota = nota;
        this.dataAvaliacao = dataAvaliacao;
    }
    public Long getId() { return id; }
    public String getEmailAvaliador() { return emailAvaliador; }
    public String getEmailAvaliado() { return emailAvaliado; }
    public String getComentarios() { return comentarios; }
    public BigDecimal getNota() { return nota; }
    public Instant getDataAvaliacao() { return dataAvaliacao; }
}
