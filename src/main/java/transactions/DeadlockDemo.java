package transactions;
import java.sql.*;
import java.util.function.Consumer;

/**
 * DeadlockDemo
 * ------------
 * Demonstrates a classic deadlock between two transactions and then shows how
 * to prevent it by enforcing a consistent lock-acquisition order.
 *
 * Deadlock scenario
 * -----------------
 *  Transaction A:  locks row 1, then tries to lock row 2.
 *  Transaction B:  locks row 2, then tries to lock row 1.
 *  → Circular wait → deadlock.
 *
 * Derby's lock manager detects the deadlock after a configurable timeout
 * (default ~60 s, reduced here via the derby.locks.deadlockTimeout system
 * property) and aborts one of the victims with SQLState 40001.
 *
 * Prevention
 * ----------
 * Both transactions always acquire locks in the same order (1 then 2).
 * No circular wait can form → no deadlock.
 */
public class DeadlockDemo {

    /**
     * @param causeDeadlock  true  → show the deadlock; A locks 1→2, B locks 2→1
     *                       false → show the fix;     both lock 1→2
     */
    public static void run(boolean causeDeadlock, Consumer<String> log)
            throws InterruptedException {

        // Reduce deadlock-detection timeout so the demo completes quickly.
        System.setProperty("derby.locks.deadlockTimeout", "5");   // seconds
        System.setProperty("derby.locks.waitTimeout",     "10");  // seconds

        String label = causeDeadlock ? "DEADLOCK scenario" : "Fixed – consistent lock order";
        log.accept("═══ Deadlock Demo – " + label + " ═══");

        DerbyDemoManager.resetEmployeeData();

        java.util.concurrent.CountDownLatch aLockedRow1 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch bLockedRow2 = new java.util.concurrent.CountDownLatch(1);

        final boolean[] aDeadlocked = {false};
        final boolean[] bDeadlocked = {false};

        // ── Transaction A ──────────────────────────────────────────────────
        Thread threadA = new Thread(() -> {
            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                log.accept("[A] BEGIN TRANSACTION");

                // A always locks row 1 first.
                updateSalary(conn, 1, 6000);
                log.accept("[A] Locked row 1  (UPDATE salary=6000)");
                aLockedRow1.countDown();

                if (causeDeadlock) {
                    // Wait for B to lock row 2 so the circular wait can form.
                    bLockedRow2.await();
                    log.accept("[A] Attempting to lock row 2 …");
                    updateSalary(conn, 2, 7000); // will deadlock
                    log.accept("[A] Locked row 2  (UPDATE salary=7000)");
                } else {
                    // Safe order: also lock row 2 after row 1.
                    log.accept("[A] Attempting to lock row 2 (same order as B) …");
                    updateSalary(conn, 2, 7000);
                    log.accept("[A] Locked row 2  (UPDATE salary=7000)");
                }

                conn.commit();
                log.accept("[A] COMMIT");

            } catch (SQLException e) {
                if ("40001".equals(e.getSQLState()) || "40XL1".equals(e.getSQLState())) {
                    aDeadlocked[0] = true;
                    log.accept("[A] ⚠ DEADLOCK detected! SQLState=" + e.getSQLState()
                            + " – Transaction A was chosen as victim and rolled back.");
                } else {
                    log.accept("[A] ERROR (" + e.getSQLState() + "): " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TxA-Deadlock");

        // ── Transaction B ──────────────────────────────────────────────────
        Thread threadB = new Thread(() -> {
            // Wait until A has locked row 1.
            try { aLockedRow1.await(); } catch (InterruptedException ie) { return; }

            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                log.accept("[B] BEGIN TRANSACTION");

                if (causeDeadlock) {
                    // B locks row 2 first – opposite order to A.
                    updateSalary(conn, 2, 6000);
                    log.accept("[B] Locked row 2  (UPDATE salary=6000)");
                    bLockedRow2.countDown();
                    log.accept("[B] Attempting to lock row 1 …");
                    updateSalary(conn, 1, 7000); // will deadlock
                    log.accept("[B] Locked row 1  (UPDATE salary=7000)");
                } else {
                    // Safe order: lock row 1 first, then row 2.
                    log.accept("[B] Attempting to lock row 1 (waiting for A to finish) …");
                    updateSalary(conn, 1, 6000);
                    log.accept("[B] Locked row 1  (UPDATE salary=6000)");
                    updateSalary(conn, 2, 7000);
                    log.accept("[B] Locked row 2  (UPDATE salary=7000)");
                    bLockedRow2.countDown();
                }

                conn.commit();
                log.accept("[B] COMMIT");

            } catch (SQLException e) {
                if ("40001".equals(e.getSQLState()) || "40XL1".equals(e.getSQLState())) {
                    bDeadlocked[0] = true;
                    log.accept("[B] ⚠ DEADLOCK detected! SQLState=" + e.getSQLState()
                            + " – Transaction B was chosen as victim and rolled back.");
                } else {
                    log.accept("[B] ERROR (" + e.getSQLState() + "): " + e.getMessage());
                }
                bLockedRow2.countDown(); // unblock A in case B was victim
            }
        }, "TxB-Deadlock");

        threadA.start();
        threadB.start();
        threadA.join(30_000); // wait at most 30 s per transaction
        threadB.join(30_000);

        if (causeDeadlock && (aDeadlocked[0] || bDeadlocked[0])) {
            log.accept("✔ Deadlock was successfully detected and one victim was rolled back.");
            log.accept("  Prevention: always acquire locks in the SAME order (e.g. by PK ascending).");
        } else if (!causeDeadlock) {
            log.accept("✔ No deadlock. Consistent lock order (1→2 for both) prevents circular waits.");
        }
        log.accept("─── Demo complete ───\n");
    }

    private static void updateSalary(Connection conn, int id, double salary) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET salary=? WHERE id=?")) {
            ps.setDouble(1, salary);
            ps.setInt   (2, id);
            ps.executeUpdate();
        }
    }
}
