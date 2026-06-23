package com.petfriends.transporte.infra.persistence;

import com.petfriends.transporte.domain.Entrega;
import com.petfriends.transporte.domain.EnderecoEntrega;
import com.petfriends.transporte.domain.EntregaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta do domínio usando Spring Data JPA. */
@Repository
public class EntregaRepositoryJpa implements EntregaRepository {

    private final EntregaJpaRepository jpa;

    public EntregaRepositoryJpa(EntregaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Entrega> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Entrega> buscarPorPedido(UUID pedidoId) {
        return jpa.findByPedidoId(pedidoId).map(this::toDomain);
    }

    @Override
    public void salvar(Entrega entrega) {
        jpa.save(toEntity(entrega));
    }

    private Entrega toDomain(EntregaJpaEntity e) {
        EnderecoEmbeddable end = e.getEndereco();
        EnderecoEntrega endereco = new EnderecoEntrega(
                end.getLogradouro(), end.getNumero(), end.getComplemento(),
                end.getBairro(), end.getCidade(), end.getUf(), end.getCep());

        return Entrega.reconstituir(
                e.getId(), e.getPedidoId(), endereco,
                e.getStatus(), e.getDespachadaEm(), e.getAtualizadaEm());
    }

    private EntregaJpaEntity toEntity(Entrega entrega) {
        EnderecoEntrega end = entrega.getEndereco();
        EnderecoEmbeddable embeddable = new EnderecoEmbeddable(
                end.logradouro(), end.numero(), end.complemento(),
                end.bairro(), end.cidade(), end.uf(), end.cep());

        return new EntregaJpaEntity(
                entrega.getId(),
                entrega.getPedidoId(),
                embeddable,
                entrega.getStatus(),
                entrega.getDespachadaEm(),
                entrega.getAtualizadaEm());
    }
}
