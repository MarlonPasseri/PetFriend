package com.petfriends.almoxarifado.infra.persistence;

import com.petfriends.almoxarifado.domain.ItemEstoque;
import com.petfriends.almoxarifado.domain.ItemEstoqueRepository;
import com.petfriends.almoxarifado.domain.Quantidade;
import com.petfriends.almoxarifado.domain.SKU;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador: implementa a porta do domínio usando Spring Data JPA. */
@Repository
public class ItemEstoqueRepositoryJpa implements ItemEstoqueRepository {

    private final ItemEstoqueJpaRepository jpa;

    public ItemEstoqueRepositoryJpa(ItemEstoqueJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ItemEstoque> buscarPorSku(SKU sku) {
        return jpa.findBySku(sku.valor()).map(this::toDomain);
    }

    @Override
    public void salvar(ItemEstoque item) {
        jpa.save(toEntity(item));
    }

    private ItemEstoque toDomain(ItemEstoqueJpaEntity e) {
        return new ItemEstoque(
                e.getId(),
                new SKU(e.getSku()),
                new Quantidade(e.getQuantidadeDisponivel()),
                new Quantidade(e.getQuantidadeReservada()));
    }

    private ItemEstoqueJpaEntity toEntity(ItemEstoque item) {
        return new ItemEstoqueJpaEntity(
                item.getId(),
                item.getSku().valor(),
                item.getQuantidadeDisponivel().valor(),
                item.getQuantidadeReservada().valor());
    }
}
