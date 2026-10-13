package com.sigtam.ms_turnos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TurnoRequestDTO(
        @NotBlank(message = "idPaciente es obligatorio") String idPaciente,
        @NotNull(message = "idDisponibilidad es obligatorio") UUID idDisponibilidad,
        boolean esUrgente
) {}