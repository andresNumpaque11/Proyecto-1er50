package com.sigtam.ms_turnos.service;

import com.sigtam.ms_turnos.dto.DisponibilidadDTO;
import com.sigtam.ms_turnos.dto.DisponibilidadRequestDTO;
import com.sigtam.ms_turnos.entity.DisponibilidadEntity;
import com.sigtam.ms_turnos.mapper.DisponibilidadMapper;
import com.sigtam.ms_turnos.repository.DisponibilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final DisponibilidadMapper disponibilidadMapper;

    public DisponibilidadDTO crear(DisponibilidadRequestDTO request) {
        DisponibilidadEntity entity = disponibilidadMapper.toEntity(request);
        entity = disponibilidadRepository.save(entity);
        return disponibilidadMapper.toDTO(entity);
    }

    public List<DisponibilidadDTO> listar() {
        return disponibilidadRepository.findAll()
                .stream()
                .map(disponibilidadMapper::toDTO)
                .toList();
    }
}