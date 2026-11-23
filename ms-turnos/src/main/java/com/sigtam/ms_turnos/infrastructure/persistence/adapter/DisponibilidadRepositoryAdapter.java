package com.sigtam.ms_turnos.infrastructure.persistence.adapter;

import com.sigtam.ms_turnos.application.port.out.DisponibilidadCommandRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.DisponibilidadQueryRepositoryPort;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import com.sigtam.ms_turnos.infrastructure.persistence.entity.DisponibilidadEntity;
import com.sigtam.ms_turnos.infrastructure.persistence.repository.SpringDisponibilidadRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DisponibilidadRepositoryAdapter
        implements DisponibilidadCommandRepositoryPort, DisponibilidadQueryRepositoryPort {
    private final SpringDisponibilidadRepository repository;

    public DisponibilidadRepositoryAdapter(SpringDisponibilidadRepository repository) {
        this.repository = repository;
    }

    @Override
    public Disponibilidad save(Disponibilidad disponibilidad) {
        DisponibilidadEntity entity = toEntity(disponibilidad);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Disponibilidad> findByIdForUpdate(UUID idDisponibilidad) {
        return repository.findByIdForUpdate(idDisponibilidad).map(this::toDomain);
    }

    @Override
    public List<Disponibilidad> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private DisponibilidadEntity toEntity(Disponibilidad disponibilidad) {
        return new DisponibilidadEntity(
                disponibilidad.getIdDisponibilidad(),
                disponibilidad.getIdSede(),
                disponibilidad.getFecha(),
                disponibilidad.getHoraInicio(),
                disponibilidad.getHoraFin(),
                disponibilidad.getCupo());
    }

    private Disponibilidad toDomain(DisponibilidadEntity entity) {
        return new Disponibilidad(
                entity.getIdDisponibilidad(),
                entity.getIdSede(),
                entity.getFecha(),
                entity.getHoraInicio(),
                entity.getHoraFin(),
                entity.getCupo());
    }
}
