import { FormEvent, useCallback, useEffect, useState } from "react";
import Operations from "./Operations";
import { request } from "./api";

type User = {
  id: number;
  full_name: string;
  email: string;
  role: "admin" | "paciente" | "farmaceutico";
  is_active: boolean;
  created_at: string;
};

type AuthResponse = {
  access_token: string;
  user: User;
};

const roleLabels: Record<User["role"], string> = {
  admin: "Administrador",
  paciente: "Paciente",
  farmaceutico: "Farmacéutico",
};

const TOKEN_KEY = "users-service-token";

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? "");
  const [user, setUser] = useState<User | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [registering, setRegistering] = useState(false);
  const [creatingPharmacist, setCreatingPharmacist] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [activeSection, setActiveSection] = useState("overview");

  const signOut = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken("");
    setUser(null);
    setUsers([]);
  }, []);

  const loadDashboard = useCallback(async (currentToken: string) => {
    try {
      const profile = await request<User>("/api/auth/me", currentToken);
      setUser(profile);
      setActiveSection(profile.role === "admin" ? "users" : profile.role === "farmaceutico" ? "inventory" : "turnos");
      if (profile.role === "admin") {
        setUsers(await request<User[]>("/api/users", currentToken));
      } else {
        setUsers([]);
      }
    } catch (cause) {
      signOut();
      setError(cause instanceof Error ? cause.message : "No se pudo validar la sesión.");
    } finally {
      setLoading(false);
    }
  }, [signOut]);

  useEffect(() => {
    if (token) void loadDashboard(token);
    else setLoading(false);
  }, [loadDashboard, token]);

  async function handleAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    const data = new FormData(event.currentTarget);
    const payload = {
      email: String(data.get("email")),
      password: String(data.get("password")),
    };
    try {
      if (registering) {
        await request<User>("/api/auth/register", undefined, {
          method: "POST",
          body: JSON.stringify({ ...payload, full_name: String(data.get("full_name")) }),
        });
        setNotice("Cuenta creada. Inicia sesión para continuar.");
        setRegistering(false);
      } else {
        const auth = await request<AuthResponse>("/api/auth/login", undefined, {
          method: "POST",
          body: JSON.stringify(payload),
        });
        localStorage.setItem(TOKEN_KEY, auth.access_token);
        setToken(auth.access_token);
        setActiveSection(auth.user.role === "admin" ? "users" : auth.user.role === "farmaceutico" ? "inventory" : "turnos");
        setLoading(true);
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Error de autenticación.");
    }
  }

  async function handleCreatePharmacist(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    const data = new FormData(event.currentTarget);
    try {
      const pharmacist = await request<User>("/api/users/pharmacists", token, {
        method: "POST",
        body: JSON.stringify({
          full_name: String(data.get("full_name")),
          email: String(data.get("email")),
          password: String(data.get("password")),
        }),
      });
      setUsers((existing) => [...existing, pharmacist]);
      setCreatingPharmacist(false);
      setNotice("Cuenta de farmacéutico creada.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo crear la cuenta.");
    }
  }

  async function changeRole(target: User, role: User["role"]) {
    await mutateUser(target.id, "role", { role });
  }

  async function changeStatus(target: User) {
    await mutateUser(target.id, "status", { is_active: !target.is_active });
  }

  async function mutateUser(id: number, action: "role" | "status", body: object) {
    setError("");
    setNotice("");
    try {
      const updated = await request<User>(`/api/users/${id}/${action}`, token, {
        method: "PATCH",
        body: JSON.stringify(body),
      });
      setUsers((existing) => existing.map((item) => item.id === id ? updated : item));
      setNotice("Cambios guardados.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo actualizar el usuario.");
    }
  }

  async function removeUser(target: User) {
    if (!window.confirm(`¿Eliminar la cuenta de ${target.full_name}? Esta acción no se puede deshacer.`)) return;
    setError("");
    try {
      await request<void>(`/api/users/${target.id}`, token, { method: "DELETE" });
      setUsers((existing) => existing.filter((item) => item.id !== target.id));
      setNotice("Usuario eliminado.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo eliminar el usuario.");
    }
  }

  if (loading) return <main className="loading">Cargando tu espacio seguro...</main>;

  if (!user) {
    return (
      <main className="auth-layout">
        <section className="intro">
          <div className="brand"><span className="brand-icon">S</span> SIGTAM</div>
          <div>
            <p className="eyebrow">PLATAFORMA DE ACCESO</p>
            <h1>Una plataforma para coordinar la gestión de medicamentos.</h1>
            <p className="intro-copy">Inicia sesión para consultar inventario, coordinar turnos y gestionar domicilios desde un solo lugar.</p>
          </div>
          <p className="intro-foot">Autenticación protegida con tokens de acceso JWT.</p>
        </section>
        <section className="auth-panel">
          <form className="auth-card" onSubmit={handleAuth}>
            <span className="card-mark">ACCESO SEGURO</span>
            <h2>{registering ? "Crea tu cuenta" : "Qué bueno verte"}</h2>
            <p className="subheading">{registering ? "Regístrate para comenzar." : "Ingresa tus credenciales para continuar."}</p>
            {registering && (
              <label>Nombre completo<input name="full_name" required minLength={2} maxLength={120} autoComplete="name" placeholder="Tu nombre" /></label>
            )}
            <label>Correo electrónico<input name="email" type="email" required autoComplete="email" placeholder="nombre@correo.com" /></label>
            <label>Contraseña<input name="password" type="password" required minLength={registering ? 8 : 1} autoComplete={registering ? "new-password" : "current-password"} placeholder={registering ? "Mínimo 8 caracteres" : "Tu contraseña"} /></label>
            {error && <p className="message error" role="alert">{error}</p>}
            {notice && <p className="message success" role="status">{notice}</p>}
            <button className="primary-button" type="submit">{registering ? "Crear cuenta" : "Iniciar sesión"} <span>→</span></button>
            <p className="switch-auth">{registering ? "¿Ya tienes cuenta?" : "¿Eres nuevo?"}{" "}
              <button type="button" className="text-button" onClick={() => { setRegistering(!registering); setError(""); setNotice(""); }}>
                {registering ? "Inicia sesión" : "Crea una cuenta"}
              </button>
            </p>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard">
      <header className="topbar">
        <a className="brand" href="#" onClick={(event) => { event.preventDefault(); setActiveSection("overview"); }}><span className="brand-icon">S</span> SIGTAM</a>
        <nav className="main-nav" aria-label="Secciones principales">
          <button className={activeSection === "overview" ? "selected" : ""} onClick={() => setActiveSection("overview")}>Inicio</button>
          {user.role === "admin" && <button className={activeSection === "users" ? "selected" : ""} onClick={() => setActiveSection("users")}>Usuarios</button>}
          <button className={activeSection === "inventory" ? "selected" : ""} onClick={() => setActiveSection("inventory")}>Inventario</button>
          <button className={activeSection === "turnos" ? "selected" : ""} onClick={() => setActiveSection("turnos")}>Turnos</button>
          {user.role !== "paciente" && <button className={activeSection === "delivery" ? "selected" : ""} onClick={() => setActiveSection("delivery")}>Domicilios</button>}
        </nav>
        <div className="account"><span className="avatar">{user.full_name[0]?.toUpperCase()}</span><span className="account-name">{user.full_name}<small>{roleLabels[user.role]}</small></span><button className="logout-button" onClick={signOut}>Cerrar sesión</button></div>
      </header>
      <section className="welcome">
        <p className="eyebrow">GESTIÓN DE MEDICAMENTOS</p>
        <h1>{activeSection === "users" ? "Gestión de usuarios" : activeSection === "inventory" ? "Inventario de medicamentos" : activeSection === "turnos" ? "Gestión de turnos" : activeSection === "delivery" ? "Domicilios y rutas" : `Hola, ${user.full_name.split(" ")[0]}`}</h1>
        <p>{activeSection === "users" ? "Administra el acceso, los roles y el estado de las cuentas." : activeSection === "inventory" ? "Consulta existencias, administra lotes y gestiona solicitudes." : activeSection === "turnos" ? "Publica horarios, solicita turnos y coordina la atención." : activeSection === "delivery" ? "Registra pedidos y domiciliarios, y actualiza el estado de las rutas." : "Un espacio de trabajo integrado para los servicios de la plataforma."}</p>
      </section>
      {error && <p className="message error dashboard-message" role="alert">{error}</p>}
      {notice && <p className="message success dashboard-message" role="status">{notice}</p>}
      {activeSection === "overview" ? (
        <div className="overview-grid">
          <section className="profile-card"><span className="avatar profile-avatar">{user.full_name[0]?.toUpperCase()}</span><div><p className="eyebrow">TU PERFIL</p><h2>{user.full_name}</h2><p>{roleLabels[user.role]} · {user.email}</p><span className="status active"><i />Cuenta activa</span></div></section>
          <div className="service-cards">
            <button className="service-card" onClick={() => setActiveSection("inventory")}><span className="service-icon">01</span><span><strong>Inventario</strong><small>Medicamentos, lotes y solicitudes</small></span><b>→</b></button>
            <button className="service-card" onClick={() => setActiveSection("turnos")}><span className="service-icon">02</span><span><strong>Turnos</strong><small>Disponibilidad y atención de pacientes</small></span><b>→</b></button>
            {user.role !== "paciente" && <button className="service-card" onClick={() => setActiveSection("delivery")}><span className="service-icon">03</span><span><strong>Domicilios</strong><small>Pedidos, repartidores y rutas</small></span><b>→</b></button>}
            {user.role === "admin" && <button className="service-card" onClick={() => setActiveSection("users")}><span className="service-icon">04</span><span><strong>Usuarios</strong><small>Cuentas y permisos de acceso</small></span><b>→</b></button>}
          </div>
        </div>
      ) : activeSection === "users" && user.role === "admin" ? (
        <section className="user-card">
          <div className="section-heading">
            <div><h2>Cuentas registradas</h2><p>{users.length} {users.length === 1 ? "cuenta" : "cuentas"} en el sistema</p></div>
            <div className="section-actions"><span className="count-badge">{users.length}</span><button className="primary-button create-pharmacist-button" onClick={() => { setCreatingPharmacist(!creatingPharmacist); setError(""); setNotice(""); }}>{creatingPharmacist ? "Cancelar" : "Crear farmacéutico"}</button></div>
          </div>
          {creatingPharmacist && (
            <form className="pharmacist-form" onSubmit={handleCreatePharmacist}>
              <label>Nombre completo<input name="full_name" required minLength={2} maxLength={120} autoComplete="name" /></label>
              <label>Correo electrónico<input name="email" type="email" required autoComplete="email" /></label>
              <label>Contraseña inicial<input name="password" type="password" required minLength={8} maxLength={128} autoComplete="new-password" /></label>
              <button className="primary-button" type="submit">Crear cuenta farmacéutico</button>
            </form>
          )}
          <div className="table-wrap">
            <table><thead><tr><th>USUARIO</th><th>ROL</th><th>ESTADO</th><th>REGISTRO</th><th>ACCIONES</th></tr></thead>
              <tbody>{users.map((item) => (
                <tr key={item.id}>
                  <td><div className="user-cell"><span className="avatar table-avatar">{item.full_name[0]?.toUpperCase()}</span><span>{item.full_name}<small>{item.email}</small></span></div></td>
                  <td><span className={`role-pill ${item.role}`}>{roleLabels[item.role]}</span></td>
                  <td><span className={`status ${item.is_active ? "active" : "inactive"}`}><i />{item.is_active ? "Activo" : "Inactivo"}</span></td>
                  <td>{new Date(item.created_at).toLocaleDateString("es-CO")}</td>
                  <td className="actions">
                    <select aria-label={`Rol de ${item.full_name}`} value={item.role} onChange={(event) => void changeRole(item, event.target.value as User["role"])}>
                      <option value="paciente">Paciente</option><option value="farmaceutico">Farmacéutico</option><option value="admin">Administrador</option>
                    </select>
                    <button className="action-button" onClick={() => void changeStatus(item)}>{item.is_active ? "Desactivar" : "Activar"}</button>
                    <button className="delete-button" aria-label={`Eliminar a ${item.full_name}`} onClick={() => void removeUser(item)}>Eliminar</button>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </section>
      ) : activeSection === "inventory" || activeSection === "turnos" || activeSection === "delivery" ? (
        <Operations section={activeSection} user={user} />
      ) : <p className="message error">No tienes permisos para acceder a esta sección.</p>}
      <footer className="footer">Gestión integrada de medicamentos <span>•</span> Sesión protegida con JWT</footer>
    </main>
  );
}
