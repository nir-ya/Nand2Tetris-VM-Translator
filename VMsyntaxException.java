

/**
 * An exception indicating a syntactic error in the input vm code.
 */
class VMsyntaxException extends Exception {
    
    
    /**
     * Constructs a new VMsyntaxException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    VMsyntaxException(String message) {
        super(message);
    }
}
