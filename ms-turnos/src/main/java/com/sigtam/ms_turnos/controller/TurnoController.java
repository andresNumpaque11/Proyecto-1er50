package com.sigtam.ms_turnos.controller;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.dto.TurnoResponseDTO;
import com.sigtam.ms_turnos.application.port.in.ConsultarTurnosQuery;
import com.sigtam.ms_turnos.application.port.in.GestionarTurnosUseCase;
import com.sigtam.ms_turnos.domain.model.Turno;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import com.sigtam.ms_turnos.infrastructure.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final GestionarTurnosUseCase gestionarTurnos;
    private final ConsultarTurnosQuery consultarTurnos;

    @PostMapping("/solicitar")
    public ResponseEntity<TurnoResponseDTO> solicitarTurno(
            @Valid @RequestBody TurnoRequestDTO request,
            HttpServletRequest servletRequest) {
        AuthenticatedUser user = authenticatedUser(servletRequest);
        if (user == null || !Integer.toString(user.id()).equals(request.idPaciente())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toResponse(gestionarTurnos.solicitar(
                request.idPaciente(), request.idDisponibilidad(), request.esUrgente())));
    }

    @PutMapping("/{idTurno}/cancelar")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(
            @PathVariable UUID idTurno,
            HttpServletRequest servletRequest) {
        AuthenticatedUser user = authenticatedUser(servletRequest);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (user.isPatient()) {
            boolean ownsTurn = consultarTurnos.consultarPorPaciente(Integer.toString(user.id())).stream()
                    .anyMatch(turno -> idTurno.equals(turno.getIdTurno()));
            if (!ownsTurn) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(toResponse(gestionarTurnos.cancelar(idTurno)));
    }

    @PutMapping("/siguiente")
    public ResponseEntity<TurnoResponseDTO> asignarSiguiente() {
        return ResponseEntity.ok(toResponse(gestionarTurnos.asignarSiguiente()));
    }

    @GetMapping("/paciente/{idPaciente}")
    public ResponseEntity<List<TurnoResponseDTO>> consultarTurnosPorPaciente(@PathVariable String idPaciente) {
        return ResponseEntity.ok(consultarTurnos.consultarPorPaciente(idPaciente)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    private TurnoResponseDTO toResponse(Turno turno) {
        return new TurnoResponseDTO(
                turno.getIdTurno(),
                turno.getIdPaciente(),
                turno.getIdDisponibilidad(),
                turno.getFechaHoraSolicitud(),
                turno.getEstado().name(),
                turno.getPrioridad().name());
    }

    private AuthenticatedUser authenticatedUser(HttpServletRequest request) {
        Object user = request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
        return user instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }
}