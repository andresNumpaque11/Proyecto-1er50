package com.sigtam.ms_turnos.infrastructure.persistence.repository;

import com.sigtam.ms_turnos.infrastructure.persistence.entity.DisponibilidadEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDisponibilidadRepository extends JpaRepository<DisponibilidadEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DisponibilidadEntity d where d.idDisponibilidad = :id")
    Optional<DisponibilidadEntity> findByIdForUpdate(@Param("id") UUID id);
}
