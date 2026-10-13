package com.sigtam.ms_turnos.controller;

import com.sigtam.ms_turnos.dto.DisponibilidadDTO;
import com.sigtam.ms_turnos.dto.DisponibilidadRequestDTO;
import com.sigtam.ms_turnos.service.DisponibilidadService;
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

    private final DisponibilidadService disponibilidadService;

    @PostMapping
    public ResponseEntity<DisponibilidadDTO> crear(@Valid @RequestBody DisponibilidadRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibilidadService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<DisponibilidadDTO>> listar() {
        return ResponseEntity.ok(disponibilidadService.listar());
    }
}