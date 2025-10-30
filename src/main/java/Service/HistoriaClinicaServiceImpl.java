/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Service;
import Dao.HistoriaClinicaDAO;
import entities.HistoriaClinica;
import exceptions.ServiceException;
import java.util.List;

/**
 * Service de HistoriaClinica. Dado que la relación es 1:1 unidireccional (A -> B),
 * las operaciones transaccionales compuestas (crear/eliminar) son orquestadas
 * por PacienteServiceImpl. Este Service se enfoca en las operaciones CRUD simples.
 */
public class HistoriaClinicaServiceImpl implements GenericService<HistoriaClinica> {

    private final HistoriaClinicaDAO historiaClinicaDao = new HistoriaClinicaDAO();
    
    // --- Métodos de GenericService ---

    @Override
    public void insertar(HistoriaClinica entidad) throws Exception {
        // NOTA: Se recomienda usar PacienteService.insertar() para insertar HC, 
        // ya que la HC requiere un Paciente asociado (FK).
        // Aquí se permite, asumiendo que la entidad ya tiene el pacienteId seteado.
        if (entidad.getPacienteId() == null || entidad.getPacienteId() <= 0) {
            throw new ServiceException("No se puede insertar la Historia Clínica sin un Paciente asociado.");
        }
        historiaClinicaDao.insertar(entidad); 
    }
    
    @Override
    public void actualizar(HistoriaClinica entidad) throws Exception {
        // Operación simple
        historiaClinicaDao.actualizar(entidad); 
    }

    @Override
    public void eliminar(int id) throws Exception {
        // Se lanza excepción porque la eliminación de HC debe hacerse mediante la eliminación del Paciente.
        throw new UnsupportedOperationException("La eliminación de HistoriaClínica debe realizarse a través de PacienteService.");
    }

    @Override
    public HistoriaClinica getById(int id) throws Exception {
        return historiaClinicaDao.getById(id);
    }

    @Override
    public List<HistoriaClinica> getAll() throws Exception {
        return historiaClinicaDao.getAll();
    }
}
