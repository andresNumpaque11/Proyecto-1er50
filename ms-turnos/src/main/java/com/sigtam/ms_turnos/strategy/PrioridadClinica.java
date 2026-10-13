package com.sigtam.ms_turnos.strategy;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.enums.TipoPrioridad;
import org.springframework.stereotype.Component;

@Component
public class PrioridadClinica implements EstrategiaPrioridad {
    @Override
    public boolean aplica(TurnoRequestDTO request) {
        return request.esUrgente();
    }

    @Override
    public TipoPrioridad calcularPrioridad(TurnoRequestDTO request) {
        return TipoPrioridad.CLINICA;
    }
}