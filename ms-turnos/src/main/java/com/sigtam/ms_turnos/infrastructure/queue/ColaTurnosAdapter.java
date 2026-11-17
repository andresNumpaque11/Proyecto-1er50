package com.sigtam.ms_turnos.infrastructure.queue;

import com.sigtam.ms_turnos.application.port.out.TurnQueuePort;
import com.sigtam.ms_turnos.domain.model.TipoPrioridad;
import com.sigtam.ms_turnos.domain.model.Turno;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class ColaTurnosAdapter implements TurnQueuePort {
    private final PriorityBlockingQueue<Turno> cola = new PriorityBlockingQueue<>(
            11,
            Comparator.comparing((Turno turno) ->
                            turno.getPrioridad() == TipoPrioridad.CLINICA ? 0 : 1)
                    .thenComparing(Turno::getFechaHoraSolicitud));

    @Override
    public void add(Turno turno) {
        cola.add(turno);
    }

    @Override
    public void addAll(Collection<Turno> turnos) {
        cola.addAll(turnos);
    }

    @Override
    public Turno next() {
        return cola.poll();
    }

    @Override
    public boolean remove(UUID idTurno) {
        return cola.removeIf(turno -> idTurno.equals(turno.getIdTurno()));
    }

    @Override
    public void clear() {
        cola.clear();
    }
}
