# Proyecto-1er50
microservicios gestion de medicamentos

## Solicitudes y despacho de medicamentos

El servicio de inventario expone estas rutas:

- `POST /api/inventory/medication-requests`: crea una solicitud pendiente. Recibe `medication` (id del medicamento), `quantity` (entero positivo) y `requestedBy` (solicitante).
- `POST /api/inventory/medication-requests/:id/dispatch`: despacha la solicitud completa. Consume primero los lotes próximos a vencer y no despacha si el stock vigente es insuficiente.
