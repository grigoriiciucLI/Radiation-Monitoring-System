package validator;
import model.MonitoringStation;
/** Validates a MonitoringStation before save/update. */
public class StationValidator implements Validator<MonitoringStation> {
    @Override
    public void validate(MonitoringStation s) throws ValidationException {
        if (s.getLocation() == null || s.getLocation().isBlank())
            throw new ValidationException("Location is required.");
        if (s.getLocation().length() > 150)
            throw new ValidationException("Location must be 150 characters or fewer.");
        if (s.getType() == null || s.getType().isBlank())
            throw new ValidationException("Type is required.");
        if (s.getStatus() == null || s.getStatus().isBlank())
            throw new ValidationException("Status is required.");
        if (s.getOperator() == null || s.getOperator().isBlank())
            throw new ValidationException("Operator is required.");
    }
}
