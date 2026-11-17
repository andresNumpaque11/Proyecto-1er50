# Arquitectura hexagonal del microservicio `ms-turnos`

## 1. Objetivo

El microservicio `ms-turnos` utiliza una arquitectura hexagonal para separar la lógica de negocio de los detalles técnicos como Spring, JPA, PostgreSQL, REST y las estructuras de mensajería o almacenamiento en memoria.

Esta separación permite que el núcleo del sistema describa **qué debe hacer** el microservicio, mientras que los adaptadores describen **cómo se comunica o almacena información**.

En esta implementación:

- El dominio representa turnos, disponibilidades y prioridades.
- Los casos de uso coordinan las operaciones del negocio.
- Los puertos definen contratos independientes de las tecnologías.
- Los adaptadores conectan el sistema con REST y PostgreSQL.
- Spring se utiliza como medio de composición e integración.

---

## 2. Estructura de paquetes

La estructura principal se encuentra dentro de:

```text
com.sigtam.ms_turnos
```

```text
ms-turnos/
└── src/main/java/com/sigtam/ms_turnos/
    ├── MsTurnosApplication.java
    ├── domain/
    │   ├── model/
    │   └── service/
    ├── application/
    │   ├── port/
    │   │   ├── in/
    │   │   └── out/
    │   ├── exception/
    │   └── service/
    ├── infrastructure/
    │   ├── persistence/
    │   ├── queue/
    │   └── config/
    ├── controller/
    ├── dto/
    └── exception/
```

---

## 3. Principios generales

### Núcleo central

El núcleo contiene:

- [Modelo de dominio](ms-turnos/src/main/java/com/sigtam/ms_turnos/domain/model)
- [Servicios y estrategias del dominio](ms-turnos/src/main/java/com/sigtam/ms_turnos/domain/service)
- [Casos de uso](ms-turnos/src/main/java/com/sigtam/ms_turnos/application/service)
- [Puertos](ms-turnos/src/main/java/com/sigtam/ms_turnos/application/port)

El núcleo no debe depender directamente de controladores REST ni de clases JPA.

### Adaptadores

Los adaptadores permiten integrar el núcleo con sistemas externos:

- Controladores REST: reciben peticiones HTTP.
- Adaptadores JPA: permiten guardar y consultar información.
- Adaptador de cola: administra turnos por prioridad.
- Adaptador de transacciones: integra los eventos con Spring.

### Dirección de dependencias

```text
controller ───────────────┐
                          ▼
                    application
                          ▲
                          │
                  ports implementados
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
        infrastructure           persistence
```

Las dependencias deben dirigirse hacia dentro:

```text
Adaptadores
    ↓
Puertos
    ↓
Casos de uso
    ↓
Dominio
```

---

# 4. Capa de dominio

La capa de dominio representa el conocimiento propio del negocio.

Ubicación:

```text
domain/model
domain/service
```

## Modelo `Turno`

El turno contiene:

- Identificador.
- Identificador del paciente.
- Identificador de la disponibilidad.
- Fecha y hora de solicitud.
- Estado.
- Prioridad.

Sus estados posibles son:

- `EN_ESPERA`
- `ATENDIDO`
- `CANCELADO`

Sus prioridades posibles son:

- `NORMAL`
- `CLINICA`

El objeto puede cancelar o atender un turno mediante métodos propios del dominio.

## Modelo `Disponibilidad`

La disponibilidad representa un horario con cupos disponibles.

Contiene:

- Identificador.
- Sede.
- Fecha.
- Hora de inicio.
- Hora final.
- Cantidad de cupos.

El dominio protege directamente las operaciones sobre cupos:

```java
disponibilidad.reservarCupo();
disponibilidad.liberarCupo();
```

Si no existe cupo disponible, se impide la reserva.

## Estrategias de prioridad

El dominio define la interfaz:

```java
EstrategiaPrioridad
```

Las estrategias disponibles son:

- `PrioridadClinica`
- `PrioridadNormal`

El comportamiento es:

