package com.sigtam.ms_turnos.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record DisponibilidadRequestDTO(
        @NotBlank(message = "idSede es obligatorio") String idSede,
        @NotNull(message = "fecha es obligatoria") @FutureOrPresent(message = "fecha no puede ser pasada") LocalDate fecha,
        @NotNull(message = "horaInicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "horaFin es obligatoria") LocalTime horaFin,
        @NotNull(message = "cupo es obligatorio") @Positive(message = "cupo debe ser mayor a 0") Integer cupo
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "horaFin debe ser posterior a horaInicio")
    public boolean isHorarioValido() {
        // Si falta alguna hora, el error ya lo reportan los @NotNull
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
