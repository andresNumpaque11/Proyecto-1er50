package com.sigtam.ms_turnos.service;

import com.sigtam.ms_turnos.entity.TurnoEntity;
import com.sigtam.ms_turnos.enums.TipoPrioridad;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Cola de prioridad de turnos en espera (CLINICA primero, luego por orden de solicitud).
 * Es thread-safe. La fuente de verdad es la base de datos: esta cola se reconstruye
 * al arrancar el servicio (ver ColaTurnosInicializador).
 */
@Component
public class ColaTurnos {

    private final PriorityBlockingQueue<TurnoEntity> cola = new PriorityBlockingQueue<>(
            11,
            Comparator.comparing((TurnoEntity t) -> t.getPrioridad() == TipoPrioridad.CLINICA ? 0 : 1)
                      .thenComparing(TurnoEntity::getFechaHoraSolicitud)
    );

    public void agregar(TurnoEntity turno) {
        cola.add(turno);
    }

    public void agregarTodos(Collection<TurnoEntity> turnos) {
        cola.addAll(turnos);
    }

    public TurnoEntity siguiente() {
        return cola.poll();
    }

    public boolean remover(UUID idTurno) {
        return cola.removeIf(t -> idTurno.equals(t.getIdTurno()));
    }

    public void vaciar() {
        cola.clear();
    }

    public int capacidad() {
        return cola.size();
    }
}