| Solicitud urgente | Prioridad resultante |
|---|---|
| `true` | `CLINICA` |
| `false` | `NORMAL` |

Las estrategias permiten agregar otros criterios de prioridad sin modificar el caso de uso principal.

---

# 5. Capa de aplicación

La capa de aplicación coordina los casos de uso y define los puertos mediante los cuales se comunica con el exterior.

No contiene controladores REST ni clases JPA.

## Puertos de entrada

Los puertos de entrada representan las operaciones que el sistema puede recibir.

Ejemplos:

- `GestionarTurnosUseCase`
- `GestionarDisponibilidadUseCase`

El puerto de turnos expone operaciones como:

- Solicitar un turno.
- Cancelar un turno.
- Asignar el siguiente turno.
- Consultar turnos por paciente.

Esto permite que el sistema pueda ser utilizado desde REST, mensajería u otra interfaz futura sin modificar el caso de uso.

## Puertos de salida

Los puertos de salida representan servicios externos necesarios por la aplicación.

Ejemplos:

- `TurnoRepositoryPort`
- `DisponibilidadRepositoryPort`
- `TurnQueuePort`
- `TransactionPort`

Estos puertos contienen solamente los métodos que la aplicación necesita.

Por ejemplo, un caso de uso puede solicitar:

```java
turnoRepository.save(turno);
```

pero no necesita conocer PostgreSQL, Spring Data o una implementación específica de JPA.

## Casos de uso

Los casos de uso implementan la lógica de coordinación:

- `GestorTurnosService`
- `GestionarDisponibilidadService`

### Solicitar un turno

El flujo interno es:

1. Buscar la disponibilidad solicitada.
2. Bloquear la disponibilidad para evitar reservas concurrentes.
3. Comprobar si queda cupo.
4. Descontar un cupo.
5. Seleccionar la estrategia de prioridad.
6. Crear el turno.
7. Guardar el turno.
8. Guardar la disponibilidad actualizada.
9. Agregar el turno a la cola después de confirmar la transacción.

Si no existe disponibilidad, se produce una excepción de recurso no encontrado.

Si no queda cupo, se produce una excepción de conflicto.

### Cancelar un turno

El flujo interno es:

1. Buscar el turno.
2. Verificar que esté en estado `EN_ESPERA`.
3. Buscar la disponibilidad asociada.
4. Liberar un cupo.
5. Cambiar el estado a `CANCELADO`.
6. Guardar el turno.
7. Retirar el turno de la cola después del commit.

### Asignar el siguiente turno

El flujo interno es:

1. Obtener el siguiente turno de la cola.
2. Consultar nuevamente su estado en la base de datos.
3. Ignorar turnos cancelados o atendidos.
4. Cambiar el estado a `ATENDIDO`.
5. Guardar el turno.
6. Si la transacción falla, volver a agregar el turno a la cola.

### Consultar turnos por paciente

El caso de uso consulta los turnos asociados al identificador del paciente.

---

# 6. Adaptador REST entrante

Los controladores representan adaptadores de entrada basados en HTTP.

Ubicación:

```text
controller
```

Los controladores actuales son:

- `TurnoController`
- `DisponibilidadController`

Ejemplo de operación:

```java
@PostMapping("/solicitar")
```

El controlador recibe un DTO HTTP, valida su contenido y llama al puerto de entrada correspondiente.

## Flujo de una solicitud HTTP

```text
Cliente
  ↓
DTO HTTP
  ↓
Controlador REST
  ↓
Puerto de entrada
  ↓
Caso de uso
  ↓
Puerto de salida
  ↓
Adaptador JPA
  ↓
PostgreSQL
```

Los DTO no representan modelos de dominio. Su responsabilidad es transportar datos de y hacia el protocolo HTTP.

Los endpoints existentes se conservaron para no romper a los clientes.

---

# 7. Adaptadores de persistencia

La persistencia se encuentra dentro de:

```text
infrastructure/persistence
```

Incluye:

