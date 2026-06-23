package com.petfriends.transporte.application;

import com.petfriends.transporte.config.TransporteMessagingConfig;
import com.petfriends.transporte.domain.EnderecoEntrega;
import com.petfriends.transporte.domain.Entrega;
import com.petfriends.transporte.domain.EntregaRepository;
import com.petfriends.shared.events.PedidoDespachadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serviço que recebe os eventos de PetFriends_Pedidos e cria a Entrega
 * correspondente, colocando-a em trânsito.
 */
@Service
public class PedidoDespachadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoDespachadoListener.class);

    private final EntregaRepository repository;

    public PedidoDespachadoListener(EntregaRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = TransporteMessagingConfig.QUEUE)
    @Transactional
    public void onPedidoDespachado(PedidoDespachadoEvent evento) {
        log.info("Recebido PedidoDespachadoEvent pedidoId={} eventId={}",
                evento.pedidoId(), evento.eventId());

        // Idempotência: não recria entrega já existente para o mesmo pedido.
        if (repository.buscarPorPedido(evento.pedidoId()).isPresent()) {
            log.info("Entrega já existe para pedidoId={}, ignorando.", evento.pedidoId());
            return;
        }

        PedidoDespachadoEvent.EnderecoEntregaDto dto = evento.endereco();
        EnderecoEntrega endereco = new EnderecoEntrega(
                dto.logradouro(), dto.numero(), dto.complemento(),
                dto.bairro(), dto.cidade(), dto.uf(), dto.cep());

        Entrega entrega = new Entrega(UUID.randomUUID(), evento.pedidoId(), endereco);
        entrega.despachar();
        repository.salvar(entrega);

        log.info("Entrega criada e despachada para pedidoId={}", evento.pedidoId());
    }
}
