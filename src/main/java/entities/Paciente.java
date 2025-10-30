package entities;

import java.time.LocalDate;

public class Paciente extends Base { // Extiende la clase Base

    private String nombre;
    private String apellido;
    private String dni;
    private LocalDate fechaNacimiento;
    
    // Relación 1:1 unidireccional
    private HistoriaClinica historiaClinica; 

    // Constructor completo (incluye los campos heredados y propios)
    public Paciente(int id, boolean eliminado, String nombre, String apellido, String dni, LocalDate fechaNacimiento, HistoriaClinica historiaClinica) {
        super(id, eliminado); // Llama al constructor completo de Base
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.historiaClinica = historiaClinica;
    }

    // Constructor por defecto (usado para crear nuevos Pacientes)
    public Paciente() {
        super(); // Llama al constructor por defecto de Base
    }
    
    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    
    public HistoriaClinica getHistoriaClinica() { return historiaClinica; }
    public void setHistoriaClinica(HistoriaClinica historiaClinica) { this.historiaClinica = historiaClinica; }

    @Override
    public String toString() {
        return "Paciente [ID=" + getId() + ", DNI=" + dni + ", Nombre=" + nombre + 
               ", HC: " + (historiaClinica != null ? historiaClinica.getNroHistoria() : "N/A") + 
               ", Eliminado=" + isEliminado() + "]";
    }
}