package com.sigtam.ms_turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadDTO(
        UUID idDisponibilidad,
        String idSede,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupo
) {}