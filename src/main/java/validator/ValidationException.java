package validator;

/** Thrown when an entity fails validation rules. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
