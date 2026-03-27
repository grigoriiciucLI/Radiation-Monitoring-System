package transactions;
import java.sql.*;
import java.util.function.Consumer;

/**
 * DirtyReadDemo
 * -------------
 * Demonstrates the Dirty Read concurrency problem and shows how READ COMMITTED
 * isolation prevents it.
 *
 * Scenario
 * --------
 *  Transaction A: UPDATE salary to 10 000 but does NOT commit yet.
 *  Transaction B: Reads the salary while A's change is still uncommitted.
 *  Transaction A: ROLLBACK – the salary reverts to the original value.
 *
 * With READ UNCOMMITTED (Derby supports this):
 *   Transaction B reads 10 000 – a value that was never actually committed,
 *   i.e. a "dirty" value that no longer exists after the rollback.
 *
 * With READ COMMITTED:
 *   Transaction B is forced to wait (or reads the last committed value),
 *   so it always sees the real, committed salary.
 *
 * Threading model
 * ---------------
 * Each transaction runs in its own thread to simulate two concurrent users.
 * CountDownLatches are used as explicit sync points so the interleaving is
 * deterministic and visible in the log.
 */
public class DirtyReadDemo {

    /**
     * Run the dirty-read scenario at the given isolation level.
     *
     * @param isolationLevel  Connection.TRANSACTION_READ_UNCOMMITTED  or
     *                        Connection.TRANSACTION_READ_COMMITTED
     * @param log             callback that appends one line to the GUI log area
     */
    public static void run(int isolationLevel, Consumer<String> log) throws InterruptedException {

        String levelName = isolationLevel == Connection.TRANSACTION_READ_UNCOMMITTED
                ? "READ UNCOMMITTED" : "READ COMMITTED";
        log.accept("═══ Dirty Read Demo – isolation: " + levelName + " ═══");

        // Reset to a known state before each run.
        DerbyDemoManager.resetEmployeeData();

        // Latch A → signals that A has performed the UPDATE (but not committed).
        java.util.concurrent.CountDownLatch aUpdated = new java.util.concurrent.CountDownLatch(1);
        // Latch B → signals that B has finished reading so A can roll back.
        java.util.concurrent.CountDownLatch bRead    = new java.util.concurrent.CountDownLatch(1);

        // ── Transaction A ──────────────────────────────────────────────────
        Thread threadA = new Thread(() -> {
            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                log.accept("[A] BEGIN TRANSACTION");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE employees SET salary = 10000 WHERE id = 1")) {
                    ps.executeUpdate();
                }
                log.accept("[A] UPDATE salary → 10 000  (NOT committed yet)");

                // Signal B that the update is done.
                aUpdated.countDown();
                // Wait for B to finish reading.
                bRead.await();

                conn.rollback();
                log.accept("[A] ROLLBACK – salary reverted to original value");

            } catch (Exception e) {
                log.accept("[A] ERROR: " + e.getMessage());
            }
        }, "TxA-DirtyRead");

        // ── Transaction B ──────────────────────────────────────────────────
        Thread threadB = new Thread(() -> {
            try {
                // Wait until A has applied (but not committed) the update.
                aUpdated.await();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }

            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(isolationLevel);

                log.accept("[B] BEGIN TRANSACTION  (" + levelName + ")");
                double salary = DerbyDemoManager.querySalary(conn, 1);
                log.accept("[B] SELECT salary FROM employees WHERE id=1  →  " + salary);

                if (isolationLevel == Connection.TRANSACTION_READ_UNCOMMITTED && salary == 10000) {
                    log.accept("[B] ⚠ DIRTY READ detected! Read 10 000 which was never committed.");
                } else {
                    log.accept("[B] ✔ Clean read – saw the committed value: " + salary);
                }

                conn.commit();
                log.accept("[B] COMMIT");

            } catch (Exception e) {
                log.accept("[B] ERROR: " + e.getMessage());
            } finally {
                bRead.countDown(); // let A roll back
            }
        }, "TxB-DirtyRead");

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        log.accept("─── Demo complete ───\n");
    }
}
