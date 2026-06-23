package com.petfriends.transporte.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Mapeamento do Value Object EnderecoEntrega como colunas embutidas na tabela. */
@Embeddable
public class EnderecoEmbeddable {

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;

    @Column(length = 2)
    private String uf;

    private String cep;

    protected EnderecoEmbeddable() {
    }

    public EnderecoEmbeddable(String logradouro, String numero, String complemento,
                              String bairro, String cidade, String uf, String cep) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.cep = cep;
    }

    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public String getCep() { return cep; }
}
