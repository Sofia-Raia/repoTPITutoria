package Main;

import entities.HistoriaClinica;
import entities.Paciente;
import entities.HistoriaClinica.GrupoSanguineo;
import exceptions.ServiceException;
import Service.PacienteServiceImpl;
import Service.HistoriaClinicaServiceImpl; // Importar el service de HC

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException; // Aunque usamos parseInt, es bueno tenerlo
import java.util.List;
import java.util.Scanner;

/**
 * Maneja la lógica de interacción del menú (captura de datos y llamadas al Service).
 */
public class MenuHandler {
    
    private final Scanner scanner;
    private final PacienteServiceImpl pacienteService;
    private final HistoriaClinicaServiceImpl historiaClinicaService; // Servicio de HC

    // Constructor corregido (acepta 3 argumentos)
    public MenuHandler(Scanner scanner, PacienteServiceImpl pacienteService, HistoriaClinicaServiceImpl historiaClinicaService) {
        if (scanner == null || pacienteService == null || historiaClinicaService == null) {
            throw new IllegalArgumentException("Scanner y Services no pueden ser null");
        }
        this.scanner = scanner;
        this.pacienteService = pacienteService;
        this.historiaClinicaService = historiaClinicaService;
    }

    // --- Métodos de Interacción ---

