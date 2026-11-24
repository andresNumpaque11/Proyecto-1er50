import { FormEvent, useCallback, useEffect, useState } from "react";
import { request } from "./api";

type Role = "admin" | "paciente" | "farmaceutico";
type WorkspaceUser = { id: number; full_name: string; role: Role };
type Section = "inventory" | "turnos" | "delivery";

type Medication = {
  _id: string;
  name: string;
  activeIngredient: string;
  presentation: string;
  concentration: string;
  active: boolean;
};

type Lot = {
  _id: string;
  medication: Medication | string;
  lotCode: string;
  quantity: number;
  minimumStock: number;
  expirationDate: string;
};

type MedicationRequest = {
  _id: string;
  medication: Medication | string;
  quantity: number;
  requestedBy: string;
  status: string;
};

type Turno = {
  idTurno: string;
  idPaciente: string;
  idDisponibilidad: string;
  fechaHoraSolicitud: string;
  estado: string;
  prioridad: string;
};

type Disponibilidad = {
  idDisponibilidad: string;
  idSede: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  cupo: number;
};

type DeliveryOrder = {
  order_id: string;
  status: string;
  route_id: string | null;
};

type Driver = {
  driver_id: string;
  name: string;
  status: string;
  current_route_id: string | null;
};

type Route = {
  route_id: string;
  city: string;
  status: string;
  driver_id: string | null;
  stops: { order_id: string; sequence: number; completed: boolean }[];
};

const staffRoles: Role[] = ["admin", "farmaceutico"];

function errorText(error: unknown) {
  return error instanceof Error ? error.message : "Ocurrió un error inesperado.";
}

function medicationName(medication: Medication | string) {
  return typeof medication === "string" ? medication : medication.name;
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return <label className="form-field"><span>{label}</span>{children}</label>;
}

function PanelMessage({ error, notice }: { error: string; notice: string }) {
  return (
    <>
      {error && <p className="message error" role="alert">{error}</p>}
      {notice && <p className="message success" role="status">{notice}</p>}
    </>
  );
}

