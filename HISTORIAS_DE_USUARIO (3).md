# Historias de Usuario - Sistema de Gestión de Pacientes e Historias Clínicas

Especificaciones funcionales completas del sistema CRUD de pacientes e historias clínicas.

## Tabla de Contenidos

- [Épica 1: Gestión de Pacientes](#épica-1-gestión-de-pacientes)
- [Épica 2: Gestión de Historias Clínicas](#épica-2-gestión-de-historias clínicas)
- [Épica 3: Operaciones Asociadas](#épica-3-operaciones-asociadas)
- [Reglas de Negocio](#reglas-de-negocio)
- [Modelo de Datos](#modelo-de-datos)

---

## Épica 1: Gestión de Pacientes

### HU-001: Crear Paciente

**Como** usuario del sistema
**Quiero** crear un registro de pacientes con sus datos básicos
**Para** almacenar información de los mismos en la base de datos

#### Criterios de Aceptación

```gherkin
Escenario: Crear paciente sin historia clínica
No se puede debido a que cuando se crea un paciente,
inmediatamente se asigna una historia clínica

Escenario: Crear paciente con historia clínica
  Dado que el usuario selecciona "Crear paciente"
  Cuando ingresa nombre "Juana", apellido "Albarracin", DNI "87654321"
  Y responde "s" a agregar historia clínica
  Y ingresa descripción "Historia clínica inicial"
  Entonces el sistema crea la historia clínica primero
  Y luego crea el paciente con referencia a la historia clínica
  Y muestra "Paciente creado exitosamente con ID: X"

Escenario: Intento de crear paciente con DNI duplicado
  Dado que existe un paciente con DNI "12345678"
  Cuando el usuario intenta crear un paciente con el mismo DNI
  Entonces el sistema muestra "Ya existe un paciente con el DNI: 12345678"
  Y no crea el registro

Escenario: Intento de crear paciente con campos vacíos
  Dado que el usuario selecciona "Crear paciente"
  Cuando se deja el nombre y apellido vacío (solo espacios o enter)
  Consecuentemente el sistema muestra "El nombre y el apellido del paciente son obligatorios"
  Y no crea el registro
```

#### Reglas de Negocio Aplicables

- **RN-001**: Nombre y apellido son obligatorios
- **RN-002**: El DNI debe ser único en el sistema
- **RN-003**: Espacios iniciales y finales se eliminan automáticamente
- **RN-004**: El ID se genera automáticamente

#### Implementación Técnica

- **Clase**: `MenuHandler.crearPaciente()` (líneas 25-47)
- **Servicio**: `PacienteServiceImpl.insertar()` (líneas 24-37)
- **Validación**: `PacienteServiceImpl.validatePaciente()` + `validateDniUnique()`
- **Flujo**:
  1. Captura entrada con `.trim()`
  2. Crea objeto Paciente
  3. Si hay historia clínica e `id == 0`, inserta historia clínica primero
  4. Inserta paciente con FK al historia clínica
  5. Genera ID automático con `Statement.RETURN_GENERATED_KEYS`

---

### HU-002: Listar todos los pacientes

**Como** usuario del sistema
**Quiero** ver un listado de todos los pacientes registrados
**Para** consultar la información almacenada

#### Criterios de Aceptación

```gherkin
Escenario: Listar todos los pacientes con historia clínica
  Dado que existen pacientes en el sistema
  Cuando el usuario selecciona "Listar pacientes"
  Y elige opción "1" (listar todos)
  Entonces el sistema muestra todos los pacientes no eliminados
  Y para cada paciente con historia clínica muestra "Historia Clinica: [n° de historia] [antecedentes] etc."

Escenario: Listar pacientes sin historia clínica
No se puede ya que se asigna una historia clinica en la creación del paciente.

Escenario: No hay pacientes en el sistema
  Dado que no existen pacientes activos
  Cuando el usuario lista todos los pacientes
  Entonces el sistema muestra "No se encontraron pacientes."
```

#### Reglas de Negocio Aplicables

- **RN-005**: Solo se listan pacientes con `eliminado = FALSE`
- **RN-006**: La historia clínica se obtiene mediante LEFT JOIN

#### Implementación Técnica

- **Clase**: `MenuHandler.listarPacientes()` (líneas 49-82, subopción 1)
- **Servicio**: `PacienteServiceImpl.getAll()`
- **DAO**: `PacienteDAO.getAll()` con `SELECT_ALL_SQL`
- **Query**:
  ```sql
  SELECT   p.id, 
  p.nombre, 
  p.apellido, 
  p.dni, 
  p.historiaClinica_id,
  hc.id AS hc_id, 
  hc.nroHistoria, 
  hc.grupoSanguineo, 
  hc.antecedentes, 
  hc.medicacionActual, 
  hc.observaciones
  FROM pacientes p
  LEFT JOIN historia_clinica hc ON p.historiaClinica_id = hc.id
  WHERE p.eliminado = FALSE
  ```

---

### HU-003: Buscar Pacientes por DNI o id

**Como** usuario del sistema
**Quiero** buscar pacientes por dni o id
**Para** encontrar rápidamente un paciente específico

#### Criterios de Aceptación

```gherkin
Escenario: Buscar por nombre o DNI con coincidencia parcial
  Dado que existen pacientes con nombre "Juan Pérez" (DNI: 12345678) y "Juana García" (DNI: 87654321)
  Cuando el usuario busca por "Jua"
  Entonces el sistema muestra ambos pacientes
Escenario: Buscar por ID exacto
  Dado que existen pacientes con ID 101 ("Juan Pérez") y 102 ("María Pérez")
  Cuando el usuario busca por "101"
  Entonces el sistema muestra al paciente con ID 101
Escenario: Buscar por DNI exacto
  Dado que existen pacientes con DNI 12345678 ("Juan Pérez") y 87654321 ("María Pérez")
  Cuando el usuario busca por "87654321"
  Entonces el sistema muestra al paciente con ese DNI
Escenario: Búsqueda sin resultados
  Dado que no existen pacientes con nombre, DNI o ID que contengan "Rodríguez"
  Cuando el usuario busca por "Rodríguez"
  Entonces el sistema muestra "No se encontraron pacientes."
Escenario: Búsqueda con espacios
  Dado que el usuario busca por "  Juan  " (con espacios)
  Cuando se ejecuta la búsqueda
  Entonces el sistema elimina espacios y busca por "Juan" en nombre, DNI o ID
```

#### Reglas de Negocio Aplicables

- **RN-007**: Busca en DNI OR id
- **RN-008**: Espacios se eliminan automáticamente
- **RN-009**: No se permiten búsquedas vacías

#### Implementación Técnica

- **Clase**: `MenuHandler.listarPacientes()` (líneas 49-82, subopción 2, línea 59 con trim())
- **Servicio**: `PacienteServiceImpl.buscarPorDNI()`
- **DAO**: `PacienteDAO.buscarPorDNI()` con `SEARCH_BY_NAME_SQL`
- **Query**:
  ```sql
  WHEREp.eliminado = FALSE AND (
  CAST(p.id AS TEXT) LIKE ? OR
  p.dni LIKE ?)
  ```
- **Parámetros**: `%filtro%` en ambos placeholders

---

### HU-004: Actualizar Paciente

**Como** usuario del sistema
**Quiero** modificar los datos de un paciente existente
**Para** mantener la información actualizada

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar solo apellido por DNI
  Dado que existe un paciente con DNI "12345678", apellido "Pérez"
  Cuando el usuario actualiza el paciente con DNI "12345678"
  Y presiona Enter en nombre e ID
  Y escribe "González" en apellido
  Entonces el sistema actualiza solo el apellido
  Y mantiene nombre e ID sin cambios

Escenario: Actualizar con DNI duplicado
  Dado que existen pacientes con DNI "111" y "222"
  Cuando el usuario intenta cambiar el DNI del paciente con DNI "222" a "111"
  Entonces el sistema muestra "Ya existe un paciente con el DNI: 111"
  Y no actualiza el registro

Escenario: Actualizar con mismo DNI
  Dado que existe un paciente con DNI "12345678"
  Cuando el usuario actualiza otros campos manteniendo el mismo DNI
  Entonces el sistema permite la actualización
  Y no muestra error de DNI duplicado

Escenario: Agregar historia clínica a paciente sin historia clínica (por ID o DNI)
  Dado que el paciente con DNI "12345678" no tiene historia clínica
  Cuando el usuario actualiza el paciente por DNI "12345678"
  Y responde "s" a agregar historia clínica
  E ingresa los campos correspondientes a historia
  Entonces el sistema crea la historia clínica
  Y la asocia al paciente
```

#### Reglas de Negocio Aplicables

- **RN-010**: Se valida DNI único excepto para la misma paciente
- **RN-011**: Campos vacíos (Enter) mantienen valor original
- **RN-012**: Se requiere ID > 0 para actualizar
- **RN-013**: Se puede agregar o actualizar historia clínica durante actualización
- **RN-014**: Trim se aplica antes de validar si el campo está vacío

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarPaciente()` (líneas 84-119)
- **Servicio**: `PacienteServiceImpl.actualizar()`
- **Validación**: `validateDniUnique(dni, pacienteId)` permite mismo DNI
- **Pattern**:
  ```java
  String nombre = scanner.nextLine().trim();
  if (!nombre.isEmpty()) {
      p.setNombre(nombre);
  }
  ```

---

### HU-005: Eliminar Paciente

**Como** usuario del sistema
**Quiero** eliminar un paciente del sistema
**Para** mantener solo registros activos

#### Criterios de Aceptación

```gherkin
Escenario: Eliminar paciente existente
  Dado que existe paciente con ID 1
  Cuando el usuario elimina el pacienteID 1
  Entonces el sistema marca eliminado = TRUE
  Y muestra "Paciente eliminada exitosamente."

Escenario: Eliminar paciente inexistente
  Dado que no existe paciente con ID 999
  Cuando el usuario intenta eliminar paciente ID 999
  Entonces el sistema muestra "No se encontró paciente con ID: 999"

Escenario: Paciente eliminada no aparece en listados
  Dado que se eliminó paciente ID 1
  Cuando el usuario lista todos los pacientes
  Entonces el pacienteID 1 no aparece en los resultados
```

#### Reglas de Negocio Aplicables

- **RN-015**: Eliminación es lógica, no física
- **RN-016**: Se ejecuta `UPDATE paciente SET eliminado = TRUE`
- **RN-017**: La historiaclínica asociada NO se elimina automáticamente
- **RN-018**: Se verifica `rowsAffected` para confirmar eliminación

#### Implementación Técnica

- **Clase**: `MenuHandler.eliminarPaciente()` (líneas 121-130)
- **Servicio**: `PacienteServiceImpl.eliminar()`
- **DAO**: `PacienteDAO.eliminar()` con `DELETE_SQL`
- **Query**: `UPDATE pacientes SET eliminado = TRUE WHERE id = ?`

---

## Épica 2: Gestión de Historias Clínicas

### HU-006: Crear Historia Clínica Independiente

**Como** usuario del sistema
**Quiero** crear una historia clínica sin asociarlo a ningun paciente
**Para** tener historias clínicas disponibles para asignación posterior

#### Criterios de Aceptación

```gherkin
Escenario: Crear historia clínica válido
  Dado que el usuario selecciona "Crear historia clínica"
  Cuando ingresa por ejemplo grupo sanguineo "A+", número DE historia "12", antecedentes "Diabético", medicación actual "Insulina" observaciones "Diabétes tipo 2 insulino-dependiente"
  Entonces el sistema crea la historia clínica con ID autogenerado
  Y muestra "Historia Clínica creada exitosamente con ID: X"

Escenario: Crear historia clínica con campos vacíos
  Dado que el usuario selecciona "Crear historia clínica"
  Cuando deja el numero de historia vacía
  Entonces el sistema muestra "El número de historoa no puede estar vacío"
  Y no crea la historia clínica
```

#### Reglas de Negocio Aplicables

- **RN-019**: Número de historia es obligatorio
- **RN-020**: Se eliminan espacios iniciales y finales
- **RN-021**: ID se genera automáticamente

#### Implementación Técnica

- **Clase**: `MenuHandler.crearHistoriaClinicaIndependiente()` (líneas 132-140)
- **Helper**: `MenuHandler.crearHistoriaClinica()` (líneas 258-264) con trim()
- **Servicio**: `HistoriaClinicaServiceImpl.insertar()`
- **DAO**: `HistoriaClinicaDAO.insertar()` con `INSERT_SQL`


### HU-007: Actualizar Historia Clínica por ID

**Como** usuario del sistema
**Quiero** actualizar una historia clínica usando su ID
**Para** corregir direcciones incorrectas

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar antecedentes de historia clínica
  Dado que existe historia clínica ID 1 con antecedente "Hipocondriaco"
  Cuando el usuario actualiza historia clínica ID 1
  Y escribe "Principio de Psicosis" en antecedentes
  Y presiona Enter en número
  Entonces el sistema actualiza solo el antecedente
  Y mantiene el número de historia sin cambios

Escenario: Actualizar historia clínica inexistente
  Dado que no existe historia clínica ID 999
  Cuando el usuario intenta actualizarlo
  Entonces el sistema muestra "Historia Clínica no encontrada."
```

#### Reglas de Negocio Aplicables

- **RN-022**: Se permite actualizar cualquier historia clínica por ID
- **RN-023**: Campos vacíos mantienen valor original
- **RN-024**: La actualización afecta a todos los pacientes asociadas

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarHistoriaClínica PorId()` (líneas 157-185)
- **Servicio**: `HistoriaClinicaServiceImpl.actualizar()`
- **DAO**: `HistoriaClinicaDAO.actualizar()` con `UPDATE_SQL`
- **Pattern**: Usa `.trim()` y verifica `isEmpty()`

---

## Épica 3: Operaciones Asociadas

### HU-008: Actualizar Historia Clínica por Paciente

**Como** usuario del sistema
**Quiero** actualizar la historia clínica de un paciente específico
**Para** modificar su dirección sin afectar otros historias clínicas

#### Criterios de Aceptación

```gherkin
Escenario: Actualizar historia clínica de paciente
  Dado que paciente ID 1 tiene historia clínica con antecedentes "Hipocondriaco"
  Cuando el usuario actualiza historia clínica por paciente ID 1
  Y escribe "Pricipio de Psicosis" en dicho campo
  Entonces el sistema actualiza la historia clínica de ese paciente
  Y muestra "Historia Clínica actualizada exitosamente."

Escenario: Paciente sin historia clínica
  Dado que paciente ID 1 no tiene historia clínica
  Cuando el usuario intenta actualizar su historia clínica
  Entonces el sistema muestra "El paciente no tiene historia clínica asociada."
```

#### Reglas de Negocio Aplicables

- **RN-025**: Solo actualiza la historia clínica de el paciente especificado
- **RN-026**: Si varios pacientes comparten historia clínica, todas se afectan
- **RN-027**: Se requiere que el paciente tenga historia clínica asociada

#### Implementación Técnica

- **Clase**: `MenuHandler.actualizarHistoriaClínica PorPaciente()` (líneas 198-232)
- **Servicio**: `HistoriaClinicaServiceImpl.actualizar()`
- **Flujo**:
  1. Obtiene paciente por ID
  2. Valida que tenga historia clínica (`p.getHistoriaClinica() != null`)
  3. Captura nuevos valores con trim()
  4. Actualiza objeto historia clínica
  5. Llama a `historia clínicaService.actualizar()`

---


## Modelo de Datos

### Diagrama Entidad-Relación

```
┌────────────────────────────────────────┐
│             paciente                   │
├────────────────────────────────────────│
│ id: INT PK AUTO_INCREMENT              │
│ nombre: VARCHAR(50) NOT NULL           │
│ apellido: VARCHAR(50) NOT NULL         │
│ dni: VARCHAR(20) NOT NULL UNIQUE       │
│ fechaNacimiento: DATE NOT NULL         │                            
│ historiaClinica_id: INT FK NULL        │     
│ eliminado: BOOLEAN DEFAULT FALSE       │
└───────────────────┬────────────────────┘
                    │ 0..1
                    │
                    │ FK
                    │
                    ▼
┌────────────────────────────────────────┐
│         historia clinica               │
├────────────────────────────────────────┤
│ id: INT PK AUTO_INCREMENT              │         
│ nroHistoria: VARCHAR(10) NOT NULL      │
│ grupoSanguineo: VARCHAR(10) NOT NULL   │
│ antecedentes: VARCHAR(100) NOT NULL    │
│ medicacionActual: VARCHAR(50) NOT NULL │ 
│ observaciones: VARCHAR(200) NOT NULL   │                              
│ eliminado: BOOLEAN DEFAULT FALSE       │
└────────────────────────────────────────┘
```

### Constraints y Validaciones

```sql
-- Constraint en base de datos
ALTER TABLE pacientes ADD CONSTRAINT uk_dni UNIQUE (dni);

-- FK nullable permite pacientes sin historia clínica
ALTER TABLE pacientes ADD CONSTRAINT fk_historia clínica
  FOREIGN KEY (historia clínica_id) REFERENCES historias clínicas(id);

-- Índices recomendados para performance
CREATE INDEX idx_paciente_nombre ON pacientes(nombre);
CREATE INDEX idx_paciente_apellido ON pacientes(apellido);
CREATE INDEX idx_paciente_eliminado ON pacientes(eliminado);
CREATE INDEX idx_historia_clínica_eliminado ON historias clínicas(eliminado);
```

### Queries Principales

#### SELECT con JOIN
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.fechaNacimiento, p.historiaClinica_id,
       h.id AS historia_id, h.nroHistoria, h.grupoSanguineo, h.antecedentes, 
       h.medicacionActual, h.observaciones
FROM pacientes p
LEFT JOIN historias_clinicas h ON p.historiaClinica_id = h.id
WHERE p.eliminado = FALSE;
```
#### Búsqueda por DNI
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.fechaNacimiento, p.historiaClinica_id,
       h.id AS historia_id, h.nroHistoria, h.grupoSanguineo, h.antecedentes, 
       h.medicacionActual, h.observaciones
FROM pacientes p
LEFT JOIN historias_clinicas h ON p.historiaClinica_id = h.id
WHERE p.eliminado = FALSE AND p.dni = ?;
```
#### Búsqueda por id
```sql
SELECT p.id, p.nombre, p.apellido, p.dni, p.fechaNacimiento, p.historiaClinica_id,
       h.id AS historia_id, h.nroHistoria, h.grupoSanguineo, h.antecedentes, 
       h.medicacionActual, h.observaciones
FROM pacientes p
LEFT JOIN historias_clinicas h ON p.historiaClinica_id = h.id
WHERE p.eliminado = FALSE AND p.id = ?;
```

## Flujos Técnicos Críticos

### Flujo 1: Crear Paciente con HistoriaClinicaService

```
Usuario (MenuHandler)
    ↓ captura datos con .trim()
PacienteServiceImpl.insertar()
    ↓ validatePaciente()
    ↓ validateDniUnique(dni, null)
    ↓ if historia clínica != null && historia clínica.id == 0:
HistoriaClinicaServiceImpl.insertar()
    ↓ validateHistoriaClinica()
    ↓ HistoriaClinicaDAO.insertar()
        ↓ INSERT historias clínicas
        ↓ obtiene ID autogenerado
        ↓ historiaClínica.setId(generatedId)
    ↓ return
PacienteServiceImpl continúa
    ↓ PacienteDAO.insertar(paciente)
        ↓ INSERT pacientes (con historia clínica_id)
        ↓ obtiene ID autogenerado
        ↓ paciente.setId(generatedId)
    ↓ return
Usuario recibe: "Paciente creada exitosamente con ID: X"
```

### Flujo 2: Eliminar Historia Clínica Seguro ()

```
Usuario (MenuHandler)
    ↓ ingrese pacienteId
PacienteServiceImpl.eliminarHistoriaClínica DePaciente(pacienteId, historia clínicaId)
    ↓ valida pacienteId > 0 && historia clínicaId > 0
    ↓ paciente = pacienteDAO.getById(pacienteId)
    ↓ if paciente == null: throw "Paciente no encontrada"
    ↓ if paciente.getHistoriaClinica() == null: throw "Sin historia clínica"
    ↓ if paciente.getHistoriaClinica().getId() != historia clínicaId:
        throw "HistoriaClinicaService no pertenece a este paciente"
    ↓ paciente.setHistoriaClínica (null)
    ↓ pacienteDAO.actualizar(paciente)
        ↓ UPDATE pacientes SET historia clínica_id = NULL WHERE id = pacienteId
    ↓ historia clínicaServiceImpl.eliminar(historia clínicaId)
        ↓ UPDATE historias clínicas SET eliminado = TRUE WHERE id = historia clínicaId
    ↓ return
Usuario recibe: "Historia Clínica eliminada exitosamente y referencia actualizada."
```

### Flujo 3: Validación DNI Único en Update

```
Usuario actualiza paciente
    ↓ PacienteServiceImpl.actualizar(paciente)
        ↓ validatePaciente(paciente)
        ↓ validateDniUnique(paciente.getDni(), paciente.getId())
            ↓ existente = pacienteDAO.buscarPorDni(dni)
            ↓ if existente != null:
                ↓ if pacienteId == null || existente.getId() != pacienteId:
                    ✗ throw "Ya existe un paciente con el DNI: X"
                ↓ else:
                    ✓ return (es la misma paciente, OK)
            ↓ else:
                ✓ return (DNI no existe, OK)
        ↓ pacienteDAO.actualizar(paciente)
    ↓ return
```

---

## Resumen de Operaciones del Menú

| Opción | Operación | Handler | HU |
|--------|-----------|---------|---|
| 1 | Crear paciente | `crearPaciente()` | HU-001 |
| 2 | Listar pacientes | `listarPacientes()` | HU-002 |
| 3 | Actualizar paciente y/o HC| `actualizarPaciente()` | HU-004 HU-008 |
| 4 | Eliminar paciente(Baja Lógica) | `eliminarPaciente()` | HU-005 |
| 5 | Buscar Paciente por DNI | `buscarPacientePorDNI()`| HU-003 |
| 6 | Buscar Paciente por ID | `buscarPacientePorId()` | HU-006 |
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
