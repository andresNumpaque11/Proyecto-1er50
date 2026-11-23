package com.sigtam.ms_turnos.application.service;

import com.sigtam.ms_turnos.application.exception.ConflictoException;
import com.sigtam.ms_turnos.application.exception.RecursoNoEncontradoException;
import com.sigtam.ms_turnos.application.port.in.GestionarTurnosUseCase;
import com.sigtam.ms_turnos.application.port.out.DisponibilidadCommandRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.TransactionPort;
import com.sigtam.ms_turnos.application.port.out.TurnoCommandRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.TurnQueuePort;
import com.sigtam.ms_turnos.application.port.out.TurnoEventPublisherPort;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import com.sigtam.ms_turnos.domain.model.EstadoTurno;
import com.sigtam.ms_turnos.domain.model.TipoPrioridad;
import com.sigtam.ms_turnos.domain.model.Turno;
import com.sigtam.ms_turnos.domain.service.EstrategiaPrioridad;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GestorTurnosService implements GestionarTurnosUseCase {
    private final TurnoCommandRepositoryPort turnoRepository;
    private final DisponibilidadCommandRepositoryPort disponibilidadRepository;
    private final TurnQueuePort colaTurnos;
    private final TransactionPort transaction;
    private final TurnoEventPublisherPort eventPublisher;
    private final List<EstrategiaPrioridad> estrategias;

    public GestorTurnosService(TurnoCommandRepositoryPort turnoRepository,
                               DisponibilidadCommandRepositoryPort disponibilidadRepository,
                               TurnQueuePort colaTurnos,
                               TransactionPort transaction,
                               TurnoEventPublisherPort eventPublisher,
                               List<EstrategiaPrioridad> estrategias) {
        this.turnoRepository = turnoRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.colaTurnos = colaTurnos;
        this.transaction = transaction;
        this.eventPublisher = eventPublisher;
        this.estrategias = estrategias;
    }

    @Override
    @Transactional
    public Turno solicitar(String idPaciente, UUID idDisponibilidad, boolean esUrgente) {
        Disponibilidad disponibilidad = disponibilidadRepository.findByIdForUpdate(idDisponibilidad)
                .orElseThrow(() -> new RecursoNoEncontradoException("Disponibilidad no encontrada"));

        try {
            disponibilidad.reservarCupo();
        } catch (IllegalStateException ex) {
            throw new ConflictoException(ex.getMessage());
        }

        TipoPrioridad prioridad = seleccionarEstrategia(esUrgente)
                .calcularPrioridad(esUrgente);
        Turno turno = turnoRepository.save(
                new Turno(idPaciente, idDisponibilidad, prioridad));
        disponibilidadRepository.save(disponibilidad);

        transaction.afterCommit(() -> colaTurnos.add(turno));
        transaction.afterCommit(() -> eventPublisher.publicarSolicitado(turno));
        return turno;
    }

    @Override
    @Transactional
    public Turno cancelar(UUID idTurno) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        if (turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new ConflictoException("Solo se pueden cancelar turnos en espera (estado actual: "
                    + turno.getEstado() + ")");
        }

        Disponibilidad disponibilidad = disponibilidadRepository
                .findByIdForUpdate(turno.getIdDisponibilidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Disponibilidad no encontrada"));
        disponibilidad.liberarCupo();
        disponibilidadRepository.save(disponibilidad);

        turno.cancelar();
        Turno guardado = turnoRepository.save(turno);
        transaction.afterCommit(() -> colaTurnos.remove(idTurno));
        transaction.afterCommit(() -> eventPublisher.publicarCancelado(guardado));
        return guardado;
    }

    @Override
    @Transactional
    public Turno asignarSiguiente() {
        Turno candidato;
        while ((candidato = colaTurnos.next()) != null) {
            Turno turno = turnoRepository.findById(candidato.getIdTurno()).orElse(null);
            if (turno == null || turno.getEstado() != EstadoTurno.EN_ESPERA) {
                continue;
            }

            transaction.afterRollback(() -> colaTurnos.add(turno));
            turno.atender();
            Turno guardado = turnoRepository.save(turno);
            transaction.afterCommit(() -> eventPublisher.publicarAtendido(guardado));
            return guardado;
        }
        throw new ConflictoException("No hay turnos en espera");
    }

    private EstrategiaPrioridad seleccionarEstrategia(boolean esUrgente) {
        return estrategias.stream()
                .filter(estrategia -> estrategia.aplica(esUrgente))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ninguna estrategia aplica"));
    }
}