- Entidades JPA.
- Repositorios Spring Data.
- Adaptadores que implementan los puertos de persistencia.
- Conversión entre objetos JPA y objetos de dominio.

## Entidades JPA

Las entidades JPA son un detalle de infraestructura:

- `TurnoEntity`
- `DisponibilidadEntity`

No deben confundirse con los modelos de dominio `Turno` y `Disponibilidad`.

## Repositorios Spring

Los repositorios Spring son:

- `SpringTurnoRepository`
- `SpringDisponibilidadRepository`

Se utilizan para acceder a PostgreSQL y ejecutar operaciones JPA.

## Adaptadores de persistencia

Los adaptadores traducen y conectan las clases:

- `TurnoRepositoryAdapter` implementa `TurnoRepositoryPort`.
- `DisponibilidadRepositoryAdapter` implementa `DisponibilidadRepositoryPort`.

Cada adaptador realiza una doble conversión:

```text
Objeto de dominio → Entidad JPA → Base de datos
Base de datos → Entidad JPA → Objeto de dominio
```

Esto evita que JPA aparezca dentro de los casos de uso.

---

# 8. Adaptador de cola

La cola se encuentra en:

```text
infrastructure/queue
```

La clase `ColaTurnosAdapter` implementa `TurnQueuePort`.

Utiliza una `PriorityBlockingQueue`, que permite:

- Operaciones seguras entre hilos.
- Prioridad por urgencia.
- Orden por fecha de solicitud dentro de una misma prioridad.

El orden utilizado es:

1. Turnos con prioridad `CLINICA`.
2. Turnos normales.
3. Dentro de cada prioridad, el turno solicitado antes.

## Inicialización de la cola

`ColaTurnosInicializador` se ejecuta cuando Spring finaliza el arranque.

En ese momento:

1. Vacía la cola.
2. Consulta los turnos pendientes en PostgreSQL.
3. Los vuelve a cargar en memoria.

De esta manera, la cola no reemplaza a la base de datos. La base de datos sigue siendo la fuente de verdad.

---

# 9. Adaptador de transacciones

Las operaciones de solicitud y atención deben manejar transacciones correctamente.

`SpringTransactionAdapter` implementa `TransactionPort`.

Permite ejecutar acciones:

- Después de confirmar una transacción.
- Después de producirse un rollback.

## Después del commit

La cola recibe el turno únicamente cuando la transacción fue exitosa.

Por ejemplo:

- Solicitar turno: agregar el turno después del commit.
- Cancelar turno: eliminarlo de la cola después del commit.

## Después del rollback

Si al asignar el siguiente turno falla la transacción, el turno vuelve a la cola.

Esto evita que el sistema pierda turnos por errores de persistencia.

---

# 10. Control de concurrencia

Cuando varios usuarios solicitan turnos al mismo tiempo, la disponibilidad podría quedar con cupos incorrectos si no existe control de concurrencia.

El repositorio JPA utiliza bloqueo pesimista:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

Esto produce una lectura con bloqueo de escritura sobre la disponibilidad.

Resultado:

- Una solicitud bloquea la fila.
- Otra solicitud espera hasta que termine.
- El segundo proceso vuelve a evaluar el cupo actualizado.
- No se pueden consumir dos veces los mismos cupos.

---

# 11. Manejo de errores

La aplicación define excepciones propias:

- `RecursoNoEncontradoException`
- `ConflictoException`

El adaptador REST transforma estas excepciones en respuestas HTTP mediante `GlobalExceptionHandler`.

Ejemplos:

| Situación | Respuesta |
|---|---|
| Recurso inexistente | `404 Not Found` |
| Sin cupo | `409 Conflict` |
| Cola vacía | `409 Conflict` |
| Datos inválidos | `400 Bad Request` |
| Error interno no controlado | `500 Internal Server Error` |

---

# 12. Ejemplo completo de ejecución

## Solicitud de un turno

```text
POST /api/turnos/solicitar
```

Flujo técnico:

