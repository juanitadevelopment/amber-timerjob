package net.teppan.amber.timerjob;

/**
 * Exception thrown when a job scheduling operation fails.
 * 
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>An invalid schedule directive is provided</li>
 *   <li>Timer scheduling fails</li>
 *   <li>Job initialization fails during scheduling</li>
 * </ul>
 * 
 * @since 1.0
 * @author Juanita Development
 */
public class SchedulingException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new SchedulingException with the specified detail message.
     * 
     * @param message the detail message
     */
    public SchedulingException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new SchedulingException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new SchedulingException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public SchedulingException(Throwable cause) {
        super(cause);
    }
} 