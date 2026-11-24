package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Disponibilidad;

import java.util.Optional;
import java.util.UUID;

public interface DisponibilidadCommandRepositoryPort {
    Disponibilidad save(Disponibilidad disponibilidad);
    Optional<Disponibilidad> findByIdForUpdate(UUID idDisponibilidad);
}
