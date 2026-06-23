-- Schema do agregado Entrega (microsserviço Transporte).
CREATE TABLE entrega (
    id            UUID         PRIMARY KEY,
    pedido_id     UUID         NOT NULL UNIQUE,
    logradouro    VARCHAR(255),
    numero        VARCHAR(255),
    complemento   VARCHAR(255),
    bairro        VARCHAR(255),
    cidade        VARCHAR(255),
    uf            VARCHAR(2),
    cep           VARCHAR(255),
    status        VARCHAR(255) NOT NULL,
    despachada_em TIMESTAMP,
    atualizada_em TIMESTAMP
);
