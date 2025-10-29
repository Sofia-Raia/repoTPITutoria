# Historias de Usuario - Sistema de Gestión de Personas y Domicilios

Especificaciones funcionales completas del sistema CRUD de personas y domicilios.

## Tabla de Contenidos

- [Épica 1: Gestión de Personas](#épica-1-gestión-de-personas)
- [Épica 2: Gestión de Domicilios](#épica-2-gestión-de-domicilios)
- [Épica 3: Operaciones Asociadas](#épica-3-operaciones-asociadas)
- [Reglas de Negocio](#reglas-de-negocio)
- [Modelo de Datos](#modelo-de-datos)

---

## Épica 1: Gestión de Personas

### HU-001: Crear Persona

**Como** usuario del sistema
**Quiero** crear un registro de persona con sus datos básicos
**Para** almacenar información de personas en la base de datos

#### Criterios de Aceptación

```gherkin
Escenario: Crear persona sin domicilio
  Dado que el usuario selecciona "Crear persona"
  Cuando ingresa nombre "Juan", apellido "Pérez", DNI "12345678"
  Y responde "n" a agregar domicilio
  Entonces el sistema crea la persona con ID autogenerado
  Y muestra "Persona creada exitosamente con ID: X"

Escenario: Crear persona con domicilio
  Dado que el usuario selecciona "Crear persona"
  Cuando ingresa nombre "María", apellido "González", DNI "87654321"
  Y responde "s" a agregar domicilio
  Y ingresa calle "San Martín", número "123"
  Entonces el sistema crea el domicilio primero
  Y luego crea la persona con referencia al domicilio
  Y muestra "Persona creada exitosamente con ID: X"

Escenario: Intento de crear persona con DNI duplicado
  Dado que existe una persona con DNI "12345678"
  Cuando el usuario intenta crear una persona con el mismo DNI
  Entonces el sistema muestra "Ya existe una persona con el DNI: 12345678"
  Y no crea el registro

Escenario: Intento de crear persona con campos vacíos
  Dado que el usuario selecciona "Crear persona"
  Cuando deja el nombre vacío (solo espacios o enter)
  Entonces el sistema muestra "El nombre no puede estar vacío"
  Y no crea el registro
```

#### Reglas de Negocio Aplicables

- **RN-001**: Nombre, apellido y DNI son obligatorios
- **RN-002**: El DNI debe ser único en el sistema
- **RN-003**: Espacios iniciales y finales se eliminan automáticamente
- **RN-004**: El ID se genera automáticamente
- **RN-005**: El domicilio es opcional durante la creación

#### Implementación Técnica

- **Clase**: `MenuHandler.crearPersona()` (líneas 25-47)
- **Servicio**: `PersonaServiceImpl.insertar()` (líneas 24-37)
- **Validación**: `PersonaServiceImpl.validatePersona()` + `validateDniUnique()`
- **Flujo**:
  1. Captura entrada con `.trim()`
  2. Crea objeto Persona
  3. Si hay domicilio y `id == 0`, inserta domicilio primero
  4. Inserta persona con FK al domicilio
  5. Genera ID automático con `Statement.RETURN_GENERATED_KEYS`

---

### HU-002: Listar Todas las Personas

**Como** usuario del sistema
**Quiero** ver un listado de todas las personas registradas
**Para** consultar la información almacenada

#### Criterios de Aceptación

```gherkin
Escenario: Listar todas las personas con domicilio
  Dado que existen personas en el sistema
  Cuando el usuario selecciona "Listar personas"
  Y elige opción "1" (listar todos)
  Entonces el sistema muestra todas las personas no eliminadas
  Y para cada persona con domicilio muestra "Domicilio: [calle] [numero]"

Escenario: Listar personas sin domicilio
  Dado que existe persona sin domicilio asociado
  Cuando el usuario lista todas las personas
  Entonces el sistema muestra la persona sin línea de domicilio

Escenario: No hay personas en el sistema
  Dado que no existen personas activas
  Cuando el usuario lista todas las personas
  Entonces el sistema muestra "No se encontraron personas."
```

#### Reglas de Negocio Aplicables

- **RN-006**: Solo se listan personas con `eliminado = FALSE`
- **RN-007**: El domicilio se obtiene mediante LEFT JOIN
- **RN-008**: Si `domicilio_id` es NULL, no se muestra domicilio

#### Implementación Técnica

- **Clase**: `MenuHandler.listarPersonas()` (líneas 49-82, subopción 1)
- **Servicio**: `PersonaServiceImpl.getAll()`
- **DAO**: `PersonaDAO.getAll()` con `SELECT_ALL_SQL`
- **Query**:
  ```sql
  SELECT p.id, p.nombre, p.apellido, p.dni, p.domicilio_id,
         d.id AS dom_id, d.calle, d.numero
  FROM personas p LEFT JOIN domicilios d ON p.domicilio_id = d.id
  WHERE p.eliminado = FALSE
  ```

---

### HU-003: Buscar Personas por Nombre o Apellido

**Como** usuario del sistema
**Quiero** buscar personas por nombre o apellido
**Para** encontrar rápidamente una persona específica

#### Criterios de Aceptación

```gherkin
Escenario: Buscar por nombre con coincidencia parcial
  Dado que existen personas "Juan Pérez" y "Juana García"
  Cuando el usuario busca por "Jua"
  Entonces el sistema muestra ambas personas

Escenario: Buscar por apellido
  Dado que existen personas "Juan Pérez" y "María Pérez"
  Cuando el usuario busca por "Pérez"
  Entonces el sistema muestra ambas personas

Escenario: Búsqueda sin resultados
  Dado que no existen personas con "Rodríguez"
  Cuando el usuario busca por "Rodríguez"
  Entonces el sistema muestra "No se encontraron personas."

Escenario: Búsqueda con espacios
  Dado que el usuario busca por "  Juan  " (con espacios)
  Cuando se ejecuta la búsqueda
  Entonces el sistema elimina espacios y busca por "Juan"
```

#### Reglas de Negocio Aplicables

- **RN-009**: La búsqueda es case-insensitive
- **RN-010**: Se busca con LIKE %texto%
- **RN-011**: Busca en nombre OR apellido
- **RN-012**: Espacios se eliminan automáticamente
- **RN-013**: No se permiten búsquedas vacías

#### Implementación Técnica

- **Clase**: `MenuHandler.listarPersonas()` (líneas 49-82, subopción 2, línea 59 con trim())
- **Servicio**: `PersonaServiceImpl.buscarPorNombreApellido()`
- **DAO**: `PersonaDAO.buscarPorNombreApellido()` con `SEARCH_BY_NAME_SQL`
- **Query**:
  ```sql
  WHERE p.eliminado = FALSE AND (p.nombre LIKE ? OR p.apellido LIKE ?)
  ```
- **Parámetros**: `%filtro%` en ambos placeholders

---

### HU-004: Actualizar Persona

**Como** usuario del sistema
**Quiero** modificar los datos de una persona existente
**Para** mantener la información actualizada

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar solo apellido
  Dado que existe persona con ID 1, apellido "Pérez"
  Cuando el usuario actualiza la persona ID 1
  Y presiona Enter en nombre y DNI
  Y escribe "González" en apellido
  Entonces el sistema actualiza solo el apellido
  Y mantiene nombre y DNI sin cambios

Escenario: Actualizar con DNI duplicado
  Dado que existen personas con DNI "111" y "222"
  Cuando el usuario intenta cambiar DNI de persona "222" a "111"
  Entonces el sistema muestra "Ya existe una persona con el DNI: 111"
  Y no actualiza el registro

Escenario: Actualizar con mismo DNI
  Dado que existe persona con ID 1, DNI "12345678"
  Cuando el usuario actualiza otros campos manteniendo el mismo DNI
  Entonces el sistema permite la actualización
  Y no muestra error de DNI duplicado

Escenario: Agregar domicilio a persona sin domicilio
  Dado que persona ID 1 no tiene domicilio
  Cuando el usuario actualiza la persona
  Y responde "s" a agregar domicilio
  Y ingresa calle y número
  Entonces el sistema crea el domicilio
  Y lo asocia a la persona
```

#### Reglas de Negocio Aplicables

- **RN-014**: Se valida DNI único excepto para la misma persona
- **RN-015**: Campos vacíos (Enter) mantienen valor original
- **RN-016**: Se requiere ID > 0 para actualizar
- **RN-017**: Se puede agregar o actualizar domicilio durante actualización
- **RN-018**: Trim se aplica antes de validar si el campo está vacío

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarPersona()` (líneas 84-119)
- **Servicio**: `PersonaServiceImpl.actualizar()`
- **Validación**: `validateDniUnique(dni, personaId)` permite mismo DNI
- **Pattern**:
  ```java
  String nombre = scanner.nextLine().trim();
  if (!nombre.isEmpty()) {
      p.setNombre(nombre);
  }
  ```

---

### HU-005: Eliminar Persona

**Como** usuario del sistema
**Quiero** eliminar una persona del sistema
**Para** mantener solo registros activos

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar persona existente
  Dado que existe persona con ID 1
  Cuando el usuario elimina la persona ID 1
  Entonces el sistema marca eliminado = TRUE
  Y muestra "Persona eliminada exitosamente."

Escenario: Eliminar persona inexistente
  Dado que no existe persona con ID 999
  Cuando el usuario intenta eliminar persona ID 999
  Entonces el sistema muestra "No se encontró persona con ID: 999"

Escenario: Persona eliminada no aparece en listados
  Dado que se eliminó persona ID 1
  Cuando el usuario lista todas las personas
  Entonces la persona ID 1 no aparece en los resultados
```

#### Reglas de Negocio Aplicables

- **RN-019**: Eliminación es lógica, no física
- **RN-020**: Se ejecuta `UPDATE personas SET eliminado = TRUE`
- **RN-021**: El domicilio asociado NO se elimina automáticamente
- **RN-022**: Se verifica `rowsAffected` para confirmar eliminación

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarPersona()` (líneas 121-130)
- **Servicio**: `PersonaServiceImpl.eliminar()`
- **DAO**: `PersonaDAO.eliminar()` con `DELETE_SQL`
- **Query**: `UPDATE personas SET eliminado = TRUE WHERE id = ?`

---

## Épica 2: Gestión de Domicilios

### HU-006: Crear Domicilio Independiente

**Como** usuario del sistema
**Quiero** crear un domicilio sin asociarlo a ninguna persona
**Para** tener domicilios disponibles para asignación posterior

#### Criterios de Aceptación

```gherkin
Escenario: Crear domicilio válido
  Dado que el usuario selecciona "Crear domicilio"
  Cuando ingresa calle "Belgrano", número "456"
  Entonces el sistema crea el domicilio con ID autogenerado
  Y muestra "Domicilio creado exitosamente con ID: X"

Escenario: Crear domicilio con campos vacíos
  Dado que el usuario selecciona "Crear domicilio"
  Cuando deja la calle vacía
  Entonces el sistema muestra "La calle no puede estar vacía"
  Y no crea el domicilio
```

#### Reglas de Negocio Aplicables

- **RN-023**: Calle y número son obligatorios
- **RN-024**: Se eliminan espacios iniciales y finales
- **RN-025**: ID se genera automáticamente

#### Implementación Técnica

- **Clase**: `MenuHandler.crearDomicilioIndependiente()` (líneas 132-140)
- **Helper**: `MenuHandler.crearDomicilio()` (líneas 258-264) con trim()
- **Servicio**: `DomicilioServiceImpl.insertar()`
- **DAO**: `DomicilioDAO.insertar()` con `INSERT_SQL`

---

### HU-007: Listar Domicilios

**Como** usuario del sistema
**Quiero** ver todos los domicilios registrados
**Para** consultar direcciones disponibles

#### Criterios de Aceptación

```gherkin
Escenario: Listar domicilios existentes
  Dado que existen domicilios en el sistema
  Cuando el usuario selecciona "Listar domicilios"
  Entonces el sistema muestra ID, calle y número de cada domicilio
  Y solo muestra domicilios con eliminado = FALSE

Escenario: No hay domicilios
  Dado que no existen domicilios activos
  Cuando el usuario lista domicilios
  Entonces el sistema muestra "No se encontraron domicilios."
```

#### Reglas de Negocio Aplicables

- **RN-026**: Solo se listan domicilios con `eliminado = FALSE`
- **RN-027**: Se muestran tanto domicilios asociados como independientes

#### Implementación Técnica

- **Clase**: `MenuHandler.listarDomicilios()` (líneas 142-155)
- **Servicio**: `DomicilioServiceImpl.getAll()`
- **DAO**: `DomicilioDAO.getAll()` con `SELECT_ALL_SQL`
- **Query**: `SELECT * FROM domicilios WHERE eliminado = FALSE`

---

### HU-008: Eliminar Domicilio por ID (Operación Peligrosa)

**Como** usuario del sistema
**Quiero** eliminar un domicilio directamente por su ID
**Para** remover direcciones no utilizadas

⚠️ **ADVERTENCIA**: Esta operación puede dejar referencias huérfanas si el domicilio está asociado a una persona

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar domicilio no asociado
  Dado que existe domicilio ID 5 sin personas asociadas
  Cuando el usuario elimina domicilio ID 5
  Entonces el sistema marca eliminado = TRUE
  Y muestra "Domicilio eliminado exitosamente."

Escenario: Eliminar domicilio asociado (problema)
  Dado que existe domicilio ID 1 asociado a persona ID 10
  Cuando el usuario elimina domicilio ID 1 por esta opción
  Entonces el sistema marca el domicilio como eliminado
  Pero la persona ID 10 mantiene domicilio_id = 1
  Y el LEFT JOIN retornará NULL para el domicilio
  Y queda una referencia huérfana en la base de datos
```

#### Reglas de Negocio Aplicables

- **RN-028**: Eliminación es lógica
- **RN-029**: ⚠️ NO verifica si está asociado a persona
- **RN-030**: Puede causar referencias huérfanas
- **RN-031**: Usar HU-010 como alternativa segura

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarDomicilioPorId()` (líneas 187-196)
- **Servicio**: `DomicilioServiceImpl.eliminar()`
- **DAO**: `DomicilioDAO.eliminar()`
- **Limitación**: No verifica `personas.domicilio_id` antes de eliminar

---

### HU-009: Actualizar Domicilio por ID

**Como** usuario del sistema
**Quiero** actualizar un domicilio usando su ID
**Para** corregir direcciones incorrectas

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar calle de domicilio
  Dado que existe domicilio ID 1 con calle "San Martín"
  Cuando el usuario actualiza domicilio ID 1
  Y escribe "Belgrano" en calle
  Y presiona Enter en número
  Entonces el sistema actualiza solo la calle
  Y mantiene el número sin cambios

Escenario: Actualizar domicilio inexistente
  Dado que no existe domicilio ID 999
  Cuando el usuario intenta actualizarlo
  Entonces el sistema muestra "Domicilio no encontrado."
```

#### Reglas de Negocio Aplicables

- **RN-032**: Se permite actualizar cualquier domicilio por ID
- **RN-033**: Campos vacíos mantienen valor original
- **RN-034**: La actualización afecta a todas las personas asociadas

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarDomicilioPorId()` (líneas 157-185)
- **Servicio**: `DomicilioServiceImpl.actualizar()`
- **DAO**: `DomicilioDAO.actualizar()` con `UPDATE_SQL`
- **Pattern**: Usa `.trim()` y verifica `isEmpty()`

---

## Épica 3: Operaciones Asociadas

### HU-010: Eliminar Domicilio por Persona (Operación Segura)

**Como** usuario del sistema
**Quiero** eliminar el domicilio asociado a una persona específica
**Para** remover la dirección sin dejar referencias huérfanas

✅ **RECOMENDADO**: Esta es la forma segura de eliminar un domicilio asociado

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar domicilio de persona correctamente
  Dado que persona ID 1 tiene domicilio ID 5
  Cuando el usuario elimina domicilio por persona ID 1
  Entonces el sistema primero actualiza persona.domicilio_id = NULL
  Y luego marca domicilio ID 5 como eliminado = TRUE
  Y muestra "Domicilio eliminado exitosamente y referencia actualizada."

Escenario: Persona sin domicilio
  Dado que persona ID 1 no tiene domicilio
  Cuando el usuario intenta eliminar su domicilio
  Entonces el sistema muestra "La persona no tiene domicilio asociado."
  Y no ejecuta ninguna operación

Escenario: Validación de pertenencia
  Dado que persona ID 1 tiene domicilio ID 5
  Cuando el servicio intenta eliminar domicilio ID 7 de persona ID 1
  Entonces el sistema muestra "El domicilio no pertenece a esta persona"
  Y no elimina nada
```

#### Reglas de Negocio Aplicables

- **RN-035**: Se actualiza la FK antes de eliminar
- **RN-036**: Se valida que el domicilio pertenezca a la persona
- **RN-037**: Operación en dos pasos: UPDATE persona → DELETE domicilio
- **RN-038**: Sin transacción explícita, pero orden correcto previene huérfanos

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarDomicilioPorPersona()` (líneas 234-256)
- **Servicio**: `PersonaServiceImpl.eliminarDomicilioDePersona()` (líneas 88-105)
- **Flujo**:
  1. Valida IDs > 0
  2. Obtiene persona por ID
  3. Valida que tenga domicilio
  4. Valida que el domicilio_id coincida
  5. `persona.setDomicilio(null)`
  6. `personaDAO.actualizar(persona)` → domicilio_id = NULL
  7. `domicilioServiceImpl.eliminar(domicilioId)` → eliminado = TRUE

#### Comparación HU-008 vs HU-010

| Aspecto | HU-008 (Por ID) | HU-010 (Por Persona) |
|---------|-----------------|----------------------|
| **Validación** | No verifica asociación | Verifica pertenencia |
| **Referencias** | Puede dejar huérfanas | Actualiza FK primero |
| **Seguridad** | ⚠️ Peligroso | ✅ Seguro |
| **Uso recomendado** | Solo para domicilios independientes | Para domicilios asociados |

---

### HU-011: Actualizar Domicilio por Persona

**Como** usuario del sistema
**Quiero** actualizar el domicilio de una persona específica
**Para** modificar su dirección sin afectar otros domicilios

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar domicilio de persona
  Dado que persona ID 1 tiene domicilio con calle "San Martín"
  Cuando el usuario actualiza domicilio por persona ID 1
  Y escribe "Belgrano" en calle
  Entonces el sistema actualiza el domicilio de esa persona
  Y muestra "Domicilio actualizado exitosamente."

Escenario: Persona sin domicilio
  Dado que persona ID 1 no tiene domicilio
  Cuando el usuario intenta actualizar su domicilio
  Entonces el sistema muestra "La persona no tiene domicilio asociado."
```

#### Reglas de Negocio Aplicables

- **RN-039**: Solo actualiza el domicilio de la persona especificada
- **RN-040**: Si varias personas comparten domicilio, todas se afectan
- **RN-041**: Se requiere que la persona tenga domicilio asociado

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarDomicilioPorPersona()` (líneas 198-232)
- **Servicio**: `DomicilioServiceImpl.actualizar()`
- **Flujo**:
  1. Obtiene persona por ID
  2. Valida que tenga domicilio (`p.getDomicilio() != null`)
  3. Captura nuevos valores con trim()
  4. Actualiza objeto domicilio
  5. Llama a `domicilioService.actualizar()`

---

## Reglas de Negocio

### Validación de Datos (RN-001 a RN-013)

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-001 | Nombre, apellido y DNI son obligatorios | `PersonaServiceImpl.validatePersona()` |
| RN-002 | DNI debe ser único en el sistema | `PersonaServiceImpl.validateDniUnique()` + DB UNIQUE constraint |
| RN-003 | Espacios iniciales/finales se eliminan | `.trim()` en MenuHandler |
| RN-004 | IDs se generan automáticamente | AUTO_INCREMENT en BD |
| RN-005 | Domicilio es opcional en persona | FK nullable |
| RN-009 | Búsquedas son case-insensitive | MySQL default collation |
| RN-010 | Búsqueda con LIKE %texto% | `PersonaDAO.SEARCH_BY_NAME_SQL` |
| RN-013 | No se permiten búsquedas vacías | Validación en service |

### Operaciones de Base de Datos (RN-014 a RN-027)

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-014 | Validación antes de persistir | Service layer valida antes de llamar DAO |
| RN-015 | Campos vacíos mantienen valor original | Pattern `if (!x.isEmpty())` |
| RN-016 | ID > 0 requerido para update/delete | Service valida `id > 0` |
| RN-019 | Eliminación es lógica | `UPDATE tabla SET eliminado = TRUE` |
| RN-020 | Soft delete en UPDATE no DELETE | Todos los DAOs usan UPDATE |
| RN-022 | Verificación de rowsAffected | Todos los UPDATE/DELETE verifican |
| RN-026 | Solo listar no eliminados | `WHERE eliminado = FALSE` en todas las queries |

### Integridad Referencial (RN-028 a RN-041)

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-028 | HU-008 no verifica referencias | `DomicilioDAO.eliminar()` sin validación |
| RN-029 | Puede causar referencias huérfanas | FK apunta a domicilio eliminado |
| RN-030 | HU-010 es alternativa segura | `PersonaServiceImpl.eliminarDomicilioDePersona()` |
| RN-035 | Actualizar FK antes de eliminar | Orden: persona.setDomicilio(null) → actualizar → eliminar |
| RN-036 | Validar pertenencia | Verifica `persona.getDomicilio().getId() == domicilioId` |
| RN-037 | Operación en dos pasos | UPDATE personas → UPDATE domicilios |
| RN-040 | Compartir domicilio afecta a todos | Un domicilio puede estar en varias personas |

### Transacciones y Coordinación (RN-042 a RN-051)

| Código | Regla | Implementación |
|--------|-------|----------------|
| RN-042 | PersonaService coordina con DomicilioService | `PersonaServiceImpl` usa `DomicilioServiceImpl` |
| RN-043 | Insertar domicilio antes que persona | `PersonaServiceImpl.insertar()` líneas 28-34 |
| RN-044 | Try-with-resources para recursos | Todas las conexiones, statements, resultsets |
| RN-045 | PreparedStatements para prevenir SQL injection | 100% de queries |
| RN-046 | LEFT JOIN para relación opcional | Todas las queries de PersonaDAO |
| RN-047 | NULL seguro en FK | `setDomicilioId()` usa `stmt.setNull(Types.INTEGER)` |
| RN-048 | TransactionManager soporta rollback | AutoCloseable con rollback en close() |
| RN-049 | Equals/HashCode de Persona basado en DNI | DNI es único |
| RN-050 | Equals/HashCode de Domicilio basado en calle+numero | Comparación semántica |
| RN-051 | Scanner se cierra al salir | `AppMenu.run()` línea 37 |

---

## Modelo de Datos

### Diagrama Entidad-Relación

```
┌─────────────────────────────────┐
│           personas              │
├─────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT       │
│ nombre: VARCHAR(50) NOT NULL    │
│ apellido: VARCHAR(50) NOT NULL  │
│ dni: VARCHAR(20) NOT NULL UNIQUE│
│ domicilio_id: INT FK NULL       │
│ eliminado: BOOLEAN DEFAULT FALSE│
└──────────────┬──────────────────┘
               │ 0..1
               │
               │ FK
               │
               ▼
┌──────────────────────────────────┐
│         domicilios               │
├──────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT        │
│ calle: VARCHAR(100) NOT NULL     │
│ numero: VARCHAR(10) NOT NULL     │
│ eliminado: BOOLEAN DEFAULT FALSE │
└──────────────────────────────────┘
```

### Constraints y Validaciones

```sql
-- Constraint en base de datos
ALTER TABLE personas ADD CONSTRAINT uk_dni UNIQUE (dni);

-- FK nullable permite personas sin domicilio
ALTER TABLE personas ADD CONSTRAINT fk_domicilio
  FOREIGN KEY (domicilio_id) REFERENCES domicilios(id);

-- Índices recomendados para performance
CREATE INDEX idx_persona_nombre ON personas(nombre);
CREATE INDEX idx_persona_apellido ON personas(apellido);
CREATE INDEX idx_persona_eliminado ON personas(eliminado);
CREATE INDEX idx_domicilio_eliminado ON domicilios(eliminado);
```

### Queries Principales

#### SELECT con JOIN
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.domicilio_id,
       d.id AS dom_id, d.calle, d.numero
FROM personas p
LEFT JOIN domicilios d ON p.domicilio_id = d.id
WHERE p.eliminado = FALSE
```

#### Búsqueda por nombre/apellido
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.domicilio_id,
       d.id AS dom_id, d.calle, d.numero
FROM personas p
LEFT JOIN domicilios d ON p.domicilio_id = d.id
WHERE p.eliminado = FALSE
  AND (p.nombre LIKE ? OR p.apellido LIKE ?)
```

#### Búsqueda por DNI
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.domicilio_id,
       d.id AS dom_id, d.calle, d.numero
FROM personas p
LEFT JOIN domicilios d ON p.domicilio_id = d.id
WHERE p.eliminado = FALSE AND p.dni = ?
```

---

## Flujos Técnicos Críticos

### Flujo 1: Crear Persona con Domicilio

```
Usuario (MenuHandler)
    ↓ captura datos con .trim()
PersonaServiceImpl.insertar()
    ↓ validatePersona()
    ↓ validateDniUnique(dni, null)
    ↓ if domicilio != null && domicilio.id == 0:
DomicilioServiceImpl.insertar()
    ↓ validateDomicilio()
    ↓ DomicilioDAO.insertar()
        ↓ INSERT domicilios
        ↓ obtiene ID autogenerado
        ↓ domicilio.setId(generatedId)
    ↓ return
PersonaServiceImpl continúa
    ↓ PersonaDAO.insertar(persona)
        ↓ INSERT personas (con domicilio_id)
        ↓ obtiene ID autogenerado
        ↓ persona.setId(generatedId)
    ↓ return
Usuario recibe: "Persona creada exitosamente con ID: X"
```

### Flujo 2: Eliminar Domicilio Seguro (HU-010)

```
Usuario (MenuHandler)
    ↓ ingresa personaId
PersonaServiceImpl.eliminarDomicilioDePersona(personaId, domicilioId)
    ↓ valida personaId > 0 && domicilioId > 0
    ↓ persona = personaDAO.getById(personaId)
    ↓ if persona == null: throw "Persona no encontrada"
    ↓ if persona.getDomicilio() == null: throw "Sin domicilio"
    ↓ if persona.getDomicilio().getId() != domicilioId:
        throw "Domicilio no pertenece a esta persona"
    ↓ persona.setDomicilio(null)
    ↓ personaDAO.actualizar(persona)
        ↓ UPDATE personas SET domicilio_id = NULL WHERE id = personaId
    ↓ domicilioServiceImpl.eliminar(domicilioId)
        ↓ UPDATE domicilios SET eliminado = TRUE WHERE id = domicilioId
    ↓ return
Usuario recibe: "Domicilio eliminado exitosamente y referencia actualizada."
```

### Flujo 3: Validación DNI Único en Update

```
Usuario actualiza persona
    ↓ PersonaServiceImpl.actualizar(persona)
        ↓ validatePersona(persona)
        ↓ validateDniUnique(persona.getDni(), persona.getId())
            ↓ existente = personaDAO.buscarPorDni(dni)
            ↓ if existente != null:
                ↓ if personaId == null || existente.getId() != personaId:
                    ✗ throw "Ya existe una persona con el DNI: X"
                ↓ else:
                    ✓ return (es la misma persona, OK)
            ↓ else:
                ✓ return (DNI no existe, OK)
        ↓ personaDAO.actualizar(persona)
    ↓ return
```

---

## Resumen de Operaciones del Menú

| Opción | Operación | Handler | HU |
|--------|-----------|---------|---|
| 1 | Crear persona | `crearPersona()` | HU-001 |
| 2 | Listar personas | `listarPersonas()` | HU-002, HU-003 |
| 3 | Actualizar persona | `actualizarPersona()` | HU-004 |
| 4 | Eliminar persona | `eliminarPersona()` | HU-005 |
| 5 | Crear domicilio | `crearDomicilioIndependiente()` | HU-006 |
| 6 | Listar domicilios | `listarDomicilios()` | HU-007 |
| 7 | Actualizar domicilio por ID | `actualizarDomicilioPorId()` | HU-009 |
| 8 | Eliminar domicilio por ID | `eliminarDomicilioPorId()` | HU-008 ⚠️ |
| 9 | Actualizar domicilio por persona | `actualizarDomicilioPorPersona()` | HU-011 |
| 10 | Eliminar domicilio por persona | `eliminarDomicilioPorPersona()` | HU-010 ✅ |
| 0 | Salir | Sets `running = false` | - |

---

## Documentación Relacionada

- **README.md**: Guía de instalación, configuración y uso
- **CLAUDE.md**: Documentación técnica para desarrollo, arquitectura detallada, patrones de código

---

**Versión**: 1.0
**Total Historias de Usuario**: 11
**Total Reglas de Negocio**: 51
**Arquitectura**: 4 capas (Main → Service → DAO → Models)
