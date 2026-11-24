package com.sigtam.ms_turnos.application.service;

import com.sigtam.ms_turnos.application.port.in.GestionarDisponibilidadUseCase;
import com.sigtam.ms_turnos.application.port.out.DisponibilidadCommandRepositoryPort;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class GestionarDisponibilidadService implements GestionarDisponibilidadUseCase {
    private final DisponibilidadCommandRepositoryPort disponibilidadRepository;

    public GestionarDisponibilidadService(DisponibilidadCommandRepositoryPort disponibilidadRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
    }

    @Override
    public Disponibilidad crear(String idSede, LocalDate fecha, LocalTime horaInicio,
                                LocalTime horaFin, Integer cupo) {
        return disponibilidadRepository.save(
                new Disponibilidad(idSede, fecha, horaInicio, horaFin, cupo));
    }

}
