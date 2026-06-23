package com.petfriends.almoxarifado.domain;

import java.util.Optional;

/**
 * Repository do agregado ItemEstoque (porta do domínio).
 * Só expõe operações sobre o aggregate root.
 */
public interface ItemEstoqueRepository {
    Optional<ItemEstoque> buscarPorSku(SKU sku);
    void salvar(ItemEstoque item);
}