    /**
     * Lógica para la Opción 1: Crear Paciente (Transacción 1:1)
     */
    public void crearPaciente() {
        System.out.println("\n--- 📝 Creación de Paciente y HC (Transacción) ---");
        try {
            Paciente p = new Paciente();
            
            // 1. Datos del Paciente
            System.out.print("Nombre: "); p.setNombre(scanner.nextLine());
            System.out.print("Apellido: "); p.setApellido(scanner.nextLine());
            System.out.print("DNI: "); p.setDni(scanner.nextLine());
            
            System.out.print("Fecha Nacimiento (AAAA-MM-DD, Enter para omitir): "); 
            String fechaStr = scanner.nextLine();
            if (!fechaStr.trim().isEmpty()) {
                p.setFechaNacimiento(LocalDate.parse(fechaStr));
            }

            // 2. Datos de Historia Clinica (Clase B)
            HistoriaClinica hc = crearHistoriaClinica();
            
            p.setHistoriaClinica(hc); // 🔑 Asociar HC al Paciente (Relación 1:1)

            // 3. Llamada al Service para la transacción
            pacienteService.insertar(p);
            
            System.out.println("✅ Éxito: Paciente ID " + p.getId() + " y HC ID " + p.getHistoriaClinica().getId() + " creados transaccionalmente.");

        } catch (ServiceException e) {
            System.err.println("❌ ERROR DE NEGOCIO: " + e.getMessage());
        } catch (DateTimeParseException e) {
            System.err.println("❌ ERROR DE FORMATO: Formato de fecha inválido. Use AAAA-MM-DD.");
        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO (Rollback realizado): " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para encapsular la creación de la HC.
     * Incluye la corrección del Enum.
     */
    private HistoriaClinica crearHistoriaClinica() {
        HistoriaClinica hc = new HistoriaClinica();
        System.out.print("Nro. Historia: "); hc.setNroHistoria(scanner.nextLine());
        
        // --- 💡 LÓGICA CORREGIDA PARA EL ENUM ---
        System.out.print("Grupo Sanguíneo (ej: O+, AB-): ");
        String gsInput = scanner.nextLine().trim().toUpperCase();
        
        // Reemplazo robusto: "O+" -> "O_MAS", "A-" -> "A_MENOS"
        String gsEnumStr = gsInput.replace("+", "_MAS").replace("-", "_MENOS");

        try {
             hc.setGrupoSanguineo(GrupoSanguineo.valueOf(gsEnumStr));
        } catch (IllegalArgumentException e) {
             System.out.println("⚠️ Grupo Sanguíneo inválido ingresado (" + gsInput + "). Se usará O_MAS por defecto.");
             hc.setGrupoSanguineo(GrupoSanguineo.O_MAS);
        }
        // --- FIN DE LA CORRECCIÓN ---

        System.out.print("Antecedentes (TEXT): "); hc.setAntecedentes(scanner.nextLine());
        System.out.print("Medicación Actual (TEXT): "); hc.setMedicacionActual(scanner.nextLine());
        System.out.print("Observaciones (TEXT): "); hc.setObservaciones(scanner.nextLine());
        
        return hc;
    }

    /**
     * Lógica para la Opción 2: Listar Pacientes
     */
    public void listarPacientes() {
        System.out.println("\n--- 📄 Listado de Pacientes Activos ---");
        try {
            List<Paciente> lista = pacienteService.getAll();
            if (lista.isEmpty()) {
                System.out.println("No hay pacientes activos en la base de datos.");
            } else {
                lista.forEach(p -> System.out.println(p));
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR AL LISTAR: " + e.getMessage());
        }
    }

    /**
     * Lógica para la Opción 3: Leer Paciente por ID
     */
    public void leerPacientePorId() {
        try { // 🔑 INICIO DEL TRY (Corregido)
            System.out.print("ID del paciente a buscar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Paciente p = pacienteService.getById(id);

            if (p == null) {
                System.out.println("⚠️ Paciente no encontrado o eliminado lógicamente.");
                return;
            }
            
            System.out.println("\n--- PACIENTE ENCONTRADO ---");
            System.out.println(p); // Llama al toString() de Paciente
            
            if (p.getHistoriaClinica() != null) {
                // Muestra detalles de la HC asociada
                System.out.println("  -> Detalle HC: Nro: " + p.getHistoriaClinica().getNroHistoria() + 
                                   ", Grupo: " + p.getHistoriaClinica().getGrupoSanguineo().getSimbolo());
                System.out.println("  -> Antecedentes: " + p.getHistoriaClinica().getAntecedentes());
            }

        } catch(NumberFormatException e) { // 🔑 CATCH 1
            System.err.println("❌ ERROR DE FORMATO: Ingrese un ID numérico.");
        } catch (Exception e) { // 🔑 CATCH 2
            System.err.println("❌ Error al leer paciente: " + e.getMessage());
        }
    }

    /**
     * Lógica para la Opción 4: Buscar Paciente por DNI
     */
    public void buscarPacientePorDni() {
        System.out.print("Ingrese DNI del Paciente a buscar: ");
        try {
            String dni = scanner.nextLine();
            Paciente p = pacienteService.buscarPorDni(dni);
            
            if (p != null) {
                System.out.println("\n--- Resultado de Búsqueda por DNI ---");
                System.out.println(p);
            } else {
                System.out.println("⚠️ No se encontró un Paciente activo con DNI: " + dni);
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR AL BUSCAR POR DNI: " + e.getMessage());
        }
    }

    /**
     * Lógica para la Opción 5: Actualizar Paciente (Transaccional)
     */
    public void actualizarPaciente() {
        System.out.print("Ingrese ID del Paciente a actualizar: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            
            Paciente p = pacienteService.getById(id);
            if (p == null) {
                System.out.println("⚠️ Paciente con ID " + id + " no encontrado.");
                return;
            }

            System.out.println("--- ✏️ Actualizando Paciente ID: " + p.getId() + " ---");
            System.out.println("(Deje en blanco y presione Enter para mantener el valor actual)");

            // 1. Actualizar Paciente (A)
            System.out.print("Nuevo Nombre (" + p.getNombre() + "): "); 
            String nuevoNombre = scanner.nextLine();
            if (!nuevoNombre.trim().isEmpty()) {
                 p.setNombre(nuevoNombre);
            }
            
            System.out.print("Nuevo Apellido (" + p.getApellido() + "): "); 
            String nuevoApellido = scanner.nextLine();
            if (!nuevoApellido.trim().isEmpty()) {
                 p.setApellido(nuevoApellido);
            }
            
            // 2. Actualización de HC (B)
            HistoriaClinica hc = p.getHistoriaClinica();
            if (hc != null) {
                System.out.println("\n--- Actualizando Historia Clínica ---");
                System.out.print("Nuevos Antecedentes (" + hc.getAntecedentes().substring(0, Math.min(20, hc.getAntecedentes().length())) + "...): ");
                String nuevosAntecedentes = scanner.nextLine();
                if (!nuevosAntecedentes.trim().isEmpty()) {
                    hc.setAntecedentes(nuevosAntecedentes);
                }
                // (Se podrían agregar más campos de HC a actualizar aquí)
            }
            
            // 3. Llamada al Service transaccional
            pacienteService.actualizar(p);
            System.out.println("✅ Éxito: Paciente y Historia Clínica actualizados transaccionalmente.");
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ERROR DE ENTRADA: Debe ingresar un ID numérico entero.");
        } catch (ServiceException e) {
            System.err.println("❌ ERROR DE NEGOCIO: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO (Rollback realizado): " + e.getMessage());
        }
    }

    /**
     * Lógica para la Opción 6: Eliminar Paciente (Baja Lógica Transaccional)
     */
    public void eliminarPaciente() {
        System.out.print("Ingrese ID del Paciente a ELIMINAR LÓGICAMENTE: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            
            System.out.print("¿Está seguro de eliminar LÓGICAMENTE al Paciente ID " + id + " y su HC? (S/N): ");
            String confirmacion = scanner.nextLine().trim().toUpperCase();
            
            if (confirmacion.equals("S")) {
                pacienteService.eliminar(id);
                System.out.println("✅ Éxito: Paciente con ID " + id + " y su HC han sido marcados como 'eliminado'.");
            } else {
                System.out.println("Operación cancelada.");
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ERROR DE ENTRADA: Debe ingresar un ID numérico entero.");
        } catch (ServiceException e) {
            System.err.println("❌ ERROR DE NEGOCIO: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO (Rollback realizado): " + e.getMessage());
        }
    }
}
