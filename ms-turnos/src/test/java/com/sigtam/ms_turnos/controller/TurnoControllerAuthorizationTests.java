package com.sigtam.ms_turnos.controller;

import com.sigtam.ms_turnos.application.port.in.ConsultarTurnosQuery;
import com.sigtam.ms_turnos.application.port.in.GestionarTurnosUseCase;
import com.sigtam.ms_turnos.domain.model.Turno;
import com.sigtam.ms_turnos.infrastructure.security.AuthenticatedUser;
import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TurnoControllerAuthorizationTests {
    @Test
    void patientCannotRequestTurnForAnotherPatient() {
        GestionarTurnosUseCase commands = mock(GestionarTurnosUseCase.class);
        ConsultarTurnosQuery queries = mock(ConsultarTurnosQuery.class);
        TurnoController controller = new TurnoController(commands, queries);
        HttpServletRequest request = authenticatedPatientRequest();

        var response = controller.solicitarTurno(
                new TurnoRequestDTO("43", UUID.randomUUID(), false), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(commands);
    }

    @Test
    void patientCannotCancelAnotherPatientsTurn() {
        GestionarTurnosUseCase commands = mock(GestionarTurnosUseCase.class);
        ConsultarTurnosQuery queries = mock(ConsultarTurnosQuery.class);
        TurnoController controller = new TurnoController(commands, queries);
        UUID turnId = UUID.randomUUID();
        when(queries.consultarPorPaciente("42")).thenReturn(List.<Turno>of());

        var response = controller.cancelarTurno(turnId, authenticatedPatientRequest());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(commands);
    }

    private MockHttpServletRequest authenticatedPatientRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                AuthenticatedUser.REQUEST_ATTRIBUTE,
                new AuthenticatedUser(42, "paciente"));
        return request;
    }
}
