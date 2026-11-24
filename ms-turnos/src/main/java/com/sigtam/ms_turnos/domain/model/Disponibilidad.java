package com.sigtam.ms_turnos.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Disponibilidad {
    private UUID idDisponibilidad;
    private final String idSede;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private Integer cupo;

    public Disponibilidad(UUID idDisponibilidad, String idSede, LocalDate fecha,
                          LocalTime horaInicio, LocalTime horaFin, Integer cupo) {
        this.idDisponibilidad = idDisponibilidad;
        this.idSede = idSede;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.cupo = cupo;
    }

    public Disponibilidad(String idSede, LocalDate fecha, LocalTime horaInicio,
                          LocalTime horaFin, Integer cupo) {
        this(null, idSede, fecha, horaInicio, horaFin, cupo);
    }

    public UUID getIdDisponibilidad() {
        return idDisponibilidad;
    }

    public void setIdDisponibilidad(UUID idDisponibilidad) {
        this.idDisponibilidad = idDisponibilidad;
    }

    public String getIdSede() {
        return idSede;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void reservarCupo() {
        if (cupo <= 0) {
            throw new IllegalStateException("No hay cupo disponible");
        }
        cupo--;
    }

    public void liberarCupo() {
        cupo++;
    }
}
