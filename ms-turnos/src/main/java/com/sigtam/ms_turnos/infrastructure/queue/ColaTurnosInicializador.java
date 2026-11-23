package com.sigtam.ms_turnos.infrastructure.queue;

import com.sigtam.ms_turnos.application.port.out.TurnoCommandRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.TurnQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ColaTurnosInicializador {
    private static final Logger log = LoggerFactory.getLogger(ColaTurnosInicializador.class);

    private final TurnoCommandRepositoryPort turnoRepository;
    private final TurnQueuePort colaTurnos;

    public ColaTurnosInicializador(TurnoCommandRepositoryPort turnoRepository,
                                   TurnQueuePort colaTurnos) {
        this.turnoRepository = turnoRepository;
        this.colaTurnos = colaTurnos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cargarCola() {
        colaTurnos.clear();
        var pendientes = turnoRepository.findPending();
        colaTurnos.addAll(pendientes);
        log.info("Cola de turnos reconstruida con {} turnos en espera", pendientes.size());
    }
}
