package Dao;

import Config.DatabaseConnection;
import entities.HistoriaClinica;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class HistoriaClinicaDAO implements GenericDAO<HistoriaClinica> {
    
    // Consultas SQL
    private final String INSERT = "INSERT INTO HistoriaClinica (nroHistoria, grupoSanguineo, antecedentes, medicacionActual, observaciones, paciente_id) VALUES (?, ?, ?, ?, ?, ?)";
    private final String UPDATE = "UPDATE HistoriaClinica SET nroHistoria=?, grupoSanguineo=?, antecedentes=?, medicacionActual=?, observaciones=? WHERE id=? AND eliminado = FALSE";
    private final String DELETE_LOGICO = "UPDATE HistoriaClinica SET eliminado = TRUE WHERE paciente_id = ?"; // Baja por ID del Paciente asociado

    // --- Métodos Transaccionales (usan Connection externa) ---
    
    @Override
    public void insertTx(HistoriaClinica entidad, Connection conn) throws Exception {
        // Implementación de crear transaccional.
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entidad.getNroHistoria());
            ps.setString(2, entidad.getGrupoSanguineo().getSimbolo()); 
            ps.setString(3, entidad.getAntecedentes());
            ps.setString(4, entidad.getMedicacionActual());
            ps.setString(5, entidad.getObservaciones());
            ps.setLong(6, entidad.getPacienteId()); // CLAVE: Usa el ID del Paciente (Long)

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        entidad.setId(rs.getInt(1)); // Asignar el ID generado (int)
                    }
                }
            } else {
                 throw new SQLException("Fallo la inserción de HistoriaClinica, no se afectaron filas.");
            }
        } catch (SQLException e) {
            throw new Exception("Error al insertar HistoriaClinica en transacción: " + e.getMessage(), e); 
        }
    }
    
    // --- Métodos que participan en la transacción (usados en Service) ---
    
    // Se crea una variante transaccional de actualizar y eliminar que se usará en el Service
    public void actualizarTx(HistoriaClinica entidad, Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, entidad.getNroHistoria());
            ps.setString(2, entidad.getGrupoSanguineo().getSimbolo());
            ps.setString(3, entidad.getAntecedentes());
            ps.setString(4, entidad.getMedicacionActual());
            ps.setString(5, entidad.getObservaciones());
            ps.setInt(6, entidad.getId()); 
            ps.executeUpdate();
        } catch (SQLException e) {
             throw new Exception("Error al actualizar HistoriaClinica en transacción: " + e.getMessage(), e);
        }
    }

    public void eliminarPorPacienteIdTx(long pacienteId, Connection conn) throws Exception {
        // Se ejecuta la baja lógica de HC usando el ID del Paciente asociado.
        try (PreparedStatement ps = conn.prepareStatement(DELETE_LOGICO)) {
            ps.setLong(1, pacienteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error al eliminar HistoriaClinica por Paciente ID en transacción: " + e.getMessage(), e);
        }
    }

    // --- Métodos de GenericDAO (Implementación Mínima o Lógica Propia) ---
    
    @Override
    public HistoriaClinica getById(int id) throws Exception {
        throw new UnsupportedOperationException("No implementado: El acceso a HC se realiza principalmente vía PacienteDao.");
    }

    @Override
    public List<HistoriaClinica> getAll() throws Exception {
        throw new UnsupportedOperationException("No implementado: El listado se realiza a través de PacienteDao.");
    }
    
    @Override
    public void insertar(HistoriaClinica entidad) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            insertTx(entidad, conn);
        } catch (SQLException e) {
             throw new Exception("Error al insertar HistoriaClinica (simple): " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(HistoriaClinica entidad) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            actualizarTx(entidad, conn);
        }
    }
  
    @Override
    public void eliminar(int id) throws Exception {
        throw new UnsupportedOperationException("Usar eliminarPorPacienteIdTx(...) en el Service.");
    }
}
