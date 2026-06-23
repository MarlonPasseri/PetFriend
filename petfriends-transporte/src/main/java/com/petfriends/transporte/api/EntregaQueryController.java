package com.petfriends.transporte.api;

import com.petfriends.transporte.infra.persistence.EntregaJpaEntity;
import com.petfriends.transporte.infra.persistence.EntregaJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Endpoints de leitura (read model) para inspeção das entregas. */
@RestController
@RequestMapping("/entregas")
public class EntregaQueryController {

    private final EntregaJpaRepository repo;

    public EntregaQueryController(EntregaJpaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<EntregaJpaEntity> listar() {
        return repo.findAll();
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<EntregaJpaEntity> porPedido(@PathVariable UUID pedidoId) {
        return repo.findByPedidoId(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
