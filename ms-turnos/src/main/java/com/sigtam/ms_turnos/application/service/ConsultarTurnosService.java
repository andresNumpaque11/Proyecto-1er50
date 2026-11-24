package com.sigtam.ms_turnos.application.service;

import com.sigtam.ms_turnos.application.port.in.ConsultarTurnosQuery;
import com.sigtam.ms_turnos.application.port.out.TurnoQueryRepositoryPort;
import com.sigtam.ms_turnos.domain.model.Turno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarTurnosService implements ConsultarTurnosQuery {
    private final TurnoQueryRepositoryPort turnoQueryRepository;

    public ConsultarTurnosService(TurnoQueryRepositoryPort turnoQueryRepository) {
        this.turnoQueryRepository = turnoQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> consultarPorPaciente(String idPaciente) {
        return turnoQueryRepository.findByIdPaciente(idPaciente);
    }
}
