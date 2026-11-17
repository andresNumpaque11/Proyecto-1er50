package com.sigtam.ms_turnos.domain.service;

import com.sigtam.ms_turnos.domain.model.TipoPrioridad;

public interface EstrategiaPrioridad {
    boolean aplica(boolean esUrgente);
    TipoPrioridad calcularPrioridad(boolean esUrgente);
}
