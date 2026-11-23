package com.sigtam.ms_turnos.infrastructure.persistence.repository;

import com.sigtam.ms_turnos.domain.model.EstadoTurno;
import com.sigtam.ms_turnos.infrastructure.persistence.entity.TurnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringTurnoRepository extends JpaRepository<TurnoEntity, UUID> {
    List<TurnoEntity> findByEstadoOrderByFechaHoraSolicitudAsc(EstadoTurno estado);
    List<TurnoEntity> findByIdPaciente(String idPaciente);
}
