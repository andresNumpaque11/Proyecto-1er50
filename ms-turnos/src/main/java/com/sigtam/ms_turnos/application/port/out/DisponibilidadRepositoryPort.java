package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Disponibilidad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisponibilidadRepositoryPort {
    Disponibilidad save(Disponibilidad disponibilidad);
    Optional<Disponibilidad> findByIdForUpdate(UUID idDisponibilidad);
    List<Disponibilidad> findAll();
}
