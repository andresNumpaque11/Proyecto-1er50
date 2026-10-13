package com.sigtam.ms_turnos.strategy;

import com.sigtam.ms_turnos.dto.TurnoRequestDTO;
import com.sigtam.ms_turnos.enums.TipoPrioridad;

public interface EstrategiaPrioridad {
    boolean aplica(TurnoRequestDTO request);
    TipoPrioridad calcularPrioridad(TurnoRequestDTO request);
}