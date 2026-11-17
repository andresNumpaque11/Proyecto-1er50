package com.sigtam.ms_turnos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Turno {
    private UUID idTurno;
    private final String idPaciente;
    private final UUID idDisponibilidad;
    private final LocalDateTime fechaHoraSolicitud;
    private EstadoTurno estado;
    private TipoPrioridad prioridad;

    public Turno(UUID idTurno, String idPaciente, UUID idDisponibilidad,
                 LocalDateTime fechaHoraSolicitud, EstadoTurno estado,
                 TipoPrioridad prioridad) {
        this.idTurno = idTurno;
        this.idPaciente = idPaciente;
        this.idDisponibilidad = idDisponibilidad;
        this.fechaHoraSolicitud = fechaHoraSolicitud;
        this.estado = estado;
        this.prioridad = prioridad;
    }

    public Turno(String idPaciente, UUID idDisponibilidad, TipoPrioridad prioridad) {
        this(null, idPaciente, idDisponibilidad, LocalDateTime.now(),
                EstadoTurno.EN_ESPERA, prioridad);
    }

    public UUID getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(UUID idTurno) {
        this.idTurno = idTurno;
    }

    public String getIdPaciente() {
        return idPaciente;
    }

    public UUID getIdDisponibilidad() {
        return idDisponibilidad;
    }

    public LocalDateTime getFechaHoraSolicitud() {
        return fechaHoraSolicitud;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public void cancelar() {
        estado = EstadoTurno.CANCELADO;
    }

    public void atender() {
        estado = EstadoTurno.ATENDIDO;
    }

    public TipoPrioridad getPrioridad() {
        return prioridad;
    }
}
