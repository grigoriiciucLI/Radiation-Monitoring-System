package service;
import model.RadiationReading;
import repository.ReadingRepository;
import validator.ReadingValidator;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for RadiationReading.
 * Extends IdentifiableService<Integer, RadiationReading, ReadingRepository>.
 */
public class ReadingService extends IdentifiableService<Integer, RadiationReading, ReadingRepository> {

    public ReadingService(ReadingRepository repository) {
        super(repository, new ReadingValidator());
    }

    /** Builds a RadiationReading from raw params and saves it. */
    public Integer create(int stationId, LocalDateTime timestamp, double radiationLevel,
                          String radiationType, String alertStatus, String notes) throws SQLException {
        RadiationReading r = new RadiationReading();
        r.setStationId(stationId);
        r.setTimestamp(timestamp != null ? timestamp : LocalDateTime.now());
        r.setRadiationLevel(radiationLevel);
        r.setRadiationType(radiationType);
        r.setAlertStatus(alertStatus);
        r.setNotes(notes);
        return save(r);
    }

    /** Builds a RadiationReading from raw params and updates it. */
    public void edit(int id, int stationId, LocalDateTime timestamp, double radiationLevel,
                     String radiationType, String alertStatus, String notes) throws SQLException {
        RadiationReading r = new RadiationReading();
        r.setId(id);
        r.setStationId(stationId);
        r.setTimestamp(timestamp != null ? timestamp : LocalDateTime.now());
        r.setRadiationLevel(radiationLevel);
        r.setRadiationType(radiationType);
        r.setAlertStatus(alertStatus);
        r.setNotes(notes);
        update(r);
    }

    /** Returns all readings for a given station. */
    public List<RadiationReading> findByStation(int stationId) throws SQLException {
        return getRepository().findByStationId(stationId);
    }

    /** Searches readings for a station by keyword. */
    public List<RadiationReading> search(int stationId, String keyword) throws SQLException {
        return getRepository().searchByStationId(stationId, keyword);
    }
}