package com.sigtam.ms_turnos.service;

import com.sigtam.ms_turnos.enums.EstadoTurno;
import com.sigtam.ms_turnos.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** Al arrancar, recarga en la cola los turnos que quedaron EN_ESPERA en la base de datos. */
@Component
@RequiredArgsConstructor
public class ColaTurnosInicializador {

    private static final Logger log = LoggerFactory.getLogger(ColaTurnosInicializador.class);

    private final TurnoRepository turnoRepository;
    private final ColaTurnos colaTurnos;

    @EventListener(ApplicationReadyEvent.class)
    public void cargarCola() {
        colaTurnos.vaciar();
        List<com.sigtam.ms_turnos.entity.TurnoEntity> pendientes =
                turnoRepository.findByEstadoOrderByFechaHoraSolicitudAsc(EstadoTurno.EN_ESPERA);
        colaTurnos.agregarTodos(pendientes);
        log.info("Cola de turnos reconstruida con {} turnos en espera", pendientes.size());
    }
}
