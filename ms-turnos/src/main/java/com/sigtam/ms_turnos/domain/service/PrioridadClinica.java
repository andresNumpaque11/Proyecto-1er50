package com.sigtam.ms_turnos.domain.service;

import com.sigtam.ms_turnos.domain.model.TipoPrioridad;
import org.springframework.stereotype.Component;

@Component
public class PrioridadClinica implements EstrategiaPrioridad {
    @Override
    public boolean aplica(boolean esUrgente) {
        return esUrgente;
    }

    @Override
    public TipoPrioridad calcularPrioridad(boolean esUrgente) {
        return TipoPrioridad.CLINICA;
    }
}
