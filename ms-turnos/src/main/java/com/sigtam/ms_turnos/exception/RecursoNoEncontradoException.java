package com.sigtam.ms_turnos.exception;

/** El recurso solicitado (turno, disponibilidad) no existe -> HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
