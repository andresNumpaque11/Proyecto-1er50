package com.sigtam.ms_turnos.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String mensaje, int status, LocalDateTime timestamp) {}