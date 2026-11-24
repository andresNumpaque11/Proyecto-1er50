import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxy = (target: string) => ({
    target,
    changeOrigin: true,
    secure: false,
  });

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api/auth": proxy(env.VITE_USERS_SERVICE_URL || "http://localhost:8000"),
        "/api/users": proxy(env.VITE_USERS_SERVICE_URL || "http://localhost:8000"),
        "/api/inventory": proxy(env.VITE_INVENTORY_SERVICE_URL || "http://localhost:7879"),
        "/api/turnos": proxy(env.VITE_TURNOS_SERVICE_URL || "http://localhost:8081"),
        "/api/disponibilidad": proxy(env.VITE_TURNOS_SERVICE_URL || "http://localhost:8081"),
        "/api/v1": proxy(env.VITE_DELIVERY_SERVICE_URL || "http://localhost:8002"),
      },
    },
  };
});
