package com.sigtam.ms_turnos.application.port.in;

import com.sigtam.ms_turnos.domain.model.Disponibilidad;

import java.util.List;

public interface ConsultarDisponibilidadQuery {
    List<Disponibilidad> listar();
}
