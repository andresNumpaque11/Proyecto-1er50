package com.sigtam.ms_turnos.service;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.dto.TurnoResponseDTO;
import com.sigtam.ms_turnos.entity.DisponibilidadEntity;
import com.sigtam.ms_turnos.entity.TurnoEntity;
import com.sigtam.ms_turnos.enums.EstadoTurno;
import com.sigtam.ms_turnos.exception.ConflictoException;
import com.sigtam.ms_turnos.exception.RecursoNoEncontradoException;
import com.sigtam.ms_turnos.mapper.TurnoMapper;
import com.sigtam.ms_turnos.repository.DisponibilidadRepository;
import com.sigtam.ms_turnos.repository.TurnoRepository;
import com.sigtam.ms_turnos.strategy.EstrategiaPrioridad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestorTurnos {

    private final TurnoRepository turnoRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final TurnoMapper turnoMapper;
    private final ColaTurnos colaTurnos;
    private final List<EstrategiaPrioridad> estrategias;

    @Transactional
    public TurnoResponseDTO solicitarTurno(TurnoRequestDTO request) {
        // Bloqueo de fila: evita que dos solicitudes simultáneas consuman el mismo cupo
        DisponibilidadEntity disponibilidad = disponibilidadRepository
                .findByIdForUpdate(request.idDisponibilidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Disponibilidad no encontrada"));

        if (disponibilidad.getCupo() <= 0) {
            throw new ConflictoException("No hay cupo disponible");
        }

        TurnoEntity turno = turnoMapper.toEntity(request, disponibilidad);

        EstrategiaPrioridad estrategia = seleccionarEstrategia(request);
        turno.setPrioridad(estrategia.calcularPrioridad(request));

        final TurnoEntity guardado = turnoRepository.save(turno);

        disponibilidad.setCupo(disponibilidad.getCupo() - 1);
        disponibilidadRepository.save(disponibilidad);

        // Solo entra a la cola si la transacción termina bien (así no quedan turnos "fantasma")
        despuesDeCommit(() -> colaTurnos.agregar(guardado));

        return turnoMapper.toResponseDTO(guardado);
    }

    @Transactional
    public TurnoResponseDTO cancelarTurno(UUID idTurno) {
        TurnoEntity turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        if (turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new ConflictoException("Solo se pueden cancelar turnos en espera (estado actual: "
                    + turno.getEstado() + ")");
        }

        // Devuelve el cupo a la disponibilidad
        DisponibilidadEntity disponibilidad = disponibilidadRepository
                .findByIdForUpdate(turno.getDisponibilidad().getIdDisponibilidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Disponibilidad no encontrada"));
        disponibilidad.setCupo(disponibilidad.getCupo() + 1);
        disponibilidadRepository.save(disponibilidad);

        turno.setEstado(EstadoTurno.CANCELADO);
        final TurnoEntity guardado = turnoRepository.save(turno);

        // Lo saca de la cola para que /siguiente no lo atienda
        despuesDeCommit(() -> colaTurnos.remover(idTurno));

        return turnoMapper.toResponseDTO(guardado);
    }

    @Transactional
    public TurnoResponseDTO asignarSiguiente() {
        TurnoEntity candidato;
        while ((candidato = colaTurnos.siguiente()) != null) {
            final TurnoEntity extraido = candidato;

            // El objeto de la cola puede estar desactualizado: se vuelve a leer de la BD
            TurnoEntity turno = turnoRepository.findById(extraido.getIdTurno()).orElse(null);
            if (turno == null || turno.getEstado() != EstadoTurno.EN_ESPERA) {
                continue; // cancelado o ya atendido: se descarta y se sigue con el próximo
            }

            // Si la transacción falla, el turno vuelve a la cola
            reencolarSiHayRollback(turno);

            turno.setEstado(EstadoTurno.ATENDIDO);
            return turnoMapper.toResponseDTO(turnoRepository.save(turno));
        }
        throw new ConflictoException("No hay turnos en espera");
    }

    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> consultarPorPaciente(String idPaciente) {
        return turnoRepository.findByIdPaciente(idPaciente)
                .stream()
                .map(turnoMapper::toResponseDTO)
                .toList();
    }

    private EstrategiaPrioridad seleccionarEstrategia(TurnoRequestDTO request) {
        return estrategias.stream()
                .filter(e -> e.aplica(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ninguna estrategia aplica"));
    }

    private void despuesDeCommit(Runnable accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }

    private void reencolarSiHayRollback(TurnoEntity turno) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    colaTurnos.agregar(turno);
                }
            }
        });
    }
}
