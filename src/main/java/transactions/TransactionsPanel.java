package transactions;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
/**
 * Lab2Panel
 * ---------
 * Swing panel that hosts all Lab-2 transaction demonstrations.
 * Added as a second tab inside MainFrame's JTabbedPane.
 *
 * All demos run on background threads so the Swing EDT is never blocked.
 */
public class TransactionsPanel extends JPanel {

    private final JTextArea logArea = new JTextArea();

    public TransactionsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(buildButtonPanel(), BorderLayout.NORTH);
        add(buildLogPanel(),    BorderLayout.CENTER);
        add(buildBatchPanel(),  BorderLayout.SOUTH);

        // Initialise Derby schema once – fast and idempotent.
        SwingUtilities.invokeLater(() -> {
            try {
                DerbyDemoManager.initSchema();
                log("Derby embedded database initialised. Ready to run demos.");
            } catch (Exception e) {
                log("ERROR initialising Derby: " + e.getMessage());
            }
        });
    }

    // ── Button panel ──────────────────────────────────────────────────────

    private JPanel buildButtonPanel() {
        JPanel outer = new JPanel(new GridLayout(3, 1, 4, 4));

        // Row 1 – Dirty Read
        JPanel row1 = titledRow("A. Dirty Read");
        row1.add(btn("Show Problem\n(READ UNCOMMITTED)", new Color(200, 70, 70),
                () -> DirtyReadDemo.run(java.sql.Connection.TRANSACTION_READ_UNCOMMITTED, this::log)));
        row1.add(btn("Show Prevention\n(READ COMMITTED)", new Color(55, 130, 55),
                () -> DirtyReadDemo.run(java.sql.Connection.TRANSACTION_READ_COMMITTED, this::log)));
        outer.add(row1);

        // Row 2 – Non-Repeatable + Phantom
        JPanel row2 = titledRow("B. Non-Repeatable Read          C. Phantom Read");
        row2.add(btn("Non-Repeatable\n(READ COMMITTED)", new Color(200, 110, 30),
                () -> NonRepeatableReadDemo.run(java.sql.Connection.TRANSACTION_READ_COMMITTED, this::log)));
        row2.add(btn("Non-Repeatable\n(REPEATABLE READ)", new Color(55, 130, 55),
                () -> NonRepeatableReadDemo.run(java.sql.Connection.TRANSACTION_REPEATABLE_READ, this::log)));
        row2.add(btn("Phantom\n(REPEATABLE READ)", new Color(200, 110, 30),
                () -> PhantomReadDemo.run(java.sql.Connection.TRANSACTION_REPEATABLE_READ, this::log)));
        row2.add(btn("Phantom\n(SERIALIZABLE)", new Color(55, 130, 55),
                () -> PhantomReadDemo.run(java.sql.Connection.TRANSACTION_SERIALIZABLE, this::log)));
        outer.add(row2);

        // Row 3 – Lost Update + Deadlock
        JPanel row3 = titledRow("D. Lost Update          E. Deadlock");
        row3.add(btn("Lost Update\n(problem)", new Color(140, 55, 160),
                () -> LostUpdateDemo.run(false, this::log)));
        row3.add(btn("Lost Update\n(prevention)", new Color(55, 130, 55),
                () -> LostUpdateDemo.run(true, this::log)));
        row3.add(btn("Deadlock\n(scenario)", new Color(180, 45, 45),
                () -> DeadlockDemo.run(true, this::log)));
        row3.add(btn("Deadlock\n(fixed order)", new Color(55, 130, 55),
                () -> DeadlockDemo.run(false, this::log)));
        outer.add(row3);

        return outer;
    }

    // ── Log panel ─────────────────────────────────────────────────────────

    private JScrollPane buildLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 30));
        logArea.setForeground(new Color(180, 230, 180));
        logArea.setCaretColor(Color.WHITE);

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(titledBorder("Transaction Log"));
        sp.setPreferredSize(new Dimension(0, 260));
        return sp;
    }

    // ── Batch performance panel ────────────────────────────────────────────

    private JPanel buildBatchPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(titledBorder("F. Batch Insert Performance"));

        String[] cols = {"Approach", "Run 1 (ms)", "Run 2 (ms)", "Run 3 (ms)", "Average (ms)"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton runBtn = new JButton("▶  Run Batch Benchmark  (3 × 5 000 rows each approach)");
        runBtn.setBackground(new Color(50, 100, 180));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.setBorderPainted(false);
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 13));

        runBtn.addActionListener(e -> {
            runBtn.setEnabled(false);
            tableModel.setRowCount(0);
            log("Starting batch benchmark – this may take ~20 seconds …");

            new Thread(() -> {
                // BatchInsertDemo.run() is a blocking call – runs on this background thread.
                List<BatchInsertDemo.ApproachResult> results = BatchInsertDemo.run(this::log);

                SwingUtilities.invokeLater(() -> {
                    for (BatchInsertDemo.ApproachResult r : results) {
                        tableModel.addRow(new Object[]{
                                r.getName(),
                                r.getTimeMs(0),
                                r.getTimeMs(1),
                                r.getTimeMs(2),
                                String.format("%.1f", r.getAvgMs())
                        });
                    }
                    runBtn.setEnabled(true);
                });
            }, "BatchBenchmark").start();
        });

        panel.add(runBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(0, 155));
        return panel;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Thread-safe log append. */
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @FunctionalInterface
    private interface DemoTask { void run() throws Exception; }

    /** Creates a coloured button that runs a demo on a background thread. */
    private JButton btn(String text, Color bg, DemoTask task) {
        JButton b = new JButton("<html><center>" + text.replace("\n", "<br>") + "</center></html>");
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setPreferredSize(new Dimension(155, 46));
        b.addActionListener(e -> new Thread(() -> {
            try {
                task.run();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log("ERROR: " + ex.getMessage());
            }
        }, "DemoThread").start());
        return b;
    }

    private JPanel titledRow(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setBorder(titledBorder(title));
        return p;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 115), 1), "  " + title + "  ");
        b.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
        b.setTitleColor(new Color(55, 100, 165));
        return b;
    }

    // ── Standalone launcher (for quick testing) ───────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Lab 2 – Transaction Demos");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(920, 720);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new TransactionsPanel());
            frame.setVisible(true);
        });
    }
}