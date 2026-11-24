package com.sigtam.ms_turnos.infrastructure.messaging;

import com.sigtam.ms_turnos.application.port.out.TurnoEventPublisherPort;
import com.sigtam.ms_turnos.domain.model.Turno;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RabbitTurnoEventPublisherAdapter implements TurnoEventPublisherPort {
    public static final String EXCHANGE = "sigtam.turnos";
    public static final String SOLICITADO_ROUTING_KEY = "turno.solicitado";
    public static final String CANCELADO_ROUTING_KEY = "turno.cancelado";
    public static final String ATENDIDO_ROUTING_KEY = "turno.atendido";

    private final RabbitTemplate rabbitTemplate;

    public RabbitTurnoEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarSolicitado(Turno turno) {
        rabbitTemplate.convertAndSend(EXCHANGE, SOLICITADO_ROUTING_KEY, toEvent(turno));
    }

    @Override
    public void publicarCancelado(Turno turno) {
        rabbitTemplate.convertAndSend(EXCHANGE, CANCELADO_ROUTING_KEY, toEvent(turno));
    }

    @Override
    public void publicarAtendido(Turno turno) {
        rabbitTemplate.convertAndSend(EXCHANGE, ATENDIDO_ROUTING_KEY, toEvent(turno));
    }

    private TurnoEvent toEvent(Turno turno) {
        return new TurnoEvent(
                turno.getIdTurno(),
                turno.getIdPaciente(),
                turno.getIdDisponibilidad(),
                turno.getEstado().name(),
                turno.getPrioridad().name(),
                turno.getFechaHoraSolicitud(),
                LocalDateTime.now());
    }
}
