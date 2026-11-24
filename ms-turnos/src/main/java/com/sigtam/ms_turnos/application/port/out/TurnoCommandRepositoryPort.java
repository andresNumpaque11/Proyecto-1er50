package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Turno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TurnoCommandRepositoryPort {
    Turno save(Turno turno);
    Optional<Turno> findById(UUID idTurno);
    List<Turno> findPending();
}
