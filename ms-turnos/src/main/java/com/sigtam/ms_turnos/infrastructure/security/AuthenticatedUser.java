package com.sigtam.ms_turnos.infrastructure.security;

public record AuthenticatedUser(int id, String role) {
    public static final String REQUEST_ATTRIBUTE = AuthenticatedUser.class.getName();

    public boolean isStaff() {
        return "admin".equals(role) || "farmaceutico".equals(role);
    }

    public boolean isPatient() {
        return "paciente".equals(role);
    }
}
