package com.petfriends.transporte.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate Root do microsserviço PetFriends_Transporte.
 *
 * Representa a remessa de um pedido e controla as transições de estado do
 * trânsito (Em Trânsito -> Entregue / Extraviada / Devolvida), coerentes com
 * o diagrama de estados do agregado Pedido.
 */
public class Entrega {

    public enum StatusEntrega { CRIADA, EM_TRANSITO, ENTREGUE, EXTRAVIADA, DEVOLVIDA }

    private final UUID id;
    private final UUID pedidoId;
    private final EnderecoEntrega endereco;
    private StatusEntrega status;
    private LocalDateTime despachadaEm;
    private LocalDateTime atualizadaEm;

    public Entrega(UUID id, UUID pedidoId, EnderecoEntrega endereco) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.endereco = endereco;
        this.status = StatusEntrega.CRIADA;
        this.atualizadaEm = LocalDateTime.now();
    }

    /** Construtor de reconstituição (uso exclusivo do factory). */
    private Entrega(UUID id, UUID pedidoId, EnderecoEntrega endereco, StatusEntrega status,
                    LocalDateTime despachadaEm, LocalDateTime atualizadaEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.endereco = endereco;
        this.status = status;
        this.despachadaEm = despachadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    /**
     * Reconstrói o agregado a partir do estado já persistido — sem disparar
     * regras de transição. Usado pelo repositório ao ler do banco.
     */
    public static Entrega reconstituir(UUID id, UUID pedidoId, EnderecoEntrega endereco,
                                       StatusEntrega status, LocalDateTime despachadaEm,
                                       LocalDateTime atualizadaEm) {
        return new Entrega(id, pedidoId, endereco, status, despachadaEm, atualizadaEm);
    }

    public void despachar() {
        exigir(status == StatusEntrega.CRIADA, "Só é possível despachar uma entrega CRIADA");
        this.status = StatusEntrega.EM_TRANSITO;
        this.despachadaEm = LocalDateTime.now();
        this.atualizadaEm = this.despachadaEm;
    }

    public void confirmarEntrega() {
        exigir(status == StatusEntrega.EM_TRANSITO, "Entrega não está em trânsito");
        this.status = StatusEntrega.ENTREGUE;
        this.atualizadaEm = LocalDateTime.now();
    }

    /** Regra "[decorridos 30 dias]" do diagrama de estados. */
    public void marcarExtraviada() {
        exigir(status == StatusEntrega.EM_TRANSITO, "Entrega não está em trânsito");
        this.status = StatusEntrega.EXTRAVIADA;
        this.atualizadaEm = LocalDateTime.now();
    }

    public void marcarDevolvida() {
        exigir(status == StatusEntrega.EM_TRANSITO, "Entrega não está em trânsito");
        this.status = StatusEntrega.DEVOLVIDA;
        this.atualizadaEm = LocalDateTime.now();
    }

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException(mensagem);
        }
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public EnderecoEntrega getEndereco() { return endereco; }
    public StatusEntrega getStatus() { return status; }
    public LocalDateTime getDespachadaEm() { return despachadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
