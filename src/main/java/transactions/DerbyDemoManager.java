package transactions;

import java.sql.*;

/**
 * DerbyDemoManager
 * -----------------
 * Manages an embedded Apache Derby database used exclusively for the Lab-2
 * transaction-isolation and batch-performance demonstrations.
 *
 * Why Derby and not PostgreSQL?
 *   PostgreSQL silently promotes READ UNCOMMITTED to READ COMMITTED, so a
 *   dirty-read can never be observed through it.  Derby honours READ UNCOMMITTED
 *   faithfully, which is exactly what the lab specification requires.
 *
 * The connection URL "jdbc:derby:lab2-demo-db;create=true" creates the database
 * files in a sub-directory called "lab2-demo-db" inside the JVM working directory
 * (i.e. the project root when run from Maven/IntelliJ).  No separate server
 * installation is needed.
 */
public class DerbyDemoManager {

    public static final String DERBY_URL = "jdbc:derby:lab2-demo-db;create=true";

    static {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Apache Derby driver not found. Add the 'derby' dependency to pom.xml.", e);
        }
    }

    /** Returns a fresh connection to the embedded Derby database. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DERBY_URL);
    }

    /**
     * Creates the demo schema (employees table) and populates seed rows.
     * Safe to call multiple times – uses IF NOT EXISTS guards.
     * Called once at application startup before any demo is run.
     */
    public static void initSchema() {
        try (Connection conn = getConnection();
             Statement st   = conn.createStatement()) {

            // Derby does not support IF NOT EXISTS for CREATE TABLE,
            // so we catch the "table already exists" error code (X0Y32).
            try {
                st.executeUpdate(
                        "CREATE TABLE employees (" +
                                "  id            INT PRIMARY KEY," +
                                "  name          VARCHAR(100)," +
                                "  salary        DECIMAL(10,2)," +
                                "  department_id INT" +
                                ")"
                );
            } catch (SQLException e) {
                if (!e.getSQLState().equals("X0Y32")) throw e; // re-throw if unexpected
            }

            // Upsert seed data so reruns start from a known state.
            resetEmployeeData(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Derby schema initialisation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resets employee rows to the canonical starting values used by the demos.
     * Called automatically by initSchema() and explicitly between demo runs.
     */
    public static void resetEmployeeData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM employees");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO employees(id, name, salary, department_id) VALUES(?,?,?,?)")) {
            Object[][] rows = {
                    {1, "Alice",   5000.00, 5},
                    {2, "Bob",     4500.00, 5},
                    {3, "Charlie", 6000.00, 3},
                    {4, "Diana",   5500.00, 3},
                    {5, "Eve",     4800.00, 5},
            };
            for (Object[] r : rows) {
                ps.setInt   (1, (Integer) r[0]);
                ps.setString(2, (String)  r[1]);
                ps.setDouble(3, (Double)  r[2]);
                ps.setInt   (4, (Integer) r[3]);
                ps.executeUpdate();
            }
        }
    }

    /** Convenience reset that opens its own connection. */
    public static void resetEmployeeData() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            resetEmployeeData(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Reset failed: " + e.getMessage(), e);
        }
    }

    /** Returns the current salary of the employee with the given id. */
    public static double querySalary(Connection conn, int employeeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT salary FROM employees WHERE id = ?")) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        throw new SQLException("Employee " + employeeId + " not found");
    }

    /** Returns the count of employees in the given department. */
    public static int queryDeptCount(Connection conn, int departmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM employees WHERE department_id = ?")) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
