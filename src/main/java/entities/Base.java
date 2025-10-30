package entities;

public abstract class Base {
    
    private int id;

    private boolean eliminado;

    protected Base(int id, boolean eliminado) {
        this.id = id;
        this.eliminado = eliminado;
    }

    protected Base() {
        this.eliminado = false;
    }

    /**
     * Obtiene el ID de la entidad.
     * @return ID de la entidad, 0 si aún no ha sido persistida
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el ID de la entidad.
     * Típicamente llamado por el DAO después de insertar en la BD.
     *
     * @param id Nuevo ID de la entidad
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Verifica si la entidad está marcada como eliminada.
     * @return true si está eliminada, false si está activa
     */
    public boolean isEliminado() {
        return eliminado;
    }

    /**
     * Marca o desmarca la entidad como eliminada.
     * En el contexto del soft delete, esto se usa para "eliminar" sin borrar físicamente.
     *
     * @param eliminado true para marcar como eliminada, false para reactivar
     */
    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}
