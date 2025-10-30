package entities;

public class HistoriaClinica extends Base { // Extiende la clase Base
    
    // Enum para el grupo sanguíneo (se mantiene igual)
    public enum GrupoSanguineo {
        A_MAS("A+"), A_MENOS("A-"), B_MAS("B+"), B_MENOS("B-"),
        AB_MAS("AB+"), AB_MENOS("AB-"), O_MAS("O+"), O_MENOS("O-");

        private final String simbolo;

        GrupoSanguineo(String simbolo) {
            this.simbolo = simbolo;
        }

        public String getSimbolo() {
            return simbolo;
        }
    }

    private String nroHistoria;
    private GrupoSanguineo grupoSanguineo;
    private String antecedentes;
    private String medicacionActual;
    private String observaciones;
    
    // Campo auxiliar para que el DAO/Service maneje la FK del Paciente (en la tabla HC)
    // Se mantiene como Long para mapear al ID del Paciente en la BD (BIGINT), 
    // aunque el ID propio de HC sea 'int' por herencia de Base.
    private Long pacienteId; 

    // Constructor completo
    public HistoriaClinica(int id, boolean eliminado, String nroHistoria, GrupoSanguineo grupoSanguineo, String antecedentes, String medicacionActual, String observaciones, Long pacienteId) {
        super(id, eliminado); // Llama al constructor completo de Base
        this.nroHistoria = nroHistoria;
        this.grupoSanguineo = grupoSanguineo;
        this.antecedentes = antecedentes;
        this.medicacionActual = medicacionActual;
        this.observaciones = observaciones;
        this.pacienteId = pacienteId;
    }
    
    // Constructor por defecto
    public HistoriaClinica() {
        super(); // Llama al constructor por defecto de Base
    }

    // Getters y Setters
    public String getNroHistoria() { return nroHistoria; }
    public void setNroHistoria(String nroHistoria) { this.nroHistoria = nroHistoria; }
    
    public GrupoSanguineo getGrupoSanguineo() { return grupoSanguineo; }
    public void setGrupoSanguineo(GrupoSanguineo grupoSanguineo) { this.grupoSanguineo = grupoSanguineo; }
    
    public String getAntecedentes() { return antecedentes; }
    public void setAntecedentes(String antecedentes) { this.antecedentes = antecedentes; }
    
    public String getMedicacionActual() { return medicacionActual; }
    public void setMedicacionActual(String medicacionActual) { this.medicacionActual = medicacionActual; }
    
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    @Override
    public String toString() {
        return "HC [ID=" + getId() + ", Nro. HC=" + nroHistoria + ", Grupo=" + (grupoSanguineo != null ? grupoSanguineo.getSimbolo() : "N/A") + 
               ", Eliminado=" + isEliminado() + "]";
    }
}