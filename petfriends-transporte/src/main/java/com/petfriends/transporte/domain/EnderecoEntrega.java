package com.petfriends.transporte.domain;

import java.util.Objects;

/**
 * Value Object: endereço de entrega.
 * Agrupamento coeso e imutável de atributos, validado na construção e
 * comparado por valor (sem identidade própria).
 */
public record EnderecoEntrega(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep) {

    public EnderecoEntrega {
        Objects.requireNonNull(logradouro, "logradouro obrigatório");
        Objects.requireNonNull(cidade, "cidade obrigatória");
        if (uf == null || uf.length() != 2) {
            throw new IllegalArgumentException("UF inválida");
        }
        if (cep == null || !cep.matches("\\d{5}-?\\d{3}")) {
            throw new IllegalArgumentException("CEP inválido");
        }
    }
}
