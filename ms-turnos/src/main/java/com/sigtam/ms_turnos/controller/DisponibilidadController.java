package com.sigtam.ms_turnos.controller;

import com.sigtam.ms_turnos.dto.DisponibilidadDTO;
import com.sigtam.ms_turnos.dto.DisponibilidadRequestDTO;
import com.sigtam.ms_turnos.application.port.in.ConsultarDisponibilidadQuery;
import com.sigtam.ms_turnos.application.port.in.GestionarDisponibilidadUseCase;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final GestionarDisponibilidadUseCase gestionarDisponibilidad;
    private final ConsultarDisponibilidadQuery consultarDisponibilidad;

    @PostMapping
    public ResponseEntity<DisponibilidadDTO> crear(@Valid @RequestBody DisponibilidadRequestDTO request) {
        Disponibilidad disponibilidad = gestionarDisponibilidad.crear(
                request.idSede(), request.fecha(), request.horaInicio(),
                request.horaFin(), request.cupo());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(disponibilidad));
    }

    @GetMapping
    public ResponseEntity<List<DisponibilidadDTO>> listar() {
        return ResponseEntity.ok(consultarDisponibilidad.listar()
                .stream()
                .map(this::toDTO)
                .toList());
    }

    private DisponibilidadDTO toDTO(Disponibilidad disponibilidad) {
        return new DisponibilidadDTO(
                disponibilidad.getIdDisponibilidad(),
                disponibilidad.getIdSede(),
                disponibilidad.getFecha(),
                disponibilidad.getHoraInicio(),
                disponibilidad.getHoraFin(),
                disponibilidad.getCupo());
    }
}