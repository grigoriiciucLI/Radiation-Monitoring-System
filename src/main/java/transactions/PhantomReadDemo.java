package transactions;

import java.sql.*;
import java.util.function.Consumer;

/**
 * PhantomReadDemo
 * ---------------
 * Demonstrates the Phantom Read problem and shows how SERIALIZABLE isolation
 * prevents it.
 *
 * Scenario
 * --------
 *  Transaction A: COUNT(*) employees in department 5 → first count.
 *  Transaction B: INSERT a new employee into department 5 and commits.
 *  Transaction A: COUNT(*) again → second count.
 *
 * With REPEATABLE READ:
 *   Row-level locks protect existing rows, but a range/predicate lock is not
 *   held.  B's INSERT succeeds, and A's second count is higher → phantom read.
 *   (Derby's REPEATABLE READ does exhibit this.)
 *
 * With SERIALIZABLE:
 *   Derby acquires a range lock on the predicate "department_id = 5",
 *   which blocks B's INSERT until A commits.  Both counts are identical.
 */
public class PhantomReadDemo {

    public static void run(int isolationLevel, Consumer<String> log) throws InterruptedException {

        String levelName = isolationLevel == Connection.TRANSACTION_REPEATABLE_READ
                ? "REPEATABLE READ" : "SERIALIZABLE";
        log.accept("═══ Phantom Read Demo – isolation: " + levelName + " ═══");

        DerbyDemoManager.resetEmployeeData();

        java.util.concurrent.CountDownLatch aFirstCount = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch bInserted   = new java.util.concurrent.CountDownLatch(1);

        // ── Transaction A ──────────────────────────────────────────────────
        Thread threadA = new Thread(() -> {
            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(isolationLevel);

                log.accept("[A] BEGIN TRANSACTION  (" + levelName + ")");

                int count1 = DerbyDemoManager.queryDeptCount(conn, 5);
                log.accept("[A] First  COUNT(*) WHERE department_id=5 → " + count1);

                aFirstCount.countDown();
                bInserted.await();

                int count2 = DerbyDemoManager.queryDeptCount(conn, 5);
                log.accept("[A] Second COUNT(*) WHERE department_id=5 → " + count2);

                if (count1 != count2) {
                    log.accept("[A] ⚠ PHANTOM READ! Count changed from "
                            + count1 + " to " + count2 + " within the same transaction.");
                } else {
                    log.accept("[A] ✔ Both counts identical (" + count1
                            + "). SERIALIZABLE prevents phantom rows.");
                }

                conn.commit();
                log.accept("[A] COMMIT");

            } catch (Exception e) {
                log.accept("[A] ERROR: " + e.getMessage());
            }
        }, "TxA-Phantom");

        // ── Transaction B ──────────────────────────────────────────────────
        Thread threadB = new Thread(() -> {
            try { aFirstCount.await(); } catch (InterruptedException ie) { return; }

            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                log.accept("[B] BEGIN TRANSACTION");
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO employees(id, name, salary, department_id) VALUES(99,?,?,?)")) {
                    ps.setString(1, "NewEmployee");
                    ps.setDouble(2, 3000.00);
                    ps.setInt   (3, 5);
                    ps.executeUpdate();
                }
                conn.commit();
                log.accept("[B] INSERT new employee into dept 5 & COMMIT");

            } catch (Exception e) {
                log.accept("[B] ERROR (may be lock wait under SERIALIZABLE): " + e.getMessage());
            } finally {
                bInserted.countDown();
            }
        }, "TxB-Phantom");

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        log.accept("─── Demo complete ───\n");
    }
}