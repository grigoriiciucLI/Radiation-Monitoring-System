package validator;
import model.RadiationReading;

/** Validates a RadiationReading before save/update. */
public class ReadingValidator implements Validator<RadiationReading> {
    @Override
    public void validate(RadiationReading r) throws ValidationException {
        if (r.getTimestamp() == null)
            throw new ValidationException("Timestamp is required.");
        if (r.getRadiationLevel() < 0)
            throw new ValidationException("Radiation level cannot be negative.");
        if (r.getRadiationType() == null || r.getRadiationType().isBlank())
            throw new ValidationException("Radiation type is required.");
        if (r.getAlertStatus() == null || r.getAlertStatus().isBlank())
            throw new ValidationException("Alert status is required.");
        if (r.getNotes() != null && r.getNotes().length() > 500)
            throw new ValidationException("Notes must be 500 characters or fewer.");
    }
}
