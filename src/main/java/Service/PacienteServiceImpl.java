
package Service;

import Config.DatabaseConnection;
import Config.TransactionManager;
import Dao.PacienteDAO; 
import Dao.HistoriaClinicaDAO; 
import entities.Paciente;
import entities.HistoriaClinica;
import exceptions.ServiceException;
import java.sql.SQLException;
import java.util.List;

// Implementa la interfaz genérica que definiste
public class PacienteServiceImpl implements GenericService<Paciente> {

    // Instancias de los DAOs que se inyectan/utilizan
    // (Usando los nombres de clase exactos que definiste en el DAO)
    private final PacienteDAO pacienteDao = new PacienteDAO();
    private final HistoriaClinicaDAO historiaClinicaDao = new HistoriaClinicaDAO();
    
    // --- Lógica de Negocio y Validación ---
    
    private void validarPaciente(Paciente p) throws ServiceException {
        // Validación de campos obligatorios (nombre, apellido, DNI)
        if (p.getNombre() == null || p.getNombre().trim().isEmpty() ||
            p.getApellido() == null || p.getApellido().trim().isEmpty()) {
            throw new ServiceException("El nombre y apellido del paciente son obligatorios.");
        }
        if (p.getDni() == null || !p.getDni().matches("\\d{7,15}")) {
            throw new ServiceException("El DNI debe tener un formato válido (solo números, 7-15 dígitos).");
        }
        // Validación de la regla 1:1
        if (p.getHistoriaClinica() == null || 
            p.getHistoriaClinica().getNroHistoria() == null || 
            p.getHistoriaClinica().getNroHistoria().trim().isEmpty()) {
            throw new ServiceException("Debe ingresar una Historia Clínica válida con un número de historia.");
        }
    }
    
    // --- Métodos de GenericService ---

    /**
     * Implementa la operación transaccional CREAR (A + B).
     */
    @Override
    public void insertar(Paciente p) throws Exception {
        validarPaciente(p); // 1. Validar reglas de negocio
        
        // Uso de try-with-resources con TransactionManager
        try (TransactionManager tx = new TransactionManager(DatabaseConnection.getConnection())) {
            tx.startTransaction(); // 2. INICIO de la Transacción

            // A. Insertar Paciente (A). El DAO asigna el ID a 'p'.
            pacienteDao.insertTx(p, tx.getConnection());
            
            // B. Preparar la Historia Clínica (B) con el ID (FK) de A.
            HistoriaClinica hc = p.getHistoriaClinica();
            hc.setPacienteId((long) p.getId()); // Cast de int a long (p.getId() es int)
            
            // C. Insertar Historia Clínica (B)
            historiaClinicaDao.insertTx(hc, tx.getConnection());
            
            // Actualizar la referencia del objeto Paciente con el ID de HC
            p.setHistoriaClinica(hc); 
            
            tx.commit(); // 3. COMMIT si todo fue exitoso

        } catch (Exception e) {
            // TransactionManager.close() hará rollback automáticamente si es necesario.
            // Mapeo y relanzamiento de excepciones para la capa superior.
            if (e.getCause() instanceof SQLException && ((SQLException) e.getCause()).getErrorCode() == 1062) { 
                throw new ServiceException("Error de unicidad (DNI/Nro. HC ya existen).", e.getCause());
            }
            throw new ServiceException("Fallo la inserción transaccional: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void actualizar(Paciente p) throws Exception {
        // Esta actualización debería ser transaccional si afecta a HC.
        
        // (La validación original aquí podría fallar si solo se quiere actualizar el nombre 
        // y no se re-envía la HC. Se ajusta la validación de HC para que solo valide 
        // los campos del Paciente en una actualización)
         if (p.getNombre() == null || p.getNombre().trim().isEmpty() ||
            p.getApellido() == null || p.getApellido().trim().isEmpty()) {
            throw new ServiceException("El nombre y apellido del paciente son obligatorios.");
        }
        
        try (TransactionManager tx = new TransactionManager(DatabaseConnection.getConnection())) {
            tx.startTransaction();

            // 1. Actualizar Paciente (A)
            // (CORREGIDO: Se llama a 'actualizarTx' que existe en el DAO)
            pacienteDao.actualizarTx(p, tx.getConnection());
            
            // 2. Si hay HC asociada, actualizar la HC (B)
            if (p.getHistoriaClinica() != null && p.getHistoriaClinica().getId() > 0) {
                 historiaClinicaDao.actualizarTx(p.getHistoriaClinica(), tx.getConnection());
            }

            tx.commit();
        } catch (Exception e) {
            // El TransactionManager maneja el rollback.
            throw new ServiceException("Fallo la actualización transaccional: " + e.getMessage(), e);
        }
    }

    /**
     * Implementa la operación transaccional ELIMINAR (Baja Lógica en A + B).
     */
    @Override
    public void eliminar(int id) throws Exception {
        // Se asegura que la baja lógica de A y B sea atómica.
        try (TransactionManager tx = new TransactionManager(DatabaseConnection.getConnection())) {
            tx.startTransaction(); 
            
            // 1. Eliminar (baja lógica) HistoriaClinica asociada (B)
            historiaClinicaDao.eliminarPorPacienteIdTx(id, tx.getConnection());
            
            // 2. Eliminar (baja lógica) Paciente (A)
            // (CORREGIDO: Se llama a 'eliminarTx' que existe en el DAO)
            pacienteDao.eliminarTx(id, tx.getConnection());

            tx.commit(); 
            
        } catch (Exception e) {
            // El TransactionManager maneja el rollback.
            throw new ServiceException("Fallo la eliminación transaccional: Paciente ID " + id, e); 
        }
    }
    
    @Override
    public Paciente getById(int id) throws Exception {
        // Operación de lectura simple, usa el DAO directamente.
        return pacienteDao.getById(id);
    }
    
    @Override
    public List<Paciente> getAll() throws Exception {
        // Operación de lectura simple.
        return pacienteDao.getAll();
    }
    
    // --- Métodos Adicionales (Búsqueda por campo relevante) ---
    
    public Paciente buscarPorDni(String dni) throws Exception {
        // Cumple el requisito de búsqueda por campo relevante
        // (CORREGIDO: Ahora 'buscarPorDni' existe en PacienteDao)
        return pacienteDao.buscarPorDni(dni);
    }
}