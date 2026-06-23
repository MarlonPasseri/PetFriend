package com.petfriends.almoxarifado.api;

import com.petfriends.almoxarifado.infra.persistence.ItemEstoqueJpaEntity;
import com.petfriends.almoxarifado.infra.persistence.ItemEstoqueJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de leitura (read model) para inspeção do estoque. */
@RestController
@RequestMapping("/estoque")
public class EstoqueQueryController {

    private final ItemEstoqueJpaRepository repo;

    public EstoqueQueryController(ItemEstoqueJpaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ItemEstoqueJpaEntity> listar() {
        return repo.findAll();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ItemEstoqueJpaEntity> porSku(@PathVariable String sku) {
        return repo.findBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
