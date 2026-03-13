package validator;

/**
 * Generic validator interface.
 * Implementations throw ValidationException if the entity is invalid.
 * @param <E> entity type to validate
 */
public interface Validator<E> {
    void validate(E entity) throws ValidationException;
}
