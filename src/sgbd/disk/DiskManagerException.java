package sgbd.disk;
/**
 * Exception thrown when an error occurs within the DiskManager layer.
 */
public class DiskManagerException extends DBMSException {

    /**
     * Constructs a new DiskManagerException with no detail message.
     */
    public DiskManagerException() {
        super();
    }

    /**
     * Constructs a new DiskManagerException with the specified detail message.
     *
     * @param message The detail message.
     */
    public DiskManagerException(String message) {
        super(message);
    }

    /**
     * Constructs a new DiskManagerException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public DiskManagerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new DiskManagerException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public DiskManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
