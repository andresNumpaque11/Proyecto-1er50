package com.sigtam.ms_turnos.controller;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.dto.TurnoResponseDTO;
import com.sigtam.ms_turnos.service.GestorTurnos;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final GestorTurnos gestorTurnos;

        @PostMapping("/solicitar")
    public ResponseEntity<TurnoResponseDTO> solicitarTurno(@Valid @RequestBody TurnoRequestDTO request) {
    return ResponseEntity.ok(gestorTurnos.solicitarTurno(request));
}

    @PutMapping("/{idTurno}/cancelar")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(@PathVariable UUID idTurno) {
        return ResponseEntity.ok(gestorTurnos.cancelarTurno(idTurno));
    }

    @PutMapping("/siguiente")
    public ResponseEntity<TurnoResponseDTO> asignarSiguiente() {
        return ResponseEntity.ok(gestorTurnos.asignarSiguiente());
    }

    @GetMapping("/paciente/{idPaciente}")
    public ResponseEntity<List<TurnoResponseDTO>> consultarTurnosPorPaciente(@PathVariable String idPaciente) {
        return ResponseEntity.ok(gestorTurnos.consultarPorPaciente(idPaciente));
    }
}