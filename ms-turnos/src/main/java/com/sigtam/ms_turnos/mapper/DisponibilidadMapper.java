package com.sigtam.ms_turnos.mapper;

import com.sigtam.ms_turnos.dto.DisponibilidadDTO;
import com.sigtam.ms_turnos.dto.DisponibilidadRequestDTO;
import com.sigtam.ms_turnos.entity.DisponibilidadEntity;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadMapper {

    public DisponibilidadDTO toDTO(DisponibilidadEntity entity) {
        return new DisponibilidadDTO(
                entity.getIdDisponibilidad(),
                entity.getIdSede(),
                entity.getFecha(),
                entity.getHoraInicio(),
                entity.getHoraFin(),
                entity.getCupo());
    }

    public DisponibilidadEntity toEntity(DisponibilidadRequestDTO dto) {
        DisponibilidadEntity entity = new DisponibilidadEntity();
        entity.setIdSede(dto.idSede());
        entity.setFecha(dto.fecha());
        entity.setHoraInicio(dto.horaInicio());
        entity.setHoraFin(dto.horaFin());
        entity.setCupo(dto.cupo());
        return entity;
    }
}