-- Registro de eventos já processados (idempotência do consumo at-least-once).
-- Garante que uma reentrega do mesmo PedidoConfirmadoEvent não reserve estoque
-- em duplicidade.
CREATE TABLE evento_processado (
    event_id      UUID      PRIMARY KEY,
    processado_em TIMESTAMP NOT NULL
);
