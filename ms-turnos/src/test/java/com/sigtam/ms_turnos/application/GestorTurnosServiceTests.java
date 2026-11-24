package com.sigtam.ms_turnos.application;

import com.sigtam.ms_turnos.application.exception.ConflictoException;
import com.sigtam.ms_turnos.application.port.out.DisponibilidadCommandRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.TransactionPort;
import com.sigtam.ms_turnos.application.port.out.TurnQueuePort;
import com.sigtam.ms_turnos.application.port.out.TurnoEventPublisherPort;
import com.sigtam.ms_turnos.application.port.out.TurnoCommandRepositoryPort;
import com.sigtam.ms_turnos.application.service.GestorTurnosService;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import com.sigtam.ms_turnos.domain.model.EstadoTurno;
import com.sigtam.ms_turnos.domain.model.TipoPrioridad;
import com.sigtam.ms_turnos.domain.model.Turno;
import com.sigtam.ms_turnos.domain.service.EstrategiaPrioridad;
import com.sigtam.ms_turnos.domain.service.PrioridadNormal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class GestorTurnosServiceTests {
    private TurnoCommandRepositoryPort turnoRepository;
    private DisponibilidadCommandRepositoryPort disponibilidadRepository;
    private TurnQueuePort queue;
    private TransactionPort transaction;
    private TurnoEventPublisherPort events;
    private GestorTurnosService service;

    @BeforeEach
    void setUp() {
        turnoRepository = mock(TurnoCommandRepositoryPort.class);
        disponibilidadRepository = mock(DisponibilidadCommandRepositoryPort.class);
        queue = mock(TurnQueuePort.class);
        transaction = mock(TransactionPort.class);
        events = mock(TurnoEventPublisherPort.class);
        EstrategiaPrioridad normal = new PrioridadNormal();
        service = new GestorTurnosService(
                turnoRepository, disponibilidadRepository, queue,
                transaction, events, List.of(normal));
    }

    @Test
    void solicitarTurnoNormalReduceCupoYPublicaEventoDespuesDelCommit() {
        UUID disponibilidadId = UUID.randomUUID();
        Disponibilidad disponibilidad = new Disponibilidad(
                UUID.randomUUID(), "SEDE-1", LocalDate.now().plusDays(1),
                LocalTime.of(8, 0), LocalTime.of(9, 0), 1);

        when(disponibilidadRepository.findByIdForUpdate(disponibilidadId))
                .thenReturn(Optional.of(disponibilidad));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno turno = invocation.getArgument(0);
            turno.setIdTurno(UUID.randomUUID());
            return turno;
        });

        Turno resultado = service.solicitar("PAC-1", disponibilidadId, false);

        assertEquals(0, disponibilidad.getCupo());
        assertEquals(TipoPrioridad.NORMAL, resultado.getPrioridad());
        assertEquals(EstadoTurno.EN_ESPERA, resultado.getEstado());
        verify(disponibilidadRepository).save(disponibilidad);
        verify(transaction, times(2)).afterCommit(any(Runnable.class));
    }

    @Test
    void solicitarTurnoSinCupoProduceConflicto() {
        UUID disponibilidadId = UUID.randomUUID();
        Disponibilidad disponibilidad = new Disponibilidad(
                UUID.randomUUID(), "SEDE-1", LocalDate.now().plusDays(1),
                LocalTime.of(8, 0), LocalTime.of(9, 0), 0);

        when(disponibilidadRepository.findByIdForUpdate(disponibilidadId))
                .thenReturn(Optional.of(disponibilidad));

        assertThrows(ConflictoException.class,
                () -> service.solicitar("PAC-1", disponibilidadId, false));
    }
}
