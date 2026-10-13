package com.sigtam.ms_turnos.mapper;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.dto.TurnoResponseDTO;
import com.sigtam.ms_turnos.entity.DisponibilidadEntity;
import com.sigtam.ms_turnos.entity.TurnoEntity;
import com.sigtam.ms_turnos.enums.EstadoTurno;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TurnoMapper {

    public TurnoResponseDTO toResponseDTO(TurnoEntity entity) {
        return new TurnoResponseDTO(
                entity.getIdTurno(),
                entity.getIdPaciente(),
                entity.getDisponibilidad().getIdDisponibilidad(),
                entity.getFechaHoraSolicitud(),
                entity.getEstado().name(),
                entity.getPrioridad().name()
        );
    }

    public TurnoEntity toEntity(TurnoRequestDTO dto, DisponibilidadEntity disponibilidad) {
        TurnoEntity entity = new TurnoEntity();
        entity.setIdPaciente(dto.idPaciente());
        entity.setDisponibilidad(disponibilidad);
        entity.setFechaHoraSolicitud(LocalDateTime.now());
        entity.setEstado(EstadoTurno.EN_ESPERA);
        return entity;
    }
}