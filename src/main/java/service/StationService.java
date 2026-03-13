package service;
import model.MonitoringStation;
import repository.StationRepository;
import validator.StationValidator;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
public class StationService extends IdentifiableService<Integer, MonitoringStation, StationRepository> {

    public StationService(StationRepository repository) {
        super(repository, new StationValidator());
    }

    /** Builds a MonitoringStation from raw params and saves it. */
//    public Integer create(String location, String type, String status,
//                          LocalDate establishedDate, String operator) throws SQLException {
//        MonitoringStation s = new MonitoringStation();
//        s.setLocation(location);
//        s.setType(type);
//        s.setStatus(status);
//        s.setEstablishedDate(establishedDate);
//        s.setOperator(operator);
//        return save(s);
//    }

    /** Builds a MonitoringStation from raw params and updates it. */
//    public void edit(int id, String location, String type, String status,
//                     LocalDate establishedDate, String operator) throws SQLException {
//        MonitoringStation s = new MonitoringStation();
//        s.setId(id);
//        s.setLocation(location);
//        s.setType(type);
//        s.setStatus(status);
//        s.setEstablishedDate(establishedDate);
//        s.setOperator(operator);
//        update(s);
//    }

    /** Searches stations by keyword. */
    public List<MonitoringStation> search(String keyword) throws SQLException {
        return getRepository().search(keyword);
    }
}

