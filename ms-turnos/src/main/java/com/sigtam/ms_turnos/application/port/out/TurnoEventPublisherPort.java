package com.sigtam.ms_turnos.application.port.out;

import com.sigtam.ms_turnos.domain.model.Turno;

public interface TurnoEventPublisherPort {
    void publicarSolicitado(Turno turno);

    void publicarCancelado(Turno turno);

    void publicarAtendido(Turno turno);
}
