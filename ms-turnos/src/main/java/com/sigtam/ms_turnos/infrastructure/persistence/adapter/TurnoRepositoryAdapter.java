package com.sigtam.ms_turnos.infrastructure.persistence.adapter;

import com.sigtam.ms_turnos.application.port.out.TurnoRepositoryPort;
import com.sigtam.ms_turnos.domain.model.EstadoTurno;
import com.sigtam.ms_turnos.domain.model.Turno;
import com.sigtam.ms_turnos.infrastructure.persistence.entity.DisponibilidadEntity;
import com.sigtam.ms_turnos.infrastructure.persistence.entity.TurnoEntity;
import com.sigtam.ms_turnos.infrastructure.persistence.repository.SpringDisponibilidadRepository;
import com.sigtam.ms_turnos.infrastructure.persistence.repository.SpringTurnoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TurnoRepositoryAdapter implements TurnoRepositoryPort {
    private final SpringTurnoRepository turnoRepository;
    private final SpringDisponibilidadRepository disponibilidadRepository;

    public TurnoRepositoryAdapter(SpringTurnoRepository turnoRepository,
                                  SpringDisponibilidadRepository disponibilidadRepository) {
        this.turnoRepository = turnoRepository;
        this.disponibilidadRepository = disponibilidadRepository;
    }

    @Override
    public Turno save(Turno turno) {
        DisponibilidadEntity disponibilidad = disponibilidadRepository
                .findById(turno.getIdDisponibilidad())
                .orElseThrow(() -> new IllegalArgumentException("Disponibilidad no encontrada"));
        TurnoEntity entity = turno.getIdTurno() == null
                ? new TurnoEntity()
                : turnoRepository.findById(turno.getIdTurno()).orElse(new TurnoEntity());
        entity.setIdPaciente(turno.getIdPaciente());
        entity.setDisponibilidad(disponibilidad);
        entity.setFechaHoraSolicitud(turno.getFechaHoraSolicitud());
        entity.setEstado(turno.getEstado());
        entity.setPrioridad(turno.getPrioridad());
        return toDomain(turnoRepository.save(entity));
    }

    @Override
    public Optional<Turno> findById(UUID idTurno) {
        return turnoRepository.findById(idTurno).map(this::toDomain);
    }

    @Override
    public List<Turno> findByIdPaciente(String idPaciente) {
        return turnoRepository.findByIdPaciente(idPaciente).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Turno> findPending() {
        return turnoRepository.findByEstadoOrderByFechaHoraSolicitudAsc(EstadoTurno.EN_ESPERA)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Turno toDomain(TurnoEntity entity) {
        return new Turno(
                entity.getIdTurno(),
                entity.getIdPaciente(),
                entity.getDisponibilidad().getIdDisponibilidad(),
                entity.getFechaHoraSolicitud(),
                entity.getEstado(),
                entity.getPrioridad());
    }
}
