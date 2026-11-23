package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Disponibilidad;

import java.util.List;

public interface DisponibilidadQueryRepositoryPort {
    List<Disponibilidad> findAll();
}
