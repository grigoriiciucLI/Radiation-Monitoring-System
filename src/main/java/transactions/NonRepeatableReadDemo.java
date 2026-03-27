package transactions;

import java.sql.*;
import java.util.function.Consumer;

/**
 * NonRepeatableReadDemo
 * ----------------------
 * Demonstrates the Non-Repeatable Read problem and shows how REPEATABLE READ
 * isolation prevents it.
 *
 * Scenario
 * --------
 *  Transaction A: Reads salary of employee 1  → first read.
 *  Transaction B: Commits an UPDATE to that same salary.
 *  Transaction A: Reads salary of employee 1 again → second read.
 *
 * With READ COMMITTED:
 *   A's second read sees B's committed change, so the two reads differ
 *   → non-repeatable read.
 *
 * With REPEATABLE READ:
 *   A holds a shared lock on the row for the duration of the transaction,
 *   so B's UPDATE blocks until A commits. Both reads return the same value.
 */
public class NonRepeatableReadDemo {

    public static void run(int isolationLevel, Consumer<String> log) throws InterruptedException {

        String levelName = isolationLevel == Connection.TRANSACTION_READ_COMMITTED
                ? "READ COMMITTED" : "REPEATABLE READ";
        log.accept("═══ Non-Repeatable Read Demo – isolation: " + levelName + " ═══");

        DerbyDemoManager.resetEmployeeData();

        java.util.concurrent.CountDownLatch aFirstRead  = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch bCommitted  = new java.util.concurrent.CountDownLatch(1);

        // ── Transaction A ──────────────────────────────────────────────────
        Thread threadA = new Thread(() -> {
            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(isolationLevel);

                log.accept("[A] BEGIN TRANSACTION  (" + levelName + ")");

                double salary1 = DerbyDemoManager.querySalary(conn, 1);
                log.accept("[A] First  read – salary = " + salary1);

                // Let B run and commit its update.
                aFirstRead.countDown();
                bCommitted.await();

                double salary2 = DerbyDemoManager.querySalary(conn, 1);
                log.accept("[A] Second read – salary = " + salary2);

                if (salary1 != salary2) {
                    log.accept("[A] ⚠ NON-REPEATABLE READ! Values differ: "
                            + salary1 + " → " + salary2);
                } else {
                    log.accept("[A] ✔ Both reads returned the same value (" + salary1
                            + "). REPEATABLE READ prevents the inconsistency.");
                }

                conn.commit();
                log.accept("[A] COMMIT");

            } catch (Exception e) {
                log.accept("[A] ERROR: " + e.getMessage());
            }
        }, "TxA-NonRepeatable");

        // ── Transaction B ──────────────────────────────────────────────────
        Thread threadB = new Thread(() -> {
            try { aFirstRead.await(); } catch (InterruptedException ie) { return; }

            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                log.accept("[B] BEGIN TRANSACTION");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE employees SET salary = 12000 WHERE id = 1")) {
                    ps.executeUpdate();
                }
                conn.commit();
                log.accept("[B] UPDATE salary → 12 000 & COMMIT");

            } catch (Exception e) {
                log.accept("[B] ERROR: " + e.getMessage());
            } finally {
                bCommitted.countDown();
            }
        }, "TxB-NonRepeatable");

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        log.accept("─── Demo complete ───\n");
    }
}