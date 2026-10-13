package com.sigtam.ms_turnos.entity;

import com.sigtam.ms_turnos.enums.EstadoTurno;
import com.sigtam.ms_turnos.enums.TipoPrioridad;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_turno")
    private UUID idTurno;

    @Column(name = "id_paciente", nullable = false)
    private String idPaciente;

    @ManyToOne
    @JoinColumn(name = "id_disponibilidad", nullable = false)
    private DisponibilidadEntity disponibilidad;

    @Column(name = "fecha_hora_solicitud", nullable = false)
    private LocalDateTime fechaHoraSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPrioridad prioridad;
}