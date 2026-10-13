package com.sigtam.ms_turnos.repository;

import com.sigtam.ms_turnos.entity.DisponibilidadEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DisponibilidadRepository extends JpaRepository<DisponibilidadEntity, UUID> {

    /**
     * Lee la disponibilidad con bloqueo de escritura (SELECT ... FOR UPDATE) para que
     * dos solicitudes simultáneas no descuenten el mismo cupo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DisponibilidadEntity d where d.idDisponibilidad = :id")
    Optional<DisponibilidadEntity> findByIdForUpdate(@Param("id") UUID id);
}
