package com.sigtam.ms_turnos.application.port.in;

import com.sigtam.ms_turnos.domain.model.Turno;

import java.util.List;

public interface ConsultarTurnosQuery {
    List<Turno> consultarPorPaciente(String idPaciente);
}
