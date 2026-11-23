package com.sigtam.ms_turnos.application;

import com.sigtam.ms_turnos.application.port.out.DisponibilidadQueryRepositoryPort;
import com.sigtam.ms_turnos.application.port.out.TurnoQueryRepositoryPort;
import com.sigtam.ms_turnos.application.service.ConsultarDisponibilidadService;
import com.sigtam.ms_turnos.application.service.ConsultarTurnosService;
import com.sigtam.ms_turnos.domain.model.Disponibilidad;
import com.sigtam.ms_turnos.domain.model.Turno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarQueriesTests {
    @Test
    void consultarTurnosUsaElPuertoDeLectura() {
        TurnoQueryRepositoryPort repository = mock(TurnoQueryRepositoryPort.class);
        ConsultarTurnosService query = new ConsultarTurnosService(repository);
        List<Turno> turnos = List.of();
        when(repository.findByIdPaciente("PAC-1")).thenReturn(turnos);

        assertEquals(turnos, query.consultarPorPaciente("PAC-1"));
        verify(repository).findByIdPaciente("PAC-1");
    }

    @Test
    void listarDisponibilidadesUsaElPuertoDeLectura() {
        DisponibilidadQueryRepositoryPort repository = mock(DisponibilidadQueryRepositoryPort.class);
        ConsultarDisponibilidadService query = new ConsultarDisponibilidadService(repository);
        Disponibilidad disponibilidad = new Disponibilidad(
                "SEDE-1", LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0), 2);
        List<Disponibilidad> disponibilidades = List.of(disponibilidad);
        when(repository.findAll()).thenReturn(disponibilidades);

        assertEquals(disponibilidades, query.listar());
        verify(repository).findAll();
    }
}
