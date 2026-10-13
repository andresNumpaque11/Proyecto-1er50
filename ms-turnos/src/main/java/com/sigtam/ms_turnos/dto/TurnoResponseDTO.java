package com.sigtam.ms_turnos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TurnoResponseDTO(
        UUID idTurno,
        String idPaciente,
        UUID idDisponibilidad,
        LocalDateTime fechaHoraSolicitud,
        String estado,
        String prioridad
) {}