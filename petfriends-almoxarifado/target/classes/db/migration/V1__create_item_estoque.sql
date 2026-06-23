-- Schema do agregado ItemEstoque (microsserviço Almoxarifado).
CREATE TABLE item_estoque (
    id                    UUID         PRIMARY KEY,
    sku                   VARCHAR(255) NOT NULL UNIQUE,
    quantidade_disponivel INTEGER      NOT NULL,
    quantidade_reservada  INTEGER      NOT NULL
);

-- Estoque inicial para demonstração do fluxo de eventos.
INSERT INTO item_estoque (id, sku, quantidade_disponivel, quantidade_reservada)
VALUES ('a1a1a1a1-0000-0000-0000-000000000001', 'RACAO-001', 100, 0);
