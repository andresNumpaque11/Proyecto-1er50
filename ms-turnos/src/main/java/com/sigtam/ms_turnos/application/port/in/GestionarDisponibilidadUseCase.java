package com.sigtam.ms_turnos.application.port.in;

import com.sigtam.ms_turnos.domain.model.Disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GestionarDisponibilidadUseCase {
    Disponibilidad crear(String idSede, LocalDate fecha, LocalTime horaInicio,
                         LocalTime horaFin, Integer cupo);
    List<Disponibilidad> listar();
}
