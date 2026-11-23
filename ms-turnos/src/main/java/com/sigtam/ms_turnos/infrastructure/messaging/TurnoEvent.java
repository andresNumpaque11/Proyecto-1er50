package com.sigtam.ms_turnos.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

public record TurnoEvent(
        UUID idTurno,
        String idPaciente,
        UUID idDisponibilidad,
        String estado,
        String prioridad,
        LocalDateTime fechaHoraSolicitud,
        LocalDateTime fechaEvento
) {}
