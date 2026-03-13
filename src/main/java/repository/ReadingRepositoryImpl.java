package repository;
import model.RadiationReading;
import util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * JDBC implementation of ReadingRepository.
 * Uses PreparedStatements for all queries.
 */
public class ReadingRepositoryImpl implements ReadingRepository {
    private static final String SELECT_ALL =
        "SELECT id, station_id, timestamp, radiation_level, radiation_type, alert_status, notes " +
        "FROM RadiationReadings ORDER BY timestamp DESC";
    private static final String SELECT_BY_ID =
        "SELECT id, station_id, timestamp, radiation_level, radiation_type, alert_status, notes " +
        "FROM RadiationReadings WHERE id = ?";
    private static final String SELECT_BY_STATION =
        "SELECT id, station_id, timestamp, radiation_level, radiation_type, alert_status, notes " +
        "FROM RadiationReadings WHERE station_id = ? ORDER BY timestamp DESC";
    private static final String INSERT =
        "INSERT INTO RadiationReadings " +
        "(station_id, timestamp, radiation_level, radiation_type, alert_status, notes) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
        "UPDATE RadiationReadings " +
        "SET timestamp=?, radiation_level=?, radiation_type=?, alert_status=?, notes=? WHERE id=?";
    private static final String DELETE =
        "DELETE FROM RadiationReadings WHERE id = ?";
    private static final String SEARCH_BY_STATION =
        "SELECT id, station_id, timestamp, radiation_level, radiation_type, alert_status, notes " +
        "FROM RadiationReadings " +
        "WHERE station_id=? AND (radiation_type ILIKE ? OR alert_status ILIKE ? OR notes ILIKE ?) " +
        "ORDER BY timestamp DESC";

    @Override
    public List<RadiationReading> findAll() throws SQLException {
        List<RadiationReading> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public RadiationReading findById(Integer id) throws SQLException {
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
    public List<RadiationReading> findByStationId(int stationId) throws SQLException {
        List<RadiationReading> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_STATION)) {
            ps.setInt(1, stationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<RadiationReading> searchByStationId(int stationId, String keyword) throws SQLException {
        List<RadiationReading> list = new ArrayList<>();
        String p = "%" + keyword + "%";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SEARCH_BY_STATION)) {
            ps.setInt(1, stationId);
            ps.setString(2, p); ps.setString(3, p); ps.setString(4, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public Integer save(RadiationReading r) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getStationId());
            ps.setTimestamp(2, r.getTimestamp() != null
                    ? Timestamp.valueOf(r.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(3, r.getRadiationLevel());
            ps.setString(4, r.getRadiationType());
            ps.setString(5, r.getAlertStatus());
            ps.setString(6, r.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void update(RadiationReading r) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE)) {
            ps.setTimestamp(1, r.getTimestamp() != null
                    ? Timestamp.valueOf(r.getTimestamp()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(2, r.getRadiationLevel());
            ps.setString(3, r.getRadiationType());
            ps.setString(4, r.getAlertStatus());
            ps.setString(5, r.getNotes());
            ps.setInt(6, r.getId());
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

    private RadiationReading mapRow(ResultSet rs) throws SQLException {
        RadiationReading r = new RadiationReading();
        r.setId(rs.getInt("id"));
        r.setStationId(rs.getInt("station_id"));
        Timestamp ts = rs.getTimestamp("timestamp");
        r.setTimestamp(ts != null ? ts.toLocalDateTime() : null);
        r.setRadiationLevel(rs.getDouble("radiation_level"));
        r.setRadiationType(rs.getString("radiation_type"));
        r.setAlertStatus(rs.getString("alert_status"));
        r.setNotes(rs.getString("notes"));
        return r;
    }
}
