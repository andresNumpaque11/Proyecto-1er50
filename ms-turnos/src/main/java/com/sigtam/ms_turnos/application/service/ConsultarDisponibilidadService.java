package com.sigtam.ms_turnos.application.service;

import com.sigtam.ms_turnos.application.port.in.ConsultarDisponibilidadQuery;
import com.sigtam.ms_turnos.application.port.out.DisponibilidadQueryRepositoryPort;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarDisponibilidadService implements ConsultarDisponibilidadQuery {
    private final DisponibilidadQueryRepositoryPort disponibilidadQueryRepository;

    public ConsultarDisponibilidadService(DisponibilidadQueryRepositoryPort disponibilidadQueryRepository) {
        this.disponibilidadQueryRepository = disponibilidadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Disponibilidad> listar() {
        return disponibilidadQueryRepository.findAll();
    }
}
