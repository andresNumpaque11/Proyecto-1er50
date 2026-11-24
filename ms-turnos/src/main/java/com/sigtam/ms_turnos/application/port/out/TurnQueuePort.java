package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Turno;

import java.util.Collection;
import java.util.UUID;

public interface TurnQueuePort {
    void add(Turno turno);
    void addAll(Collection<Turno> turnos);
    Turno next();
    boolean remove(UUID idTurno);
    void clear();
}