function InventoryPanel({ user }: { user: WorkspaceUser }) {
  const [medications, setMedications] = useState<Medication[]>([]);
  const [lots, setLots] = useState<Lot[]>([]);
  const [lowStock, setLowStock] = useState<Lot[]>([]);
  const [expiring, setExpiring] = useState<Lot[]>([]);
  const [requests, setRequests] = useState<MedicationRequest[]>([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);

  const loadInventory = useCallback(async () => {
    setError("");
    try {
      const [medicationList, lotList, lowStockList, expiringList] = await Promise.all([
        request<Medication[]>("/api/inventory/medications"),
        request<Lot[]>("/api/inventory/lots"),
        request<Lot[]>("/api/inventory/alerts/low-stock"),
        request<Lot[]>("/api/inventory/alerts/expiring?days=30"),
      ]);
      setMedications(medicationList);
      setLots(lotList);
      setLowStock(lowStockList);
      setExpiring(expiringList);
    } catch (cause) {
      setError(errorText(cause));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadInventory(); }, [loadInventory]);

  async function submit(
    event: FormEvent<HTMLFormElement>,
    action: (data: FormData) => Promise<void>,
  ) {
    event.preventDefault();
    const form = event.currentTarget;
    setError("");
    setNotice("");
    try {
      await action(new FormData(form));
      form.reset();
      await loadInventory();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function createMedication(data: FormData) {
    await request("/api/inventory/medications", undefined, {
      method: "POST",
      body: JSON.stringify({
        name: data.get("name"),
        activeIngredient: data.get("activeIngredient"),
        presentation: data.get("presentation"),
        concentration: data.get("concentration"),
      }),
    });
    setNotice("Medicamento agregado al inventario.");
  }

  async function createLot(data: FormData) {
    await request("/api/inventory/lots", undefined, {
      method: "POST",
      body: JSON.stringify({
        medication: data.get("medication"),
        lotCode: data.get("lotCode"),
        quantity: Number(data.get("quantity")),
        minimumStock: Number(data.get("minimumStock")),
        expirationDate: data.get("expirationDate"),
      }),
    });
    setNotice("Lote registrado.");
  }

  async function createRequest(data: FormData) {
    const medicationRequest = await request<MedicationRequest>(
      "/api/inventory/medication-requests",
      undefined,
      {
        method: "POST",
        body: JSON.stringify({
          medication: data.get("medication"),
          quantity: Number(data.get("quantity")),
          requestedBy: user.full_name,
        }),
      },
    );
    setRequests((current) => [medicationRequest, ...current]);
    setNotice("Solicitud creada y pendiente de despacho.");
  }

  async function dispatchMedicationRequest(id: string) {
    setError("");
    setNotice("");
    try {
      await request(`/api/inventory/medication-requests/${id}/dispatch`, undefined, {
        method: "POST",
      });
      setRequests((current) => current.map((item) =>
        item._id === id ? { ...item, status: "dispatched" } : item,
      ));
      setNotice("Solicitud despachada; el stock se actualizó.");
      await loadInventory();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function adjustStock(event: FormEvent<HTMLFormElement>, lot: Lot) {
    event.preventDefault();
    setError("");
    setNotice("");
    const form = new FormData(event.currentTarget);
    try {
      await request(`/api/inventory/lots/${lot._id}/stock`, undefined, {
        method: "PATCH",
        body: JSON.stringify({ adjustment: Number(form.get("adjustment")) }),
      });
      setNotice(`Stock del lote ${lot.lotCode} actualizado.`);
      await loadInventory();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  const canManage = staffRoles.includes(user.role);

  return (
    <div className="workspace">
      <PanelMessage error={error} notice={notice} />
      <div className="metric-grid">
        <Metric label="Medicamentos" value={medications.length} />
        <Metric label="Lotes registrados" value={lots.length} />
        <Metric label="Bajo stock" value={lowStock.length} warning={lowStock.length > 0} />
        <Metric label="Por vencer (30 días)" value={expiring.length} warning={expiring.length > 0} />
      </div>
      {loading ? <p className="empty-state">Consultando inventario...</p> : (
        <>
          {canManage && (
            <div className="operation-grid">
              <form className="operation-card" onSubmit={(event) => void submit(event, createMedication)}>
                <h3>Nuevo medicamento</h3>
                <Field label="Nombre"><input name="name" required /></Field>
                <Field label="Principio activo"><input name="activeIngredient" required /></Field>
                <Field label="Presentación"><input name="presentation" required placeholder="Tabletas, jarabe..." /></Field>
                <Field label="Concentración"><input name="concentration" required placeholder="500 mg" /></Field>
                <button className="primary-button" type="submit">Guardar medicamento</button>
              </form>
              <form className="operation-card" onSubmit={(event) => void submit(event, createLot)}>
                <h3>Registrar lote</h3>
                <Field label="Medicamento">
                  <select name="medication" required defaultValue="">
                    <option value="" disabled>Selecciona un medicamento</option>
                    {medications.filter((item) => item.active).map((item) =>
                      <option key={item._id} value={item._id}>{item.name}</option>,
                    )}
                  </select>
                </Field>
                <Field label="Código de lote"><input name="lotCode" required /></Field>
                <div className="field-row">
                  <Field label="Unidades"><input name="quantity" type="number" min="0" step="1" required /></Field>
                  <Field label="Stock mínimo"><input name="minimumStock" type="number" min="0" step="1" defaultValue="10" required /></Field>
                </div>
                <Field label="Vencimiento"><input name="expirationDate" type="date" min={new Date().toISOString().slice(0, 10)} required /></Field>
                <button className="primary-button" type="submit">Guardar lote</button>
              </form>
            </div>
          )}
          <div className="section-card">
            <div className="section-heading"><div><h2>Solicitar medicamento</h2><p>El despacho consume primero los lotes próximos a vencer.</p></div></div>
            <form className="inline-form" onSubmit={(event) => void submit(event, createRequest)}>
              <Field label="Medicamento">
                <select name="medication" required defaultValue="">
                  <option value="" disabled>Selecciona un medicamento</option>
                  {medications.filter((item) => item.active).map((item) =>
                    <option key={item._id} value={item._id}>{item.name}</option>,
                  )}
                </select>
              </Field>
              <Field label="Cantidad"><input name="quantity" type="number" min="1" step="1" required /></Field>
              <button className="primary-button" type="submit">Crear solicitud</button>
            </form>
            {requests.length > 0 && (
              <div className="request-list">
                <h3>Solicitudes de esta sesión</h3>
                {requests.map((item) => (
                  <div className="request-row" key={item._id}>
                    <span><strong>{medicationName(item.medication)}</strong><small>{item.quantity} unidades · {item.status}</small></span>
                    {canManage && item.status === "pending" && <button className="action-button" onClick={() => void dispatchMedicationRequest(item._id)}>Despachar</button>}
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="section-card">
            <div className="section-heading"><div><h2>Existencias por lote</h2><p>Consulta stock, vencimiento y ajustes.</p></div><button className="action-button" onClick={() => void loadInventory()}>Actualizar</button></div>
            {lots.length === 0 ? <p className="empty-state">Aún no hay lotes registrados.</p> : (
              <div className="table-wrap">
                <table><thead><tr><th>MEDICAMENTO</th><th>LOTE</th><th>UNIDADES</th><th>VENCIMIENTO</th>{canManage && <th>AJUSTAR</th>}</tr></thead>
                  <tbody>{lots.map((lot) => (
                    <tr key={lot._id}>
                      <td>{medicationName(lot.medication)}</td><td>{lot.lotCode}</td><td>{lot.quantity} <small>mín. {lot.minimumStock}</small></td>
                      <td>{new Date(lot.expirationDate).toLocaleDateString("es-CO")}</td>
                      {canManage && <td><form className="stock-adjust" onSubmit={(event) => void adjustStock(event, lot)}><input name="adjustment" type="number" step="1" required aria-label={`Ajuste de ${lot.lotCode}`} placeholder="+/-" /><button className="action-button" type="submit">Aplicar</button></form></td>}
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            )}
          </div>
          {(lowStock.length > 0 || expiring.length > 0) && (
            <div className="operation-grid">
              <AlertCard title="Bajo stock" lots={lowStock} />
              <AlertCard title="Próximos a vencer" lots={expiring} />
            </div>
          )}
        </>
      )}
    </div>
  );
}

function AlertCard({ title, lots }: { title: string; lots: Lot[] }) {
  return (
    <section className="section-card">
      <div className="section-heading"><div><h2>{title}</h2><p>{lots.length} lote(s) requieren atención</p></div></div>
      {lots.length === 0 ? <p className="empty-state">Sin alertas.</p> : lots.map((lot) =>
        <div className="request-row" key={lot._id}><span><strong>{medicationName(lot.medication)}</strong><small>Lote {lot.lotCode} · {lot.quantity} unidades · {new Date(lot.expirationDate).toLocaleDateString("es-CO")}</small></span></div>,
      )}
    </section>
  );
}

function TurnosPanel({ user }: { user: WorkspaceUser }) {
  const [availability, setAvailability] = useState<Disponibilidad[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [nextTurn, setNextTurn] = useState<Turno | null>(null);
  const [loading, setLoading] = useState(true);

  const loadTurnos = useCallback(async () => {
    setError("");
    try {
      const [slots, patientTurns] = await Promise.all([
        request<Disponibilidad[]>("/api/disponibilidad"),
        request<Turno[]>(`/api/turnos/paciente/${user.id}`),
      ]);
      setAvailability(slots);
      setTurnos(patientTurns);
    } catch (cause) {
      setError(errorText(cause));
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => { void loadTurnos(); }, [loadTurnos]);

  async function createAvailability(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setError("");
    setNotice("");
    const data = new FormData(form);
    try {
      await request("/api/disponibilidad", undefined, {
        method: "POST",
        body: JSON.stringify({
          idSede: data.get("idSede"),
          fecha: data.get("fecha"),
          horaInicio: data.get("horaInicio"),
          horaFin: data.get("horaFin"),
          cupo: Number(data.get("cupo")),
        }),
      });
      form.reset();
      setNotice("Disponibilidad publicada.");
      await loadTurnos();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function createTurno(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");
    const data = new FormData(event.currentTarget);
    try {
      await request<Turno>("/api/turnos/solicitar", undefined, {
        method: "POST",
        body: JSON.stringify({
          idPaciente: String(user.id),
          idDisponibilidad: data.get("idDisponibilidad"),
          esUrgente: data.get("esUrgente") === "on",
        }),
      });
      setNotice("Turno solicitado.");
      await loadTurnos();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function cancelTurno(id: string) {
    setError("");
    setNotice("");
    try {
      await request(`/api/turnos/${id}/cancelar`, undefined, { method: "PUT" });
      setNotice("Turno cancelado.");
      await loadTurnos();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function assignNext() {
    setError("");
    setNotice("");
    try {
      const assigned = await request<Turno>("/api/turnos/siguiente", undefined, { method: "PUT" });
      setNextTurn(assigned);
      setNotice("Siguiente turno asignado.");
      await loadTurnos();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  const canManage = staffRoles.includes(user.role);

  return (
    <div className="workspace">
      <PanelMessage error={error} notice={notice} />
      <div className="metric-grid">
        <Metric label="Disponibilidades" value={availability.length} />
        <Metric label="Mis turnos" value={turnos.length} />
        <Metric label="En espera" value={turnos.filter((turn) => turn.estado === "EN_ESPERA").length} />
      </div>
      {canManage && (
        <div className="operation-grid">
          <form className="operation-card" onSubmit={(event) => void createAvailability(event)}>
            <h3>Publicar disponibilidad</h3>
            <Field label="Sede"><input name="idSede" required /></Field>
            <Field label="Fecha"><input name="fecha" type="date" min={new Date().toISOString().slice(0, 10)} required /></Field>
            <div className="field-row"><Field label="Desde"><input name="horaInicio" type="time" required /></Field><Field label="Hasta"><input name="horaFin" type="time" required /></Field></div>
            <Field label="Cupos"><input name="cupo" type="number" min="1" step="1" required /></Field>
            <button className="primary-button" type="submit">Publicar horario</button>
          </form>
          <section className="section-card availability-card">
            <div className="section-heading"><div><h2>Horarios disponibles</h2><p>Selecciona un horario para solicitar turno.</p></div></div>
            {availability.length === 0 ? <p className="empty-state">No hay disponibilidades registradas.</p> : (
              <form className="operation-card compact-card" onSubmit={(event) => void createTurno(event)}>
                <Field label="Horario"><select name="idDisponibilidad" required defaultValue="">
                  <option value="" disabled>Selecciona fecha y sede</option>
                  {availability.map((slot) => <option key={slot.idDisponibilidad} value={slot.idDisponibilidad}>{slot.idSede} · {slot.fecha} · {slot.horaInicio}–{slot.horaFin} · {slot.cupo} cupos</option>)}
                </select></Field>
                <label className="checkbox-field"><input name="esUrgente" type="checkbox" /> Solicitud prioritaria por urgencia clínica</label>
                <button className="primary-button" type="submit">Solicitar turno</button>
              </form>
            )}
          </section>
        </div>
      )}
      {!canManage && (
        <section className="section-card">
          <div className="section-heading"><div><h2>Solicitar un turno</h2><p>Elige uno de los horarios con cupo disponible.</p></div></div>
          {availability.length === 0 ? <p className="empty-state">No hay disponibilidades registradas.</p> : (
            <form className="inline-form" onSubmit={(event) => void createTurno(event)}>
              <Field label="Horario"><select name="idDisponibilidad" required defaultValue="">
                <option value="" disabled>Selecciona fecha y sede</option>
                {availability.map((slot) => <option key={slot.idDisponibilidad} value={slot.idDisponibilidad}>{slot.idSede} · {slot.fecha} · {slot.horaInicio}–{slot.horaFin} · {slot.cupo} cupos</option>)}
              </select></Field>
              <label className="checkbox-field"><input name="esUrgente" type="checkbox" /> Prioridad clínica</label>
              <button className="primary-button" type="submit">Solicitar turno</button>
            </form>
          )}
        </section>
      )}
      {canManage && <section className="section-card queue-card"><div><h2>Atención de turnos</h2><p>Solicita al servicio el siguiente turno priorizado.</p></div><button className="primary-button" onClick={() => void assignNext()}>Asignar siguiente turno</button></section>}
      {nextTurn && <section className="section-card next-turn"><span className="eyebrow">SIGUIENTE TURNO</span><strong>{nextTurn.idPaciente}</strong><span>{nextTurn.prioridad} · {nextTurn.estado}</span></section>}
      <section className="section-card">
        <div className="section-heading"><div><h2>Mis turnos</h2><p>Consulta y administra tus solicitudes.</p></div><button className="action-button" onClick={() => void loadTurnos()}>Actualizar</button></div>
        {loading ? <p className="empty-state">Consultando turnos...</p> : turnos.length === 0 ? <p className="empty-state">Todavía no tienes turnos.</p> : (
          <div className="table-wrap"><table><thead><tr><th>FECHA DE SOLICITUD</th><th>PRIORIDAD</th><th>ESTADO</th><th>ACCIONES</th></tr></thead>
            <tbody>{turnos.map((turn) => <tr key={turn.idTurno}><td>{new Date(turn.fechaHoraSolicitud).toLocaleString("es-CO")}</td><td>{turn.prioridad}</td><td><span className={`status ${turn.estado === "CANCELADO" ? "inactive" : "active"}`}><i />{turn.estado}</span></td><td>{turn.estado === "EN_ESPERA" && <button className="action-button" onClick={() => void cancelTurno(turn.idTurno)}>Cancelar</button>}</td></tr>)}</tbody>
          </table></div>
        )}
      </section>
    </div>
  );
}

function DeliveryPanel() {
  const [orders, setOrders] = useState<DeliveryOrder[]>([]);
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [routeId, setRouteId] = useState("");
  const [route, setRoute] = useState<Route | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function createOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setError("");
    setNotice("");
    const data = new FormData(form);
    const orderId = String(data.get("order_id")).trim() || `pedido-${Date.now()}`;
    try {
      const order = await request<DeliveryOrder>("/api/v1/delivery-orders", undefined, {
        method: "POST",
        headers: { "Idempotency-Key": orderId },
        body: JSON.stringify({
          order_id: orderId,
          destination: {
            street: data.get("street"),
            city: data.get("city"),
            postal_code: data.get("postal_code") || null,
            latitude: data.get("latitude") ? Number(data.get("latitude")) : null,
            longitude: data.get("longitude") ? Number(data.get("longitude")) : null,
            reference: data.get("reference") || null,
          },
          priority: data.get("priority"),
          external_reference: data.get("external_reference") || null,
        }),
      });
      setOrders((current) => [order, ...current.filter((item) => item.order_id !== order.order_id)]);
      if (order.route_id) setRouteId(order.route_id);
      setNotice(`Pedido ${order.order_id} recibido.`);
      form.reset();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function registerDriver(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setError("");
    setNotice("");
    const data = new FormData(form);
    try {
      const driver = await request<Driver>("/api/v1/drivers", undefined, {
        method: "POST",
        body: JSON.stringify({
          driver_id: data.get("driver_id"),
          name: data.get("name"),
          phone: data.get("phone") || null,
        }),
      });
      setDrivers((current) => [driver, ...current.filter((item) => item.driver_id !== driver.driver_id)]);
      setNotice("Domiciliario registrado.");
      form.reset();
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function toggleAvailability(driver: Driver) {
    setError("");
    setNotice("");
    try {
      const updated = await request<Driver>(`/api/v1/drivers/${encodeURIComponent(driver.driver_id)}/availability`, undefined, {
        method: "PATCH",
        body: JSON.stringify({ available: driver.status !== "AVAILABLE" }),
      });
      setDrivers((current) => current.map((item) => item.driver_id === updated.driver_id ? updated : item));
      setNotice(`Disponibilidad de ${updated.name} actualizada.`);
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function loadRoute(id = routeId) {
    if (!id.trim()) {
      setError("Ingresa el identificador de una ruta.");
      return;
    }
    setError("");
    setNotice("");
    try {
      const result = await request<Route>(`/api/v1/routes/${encodeURIComponent(id.trim())}`);
      setRoute(result);
      setRouteId(result.route_id);
      setNotice("Ruta consultada.");
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  async function updateRoute(path: string, successMessage: string) {
    if (!route) return;
    setError("");
    setNotice("");
    try {
      const updated = await request<Route>(path, undefined, { method: "POST" });
      setRoute(updated);
      setNotice(successMessage);
    } catch (cause) {
      setError(errorText(cause));
    }
  }

  return (
    <div className="workspace">
      <PanelMessage error={error} notice={notice} />
      <div className="metric-grid">
        <Metric label="Pedidos de esta sesión" value={orders.length} />
        <Metric label="Domiciliarios registrados" value={drivers.length} />
        <Metric label="Rutas consultadas" value={route ? 1 : 0} />
      </div>
      <div className="operation-grid">
        <form className="operation-card" onSubmit={(event) => void createOrder(event)}>
          <h3>Crear pedido de domicilio</h3>
          <Field label="Referencia del pedido"><input name="order_id" placeholder="Se genera automáticamente si se deja vacío" /></Field>
          <Field label="Dirección"><input name="street" required /></Field>
          <div className="field-row"><Field label="Ciudad"><input name="city" required /></Field><Field label="Código postal"><input name="postal_code" /></Field></div>
          <div className="field-row"><Field label="Latitud"><input name="latitude" type="number" min="-90" max="90" step="any" /></Field><Field label="Longitud"><input name="longitude" type="number" min="-180" max="180" step="any" /></Field></div>
          <Field label="Prioridad"><select name="priority" defaultValue="NORMAL"><option value="NORMAL">Normal</option><option value="HIGH">Alta</option></select></Field>
          <Field label="Referencia de entrega"><input name="reference" /></Field>
          <button className="primary-button" type="submit">Registrar pedido</button>
        </form>
        <form className="operation-card" onSubmit={(event) => void registerDriver(event)}>
          <h3>Registrar domiciliario</h3>
          <Field label="Identificador"><input name="driver_id" required /></Field>
          <Field label="Nombre completo"><input name="name" required /></Field>
          <Field label="Teléfono"><input name="phone" type="tel" /></Field>
          <button className="primary-button" type="submit">Registrar domiciliario</button>
        </form>
      </div>
      {orders.length > 0 && (
        <section className="section-card">
          <div className="section-heading"><div><h2>Pedidos recientes</h2><p>El historial de pedidos no está expuesto por este servicio; se muestran los de esta sesión.</p></div></div>
          {orders.map((order) => <div className="request-row" key={order.order_id}><span><strong>{order.order_id}</strong><small>{order.status} · Ruta: {order.route_id ?? "Pendiente de asignación"}</small></span>{order.route_id && <button className="action-button" onClick={() => { setRouteId(order.route_id ?? ""); void loadRoute(order.route_id ?? ""); }}>Ver ruta</button>}</div>)}
        </section>
      )}
      {drivers.length > 0 && (
        <section className="section-card">
          <div className="section-heading"><div><h2>Domiciliarios registrados en esta sesión</h2><p>Consulta de disponibilidad y estado.</p></div></div>
          {drivers.map((driver) => <div className="request-row" key={driver.driver_id}><span><strong>{driver.name}</strong><small>{driver.driver_id} · {driver.status}{driver.current_route_id ? ` · Ruta ${driver.current_route_id}` : ""}</small></span><button className="action-button" onClick={() => void toggleAvailability(driver)}>{driver.status === "AVAILABLE" ? "Marcar no disponible" : "Marcar disponible"}</button></div>)}
        </section>
      )}
      <section className="section-card">
        <div className="section-heading"><div><h2>Operar ruta</h2><p>Consulta una ruta asignada y actualiza sus paradas.</p></div></div>
        <form className="inline-form route-lookup" onSubmit={(event) => { event.preventDefault(); void loadRoute(); }}>
          <Field label="Identificador de ruta"><input value={routeId} onChange={(event) => setRouteId(event.target.value)} required /></Field>
          <button className="action-button" type="submit">Consultar ruta</button>
          {route && <button className="action-button" type="button" onClick={() => void updateRoute(`/api/v1/routes/${encodeURIComponent(route.route_id)}/start`, "Ruta iniciada.")}>Iniciar ruta</button>}
        </form>
        {route && (
          <div className="route-details">
            <p><strong>{route.city}</strong><span>{route.status} · Domiciliario: {route.driver_id ?? "Sin asignar"}</span></p>
            {route.stops.map((stop) => (
              <div className="request-row" key={stop.order_id}><span><strong>#{stop.sequence} · {stop.order_id}</strong><small>{stop.completed ? "Entregado" : "Pendiente"}</small></span>{!stop.completed && <button className="action-button" onClick={() => void updateRoute(`/api/v1/routes/${encodeURIComponent(route.route_id)}/stops/${encodeURIComponent(stop.order_id)}/complete`, "Entrega marcada como completada.")}>Completar entrega</button>}</div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function Metric({ label, value, warning = false }: { label: string; value: number; warning?: boolean }) {
  return <div className={`metric-card ${warning ? "warning" : ""}`}><span>{label}</span><strong>{value}</strong></div>;
}

export default function Operations({
  section,
  user,
}: {
  section: Section;
  user: WorkspaceUser;
}) {
  if (section === "inventory") return <InventoryPanel user={user} />;
  if (section === "turnos") return <TurnosPanel user={user} />;
  return <DeliveryPanel />;
}
