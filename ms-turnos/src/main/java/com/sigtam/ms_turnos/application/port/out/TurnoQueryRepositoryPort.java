package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Turno;

import java.util.List;

public interface TurnoQueryRepositoryPort {
    List<Turno> findByIdPaciente(String idPaciente);
}
