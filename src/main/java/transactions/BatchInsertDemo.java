package transactions;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/**
 * BatchInsertDemo
 * ---------------
 * Compares three strategies for inserting 5 000 rows and reports timing.
 *
 * Approach 1 – Auto-commit      : one implicit transaction per INSERT (slowest)
 * Approach 2 – Commit every 100 : 50 explicit commits (moderate)
 * Approach 3 – Single Tx + batch: one commit, JDBC batch flushed every 50 (fastest)
 *
 * Each approach is run RUNS times; averages are computed and returned.
 */
public class BatchInsertDemo {

    private static final int RECORD_COUNT = 5_000;
    private static final int RUNS         = 3;
    private static final int COMMIT_EVERY = 100;
    private static final int BATCH_SIZE   = 50;

    // ── Result container ─────────────────────────────────────────────────

    /**
     * Holds the timing results for one insert approach.
     * Declared as a static nested class (not a local record) so that
     * Lab2Panel – and IntelliJ's type resolver – can reference it as
     * {@code BatchInsertDemo.ApproachResult} without ambiguity.
     */
    public static class ApproachResult {
        private final String name;
        private final long[] timesMs;   // one entry per run
        private final double avgMs;

        public ApproachResult(String name, long[] timesMs) {
            this.name    = name;
            this.timesMs = timesMs;
            long sum = 0;
            for (long t : timesMs) sum += t;
            this.avgMs = timesMs.length == 0 ? 0 : (double) sum / timesMs.length;
        }

        public String  getName()           { return name; }
        public long[]  getTimesMs()        { return timesMs; }
        public long    getTimeMs(int run)  { return timesMs[run]; }
        public double  getAvgMs()          { return avgMs; }
    }

    // ── Public entry point ────────────────────────────────────────────────

    public static List<ApproachResult> run(Consumer<String> log) {
        log.accept("═══ Batch Insert Performance Demo ═══");
        log.accept("Inserting " + RECORD_COUNT + " rows, " + RUNS + " runs each.\n");

        List<ApproachResult> results = new ArrayList<>();
        results.add(benchmark("1 – Auto-commit",       BatchInsertDemo::approach1, log));
        results.add(benchmark("2 – Commit every 100",  BatchInsertDemo::approach2, log));
        results.add(benchmark("3 – Single Tx + batch", BatchInsertDemo::approach3, log));

        // Print summary to log
        log.accept("\n--- Summary ---");
        log.accept(String.format("%-28s %10s %10s %10s %12s",
                "Approach", "Run 1 ms", "Run 2 ms", "Run 3 ms", "Average ms"));
        for (ApproachResult r : results) {
            log.accept(String.format("%-28s %10d %10d %10d %12.1f",
                    r.getName(),
                    r.getTimeMs(0), r.getTimeMs(1), r.getTimeMs(2),
                    r.getAvgMs()));
        }
        log.accept("─── Demo complete ───\n");
        return results;
    }

    // ── Benchmark harness ─────────────────────────────────────────────────

    @FunctionalInterface
    private interface InsertApproach {
        void execute() throws Exception;
    }

    private static ApproachResult benchmark(String name, InsertApproach impl, Consumer<String> log) {
        long[] times = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            cleanupTestRows();
            long start = System.currentTimeMillis();
            try {
                impl.execute();
            } catch (Exception e) {
                log.accept("[ERROR] " + name + " run " + (i + 1) + ": " + e.getMessage());
            }
            times[i] = System.currentTimeMillis() - start;
            log.accept(name + "  run " + (i + 1) + ": " + times[i] + " ms");
        }
        return new ApproachResult(name, times);
    }

    // ── Approach 1: auto-commit (one tx per INSERT) ───────────────────────

    private static void approach1() throws Exception {
        try (Connection conn = DerbyDemoManager.getConnection()) {
            // auto-commit is ON by default – no need to change it
            String sql = "INSERT INTO employees(id, name, salary, department_id) VALUES(?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORD_COUNT; i++) {
                    ps.setInt   (1, 1000 + i);
                    ps.setString(2, "TestEmp_" + i);
                    ps.setDouble(3, 3000.00 + i);
                    ps.setInt   (4, 9);
                    ps.executeUpdate();   // implicit commit on every row
                }
            }
        }
    }

    // ── Approach 2: manual commit every 100 rows ──────────────────────────

    private static void approach2() throws Exception {
        try (Connection conn = DerbyDemoManager.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO employees(id, name, salary, department_id) VALUES(?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORD_COUNT; i++) {
                    ps.setInt   (1, 1000 + i);
                    ps.setString(2, "TestEmp_" + i);
                    ps.setDouble(3, 3000.00 + i);
                    ps.setInt   (4, 9);
                    ps.executeUpdate();
                    if ((i + 1) % COMMIT_EVERY == 0) {
                        conn.commit();
                    }
                }
                conn.commit();   // flush any remaining rows
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── Approach 3: single transaction + JDBC batch ───────────────────────

    private static void approach3() throws Exception {
        try (Connection conn = DerbyDemoManager.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO employees(id, name, salary, department_id) VALUES(?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < RECORD_COUNT; i++) {
                    ps.setInt   (1, 1000 + i);
                    ps.setString(2, "TestEmp_" + i);
                    ps.setDouble(3, 3000.00 + i);
                    ps.setInt   (4, 9);
                    ps.addBatch();
                    if ((i + 1) % BATCH_SIZE == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();   // flush remaining rows
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /** Removes test rows inserted by the benchmark (department_id = 9). */
    private static void cleanupTestRows() {
        try (Connection conn = DerbyDemoManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM employees WHERE department_id = 9")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // Non-fatal – table may simply have no test rows yet.
        }
    }
}