```text
1. Cliente envía la petición HTTP.
2. TurnoController recibe el TurnoRequestDTO.
3. El controlador llama a GestionarTurnosUseCase.
4. GestorTurnosService ejecuta el caso de uso.
5. Se consulta DisponibilidadRepositoryPort.
6. DisponibilidadRepositoryAdapter consulta PostgreSQL.
7. Se reserva un cupo.
8. Se selecciona la estrategia de prioridad.
9. Se guarda el turno mediante TurnoRepositoryPort.
10. Al confirmar la transacción, se agrega el turno a la cola.
11. El controlador convierte el Turno en TurnoResponseDTO.
12. REST devuelve la respuesta al cliente.
```

---

# 13. Beneficios obtenidos

La arquitectura permite:

- Cambiar PostgreSQL por otro repositorio sin modificar casos de uso.
- Cambiar REST por otro mecanismo de entrada.
- Cambiar la cola en memoria por Redis o Kafka.
- Probar las reglas de negocio sin iniciar infraestructura.
- Mantener independiente el dominio de Spring.
- Agregar nuevas estrategias de prioridad.
- Añadir otros casos de uso sin aumentar el acoplamiento.

Por ejemplo, para reemplazar JPA por MongoDB solamente se necesita crear otro adaptador que implemente los puertos de persistencia.

No sería necesario modificar:

- `GestorTurnosService`
- `GestionarDisponibilidadService`
- Los modelos del dominio

---

# 14. Comprobación de la arquitectura

Es posible verificar que el dominio no depende de infraestructura ejecutando búsquedas.

Desde el directorio `ms-turnos`:

```powershell
rg "org.springframework|jakarta.persistence|com\.sigtam\.ms_turnos\.(controller|dto|infrastructure)" src\main\java\com\sigtam\ms_turnos\domain
```

Esta búsqueda debería encontrar pocas o ninguna dependencia.

De manera similar:

```powershell
rg "jakarta.persistence|com\.sigtam\.ms_turnos\.(controller|infrastructure)" src\main\java\com\sigtam\ms_turnos\application
```

Además, se puede agregar ArchUnit para imponer arquitecturalmente las reglas.

Ejemplo conceptual:

```java
noClasses()
    .that()
    .resideInAPackage("..domain..")
    .should()
    .dependOnClassesThat()
    .resideInAnyPackage(
        "..controller..",
        "..infrastructure..",
        "..dto.."
    );
```

Esta prueba impediría que el dominio dependa directamente de infraestructura o transporte.

---

# 15. Validación realizada

La migración fue validada mediante Maven:

```powershell
mvn test
```

Resultado esperado:

```text
Tests run: 1, Failures: 0, Errors: 0
BUILD SUCCESS
```

Esta prueba confirma que Spring puede iniciar el contexto y conectar los componentes después de la reorganización.

---

# 16. Nivel de aplicación práctica

La actualización convierte `ms-turnos` en una arquitectura hexagonal aplicada con Spring.

Conviene presentarla de esa manera, no como una arquitectura completamente independiente de frameworks.

Actualmente existen dos posibles mejoras para llevar el aislamiento a un nivel más puro:

1. Retirar `@Transactional` de los casos de uso.
2. Retirar `@Component` de las estrategias del dominio.

Esto permitiría que el dominio y la aplicación dependan todavía menos de Spring.

Por ejemplo:

- Spring podría configurar las estrategias mediante una clase de infraestructura.
- Las transacciones podrían administrarse por un puerto propio.
- Los casos de uso podrían mantener su lógica sin importar clases de Spring.

---

# 17. Conclusión

`ms-turnos` ahora separa:

- **Dominio**: reglas propias del sistema de turnos.
- **Aplicación**: coordinación de operaciones mediante casos de uso.
- **Puertos**: contratos independientes de tecnologías.
- **Adaptadores entrantes**: controladores REST.
- **Adaptadores salientes**: JPA, PostgreSQL, cola y transacciones.

Esta organización facilita el mantenimiento, las pruebas y el reemplazo de tecnologías externas sin modificar el núcleo del negocio.
