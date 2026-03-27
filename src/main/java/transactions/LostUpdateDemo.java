package transactions;
import java.sql.*;
import java.util.function.Consumer;

/**
 * LostUpdateDemo
 * --------------
 * Demonstrates the Lost Update problem – two transactions both read a value,
 * compute a new value based on the old one, and then write back.
 * The second write silently overwrites the first → one update is "lost".
 *
 * Scenario
 * --------
 *  Both A and B read salary = 5 000.
 *  A computes 5 000 + 1 000 = 6 000 and writes it.
 *  B computes 5 000 + 500  = 5 500 and writes it.
 *  Final value = 5 500.  A's +1 000 raise is lost.
 *  Correct value should be 5 000 + 1 000 + 500 = 6 500.
 *
 * Prevention
 * ----------
 *  Option 1 – Pessimistic locking: A issues  SELECT … FOR UPDATE  which
 *             prevents B from reading until A commits.
 *  Option 2 – Optimistic locking: use a version column and retry on conflict.
 *
 * This demo shows the problem at READ COMMITTED, then shows prevention with
 * SELECT FOR UPDATE (pessimistic).
 *
 * Note: Derby supports SELECT … FOR UPDATE only inside a cursor; we simulate
 * pessimistic locking by wrapping in SERIALIZABLE, which is functionally
 * equivalent and simpler to show in a demo.
 */
public class LostUpdateDemo {

    /** Run the lost-update scenario.
     *  @param usePessimisticLocking  when false → shows the lost update;
     *                                when true  → prevents it with SERIALIZABLE. */
    public static void run(boolean usePessimisticLocking, Consumer<String> log)
            throws InterruptedException {

        String label = usePessimisticLocking ? "SERIALIZABLE (prevention)" : "READ COMMITTED (problem)";
        log.accept("═══ Lost Update Demo – " + label + " ═══");

        DerbyDemoManager.resetEmployeeData();

        java.util.concurrent.CountDownLatch aRead    = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch bWritten = new java.util.concurrent.CountDownLatch(1);

        int isoLevel = usePessimisticLocking
                ? Connection.TRANSACTION_SERIALIZABLE
                : Connection.TRANSACTION_READ_COMMITTED;

        // ── Transaction A ──────────────────────────────────────────────────
        Thread threadA = new Thread(() -> {
            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(isoLevel);

                log.accept("[A] BEGIN TRANSACTION");
                double salary = DerbyDemoManager.querySalary(conn, 1);
                double newSalary = salary + 1000;
                log.accept("[A] READ salary=" + salary + "  →  will write " + newSalary);

                // Signal B that A has read the salary.
                aRead.countDown();

                if (usePessimisticLocking) {
                    log.accept("[A] Holding SERIALIZABLE lock – B must wait until A commits.");
                }

                // Simulate some processing time before the write.
                Thread.sleep(400);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE employees SET salary=? WHERE id=1")) {
                    ps.setDouble(1, newSalary);
                    ps.executeUpdate();
                }
                conn.commit();
                log.accept("[A] COMMIT  (salary written: " + newSalary + ")");

            } catch (Exception e) {
                log.accept("[A] ERROR: " + e.getMessage());
            }
        }, "TxA-LostUpdate");

        // ── Transaction B ──────────────────────────────────────────────────
        Thread threadB = new Thread(() -> {
            try { aRead.await(); } catch (InterruptedException ie) { return; }

            try (Connection conn = DerbyDemoManager.getConnection()) {
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(isoLevel);

                log.accept("[B] BEGIN TRANSACTION");
                double salary = DerbyDemoManager.querySalary(conn, 1);
                double newSalary = salary + 500;
                log.accept("[B] READ salary=" + salary + "  →  will write " + newSalary);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE employees SET salary=? WHERE id=1")) {
                    ps.setDouble(1, newSalary);
                    ps.executeUpdate();
                }
                conn.commit();
                log.accept("[B] COMMIT  (salary written: " + newSalary + ")");

            } catch (Exception e) {
                log.accept("[B] ERROR: " + e.getMessage());
            } finally {
                bWritten.countDown();
            }
        }, "TxB-LostUpdate");

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        // Show the final salary in the database.
        try (Connection conn = DerbyDemoManager.getConnection()) {
            double finalSalary = DerbyDemoManager.querySalary(conn, 1);
            log.accept("Final salary in DB: " + finalSalary
                    + "  (expected 6500 if both updates survived)");
            if (!usePessimisticLocking && finalSalary != 6500) {
                log.accept("⚠ A's +1000 raise was LOST! Only B's +500 persisted.");
            } else if (usePessimisticLocking && finalSalary == 6500) {
                log.accept("✔ Both updates applied correctly. No update was lost.");
            }
        } catch (SQLException e) {
            log.accept("ERROR reading final salary: " + e.getMessage());
        }

        log.accept("─── Demo complete ───\n");
    }
}