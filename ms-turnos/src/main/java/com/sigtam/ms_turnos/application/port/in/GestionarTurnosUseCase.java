package com.sigtam.ms_turnos.application.port.in;

import com.sigtam.ms_turnos.domain.model.Turno;

import java.util.List;
import java.util.UUID;

public interface GestionarTurnosUseCase {
    Turno solicitar(String idPaciente, UUID idDisponibilidad, boolean esUrgente);
    Turno cancelar(UUID idTurno);
    Turno asignarSiguiente();
    List<Turno> consultarPorPaciente(String idPaciente);
}
