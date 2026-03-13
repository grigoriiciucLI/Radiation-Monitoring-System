package repository;
import model.MonitoringStation;
import util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of StationRepository.
 * Uses PreparedStatements for all queries to prevent SQL injection.
 */
public class StationRepositoryImpl implements StationRepository {
    private static final String SELECT_ALL =
        "SELECT id, location, type, status, established_date, operator " +
        "FROM MonitoringStations ORDER BY location";
    private static final String SELECT_BY_ID =
        "SELECT id, location, type, status, established_date, operator " +
        "FROM MonitoringStations WHERE id = ?";
    private static final String INSERT =
        "INSERT INTO MonitoringStations (location, type, status, established_date, operator) " +
        "VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE =
        "UPDATE MonitoringStations " +
        "SET location=?, type=?, status=?, established_date=?, operator=? WHERE id=?";
    private static final String DELETE =
        "DELETE FROM MonitoringStations WHERE id = ?";
    private static final String SEARCH =
        "SELECT id, location, type, status, established_date, operator " +
        "FROM MonitoringStations " +
        "WHERE location ILIKE ? OR operator ILIKE ? OR type ILIKE ? ORDER BY location";
    @Override
    public List<MonitoringStation> findAll() throws SQLException {
        List<MonitoringStation> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
    @Override
    public MonitoringStation findById(Integer id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }
    @Override
    public List<MonitoringStation> search(String keyword) throws SQLException {
        List<MonitoringStation> list = new ArrayList<>();
        String p = "%" + keyword + "%";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SEARCH)) {
            ps.setString(1, p); ps.setString(2, p); ps.setString(3, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }
    @Override
    public Integer save(MonitoringStation s) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getLocation());
            ps.setString(2, s.getType());
            ps.setString(3, s.getStatus());
            ps.setDate(4, s.getEstablishedDate() != null ? Date.valueOf(s.getEstablishedDate()) : null);
            ps.setString(5, s.getOperator());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void update(MonitoringStation s) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setString(1, s.getLocation());
            ps.setString(2, s.getType());
            ps.setString(3, s.getStatus());
            ps.setDate(4, s.getEstablishedDate() != null ? Date.valueOf(s.getEstablishedDate()) : null);
            ps.setString(5, s.getOperator());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private MonitoringStation mapRow(ResultSet rs) throws SQLException {
        MonitoringStation s = new MonitoringStation();
        s.setId(rs.getInt("id"));
        s.setLocation(rs.getString("location"));
        s.setType(rs.getString("type"));
        s.setStatus(rs.getString("status"));
        Date d = rs.getDate("established_date");
        s.setEstablishedDate(d != null ? d.toLocalDate() : null);
        s.setOperator(rs.getString("operator"));
        return s;
    }
}
