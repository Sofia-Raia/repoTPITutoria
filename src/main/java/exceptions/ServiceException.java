/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package exceptions;

/**
 *
 * @author sofia
 */
public class ServiceException extends Exception {

    /**
     * Constructor que acepta un mensaje detallado del error de negocio.
     * @param message El mensaje que describe la falla de la regla de negocio.
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Constructor que acepta un mensaje y una causa (Throwable).
     * Útil para encapsular excepciones de capas inferiores (ej. SQLException)
     * y lanzarlas como errores de negocio.
     * @param message El mensaje que describe el error.
     * @param cause La excepción original que causó este error.
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

