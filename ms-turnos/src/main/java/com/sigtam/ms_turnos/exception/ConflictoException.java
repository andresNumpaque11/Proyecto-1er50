package com.sigtam.ms_turnos.exception;

/** La operación no es válida para el estado actual (sin cupo, turno ya atendido, cola vacía) -> HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
