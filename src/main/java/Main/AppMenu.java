package Main;

import Service.PacienteServiceImpl;
import Service.HistoriaClinicaServiceImpl; // 1. Importar el servicio de HC
import java.util.Scanner;

public class AppMenu {
    
    private final Scanner scanner;
    private final MenuHandler menuHandler;
    private boolean running;

    public AppMenu() {
        this.scanner = new Scanner(System.in);
        
        // 2. Crear las instancias de AMBOS servicios
        PacienteServiceImpl pacienteService = new PacienteServiceImpl();
        HistoriaClinicaServiceImpl historiaClinicaService = new HistoriaClinicaServiceImpl();
        
        // 3. Inyectar TODAS las dependencias (3 argumentos) al MenuHandler
        this.menuHandler = new MenuHandler(scanner, pacienteService, historiaClinicaService);
        this.running = true;
    }

    /* * Nota: Si tienes una clase 'Main.java' separada que llama a 'AppMenu', 
     * puedes eliminar este método 'main' de 'AppMenu.java'. 
     * Si 'AppMenu' es tu clase de arranque, déjalo.
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }

    public void run() {
        while (running) {
            try {
                MenuDisplay.mostrarMenuPrincipal(); // Llama al display
                int opcion = Integer.parseInt(scanner.nextLine());
                processOption(opcion);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor, ingrese un número.");
            }
        }
        scanner.close();
    }

    /**
     * Procesa la opción del menú principal.
     * Adaptado al CRUD de Paciente (1:1).
     */
    private void processOption(int opcion) {
        switch (opcion) {
            case 1: menuHandler.crearPaciente(); break;
            case 2: menuHandler.listarPacientes(); break;
            case 3: menuHandler.actualizarPaciente(); break;
            case 4: menuHandler.eliminarPaciente(); break;
            case 5: menuHandler.buscarPacientePorDni(); break;
            case 6: menuHandler.leerPacientePorId(); break;
            case 0:
                System.out.println("Saliendo...");
                running = false;
                break;
            default:
                System.out.println("Opción no válida.");
        }
    }
}