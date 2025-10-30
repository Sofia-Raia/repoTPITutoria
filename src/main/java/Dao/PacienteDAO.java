package Dao;
import Config.DatabaseConnection;
import entities.Paciente;
import entities.HistoriaClinica;
import entities.HistoriaClinica.GrupoSanguineo;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PacienteDAO implements GenericDAO<Paciente> {
    
    // --- Consultas SQL ---
    private final String INSERT = "INSERT INTO Paciente (nombre, apellido, dni, fechaNacimiento) VALUES (?, ?, ?, ?)";
    
    // Base de la consulta con LEFT JOIN para traer la HistoriaClinica (1:1)
    private final String SELECT_BASE = "SELECT p.*, hc.id as hc_id, hc.nroHistoria, hc.grupoSanguineo, hc.antecedentes, hc.medicacionActual, hc.observaciones "
                                      + "FROM Paciente p LEFT JOIN HistoriaClinica hc ON p.id = hc.paciente_id ";
    
    private final String SELECT_BY_ID = SELECT_BASE + "WHERE p.id = ? AND p.eliminado = FALSE";
    private final String SELECT_ALL = SELECT_BASE + "WHERE p.eliminado = FALSE ORDER BY p.apellido, p.nombre";
    private final String SELECT_BY_DNI = SELECT_BASE + "WHERE p.dni = ? AND p.eliminado = FALSE"; 
    
    private final String UPDATE = "UPDATE Paciente SET nombre=?, apellido=?, dni=?, fechaNacimiento=? WHERE id=? AND eliminado = FALSE";
    private final String DELETE_LOGICO = "UPDATE Paciente SET eliminado = TRUE WHERE id=?";

    // --- Mapeo de Resultados (ResultSet a Objeto Paciente) ---
    private Paciente mapPaciente(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        
        // Mapeo de campos heredados de Base (int id, boolean eliminado)
        p.setId(rs.getInt("id")); 
        p.setEliminado(rs.getBoolean("eliminado"));
        
        // Mapeo de campos propios de Paciente
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setDni(rs.getString("dni"));
        
        Date fechaSql = rs.getDate("fechaNacimiento");
        if (fechaSql != null) {
            p.setFechaNacimiento(fechaSql.toLocalDate()); 
        }
        
        // Mapeo de la HistoriaClinica (Clave de la relación 1:1)
        if (rs.getInt("hc_id") > 0) { // Verifica si el LEFT JOIN trajo una HC válida
            HistoriaClinica hc = new HistoriaClinica();
            
            // Mapeo de campos heredados de Base para HC
            hc.setId(rs.getInt("hc_id"));
            
            // Mapeo de campos propios de HC
            hc.setNroHistoria(rs.getString("nroHistoria"));
            
            String gsString = rs.getString("grupoSanguineo");
            if (gsString != null) {
                // 💡 --- CORRECCIÓN CRÍTICA ---
                // Conversión robusta de String (A+) a Enum (A_MAS)
                String gsEnumStr = gsString.replace("+", "_MAS").replace("-", "_MENOS");
                hc.setGrupoSanguineo(GrupoSanguineo.valueOf(gsEnumStr));
            }
            
            hc.setAntecedentes(rs.getString("antecedentes"));
            hc.setMedicacionActual(rs.getString("medicacionActual"));
            hc.setObservaciones(rs.getString("observaciones"));
            
            p.setHistoriaClinica(hc); // Asignación de la Entidad B a la Entidad A
        }
        return p;
    }

    // --- Métodos Transaccionales (usan Connection externa) ---
    
    @Override
    public void insertTx(Paciente entidad, Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entidad.getNombre());
            ps.setString(2, entidad.getApellido());
            ps.setString(3, entidad.getDni());
            ps.setDate(4, (entidad.getFechaNacimiento() != null) ? Date.valueOf(entidad.getFechaNacimiento()) : null);

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entidad.setId(rs.getInt(1)); // Asignar el ID generado (int)
                    }
                }
            } else {
                 throw new SQLException("Fallo la inserción de Paciente, no se afectaron filas.");
            }
        } catch (SQLException e) {
            // Relanza SQLException como Exception (según firma de GenericDAO)
            throw new Exception("Error al insertar Paciente en transacción: " + e.getMessage(), e); 
        }
    }
    
    // --- Métodos de Lectura (usan Connection propia) ---

    @Override
    public Paciente getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id); // Usar int para el ID
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPaciente(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new Exception("Error al leer Paciente por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Paciente> getAll() throws Exception {
        List<Paciente> pacientes = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                pacientes.add(mapPaciente(rs));
            }
            return pacientes;
        } catch (SQLException e) {
             throw new Exception("Error al listar Pacientes: " + e.getMessage(), e);
        }
    }

    // --- Métodos de GenericDAO (con conexión propia) ---
    
    @Override
    public void actualizar(Paciente entidad) throws Exception {
        // Implementa abriendo y cerrando su propia conexión.
        try (Connection conn = DatabaseConnection.getConnection()) {
            actualizarTx(entidad, conn); // Reutiliza la lógica transaccional
        } catch (SQLException e) {
            throw new Exception("Error al actualizar Paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(int id) throws Exception {
        // Implementa la baja lógica (UPDATE) con su propia conexión.
        try (Connection conn = DatabaseConnection.getConnection()) {
             eliminarTx(id, conn); // Reutiliza la lógica transaccional
        } catch (SQLException e) {
            throw new Exception("Error al eliminar (lógicamente) Paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertar(Paciente entidad) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            insertTx(entidad, conn);
        } catch (SQLException e) {
             throw new Exception("Error al insertar Paciente (simple): " + e.getMessage(), e);
        }
    }
    
    // --- Métodos Adicionales para el Service (Transaccionales o Búsquedas) ---
    
    /**
     * Búsqueda por DNI (campo relevante).
     */
    public Paciente buscarPorDni(String dni) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_DNI)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPaciente(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new Exception("Error al buscar Paciente por DNI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Variante transaccional de Actualizar (usada por el Service).
     */
    public void actualizarTx(Paciente entidad, Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, entidad.getNombre());
            ps.setString(2, entidad.getApellido());
            ps.setString(3, entidad.getDni());
            ps.setDate(4, (entidad.getFechaNacimiento() != null) ? Date.valueOf(entidad.getFechaNacimiento()) : null);
            ps.setInt(5, entidad.getId()); 
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al actualizar Paciente en transacción: " + e.getMessage(), e);
        }
    }
    
    /**
     * Variante transaccional de Eliminar (usada por el Service).
     */
    public void eliminarTx(int id, Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_LOGICO)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al eliminar Paciente en transacción: " + e.getMessage(), e);
        }
    }
}
