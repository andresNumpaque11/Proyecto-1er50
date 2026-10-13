package com.sigtam.ms_turnos.repository;

import com.sigtam.ms_turnos.entity.TurnoEntity;
import com.sigtam.ms_turnos.enums.EstadoTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TurnoRepository extends JpaRepository<TurnoEntity, UUID> {

    List<TurnoEntity> findByEstadoOrderByFechaHoraSolicitudAsc(EstadoTurno estado);

    List<TurnoEntity> findByIdPaciente(String idPaciente);
}