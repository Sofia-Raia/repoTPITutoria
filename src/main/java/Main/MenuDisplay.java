package Main;


public class MenuDisplay {
    public static void mostrarMenuPrincipal() {
      System.out.println("\n========= TFI: PACIENTE - HISTORIA CLINICA (1:1) =========");
      System.out.println("1. Crear Paciente y Historia Clínica (Transaccional)");
      System.out.println("2. Listar Pacientes por ID");
      System.out.println("3. Actualizar Paciente y/o HC");
      System.out.println("4. Eliminar Paciente (Baja Lógica Transaccional)");
      System.out.println("5. Buscar Paciente por DNI (Consulta Relevante)");
      System.out.println("6. Buscar Paciente por ID");
      System.out.println("0. Salir");
      System.out.print("Ingrese una opcion: ");
    }
}