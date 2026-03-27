package ui;
import model.MonitoringStation;
import model.RadiationReading;
import repository.ReadingRepositoryImpl;
import repository.StationRepositoryImpl;
import service.ReadingService;
import service.StationService;
import transactions.TransactionsPanel;
import validator.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MainFrame – primary Swing window for the Radiation Monitoring System.
 *
 * Lab-2 change: the window now uses a JTabbedPane so the existing monitoring
 * UI lives on the first tab and the Lab-2 transaction demonstrations live on
 * the second tab.  No existing logic was modified.
 */
public class MainFrame extends JFrame {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StationService stationService =
            new StationService(new StationRepositoryImpl());
    private final ReadingService readingService =
            new ReadingService(new ReadingRepositoryImpl());

    private List<MonitoringStation> stations;
    private List<RadiationReading>  readings;
    private int selectedStationId = -1;

    private final DefaultTableModel stationModel = new DefaultTableModel(
            new String[]{"ID","Location","Type","Status","Established","Operator"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable stationTable = new JTable(stationModel);

    private final DefaultTableModel readingModel = new DefaultTableModel(
            new String[]{"ID","Timestamp","Level (mSv)","Type","Alert Status","Notes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable readingTable = new JTable(readingModel);

    private final JTextField tfStationSearch = new JTextField(16);
    private final JTextField tfReadingSearch  = new JTextField(14);

    public MainFrame() {
        super("Radiation Monitoring System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 760);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);
        buildUI();
        applyTheme();
        loadStations();
    }

    // ── UI construction ──────────────────────────────────────────────────

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1 – existing monitoring application
        JPanel monitoringTab = new JPanel(new BorderLayout());
        monitoringTab.add(buildTopBar(),    BorderLayout.NORTH);
        monitoringTab.add(buildSplitPane(), BorderLayout.CENTER);
        tabs.addTab("📡  Radiation Monitor", monitoringTab);

        // Tab 2 – Lab 2 transaction demonstrations
        tabs.addTab("🔬  Lab 2 – Transactions", new TransactionsPanel());

        setContentPane(tabs);
    }

    // ── Everything below is unchanged from the original MainFrame ────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 6));
        bar.setBorder(new EmptyBorder(10, 12, 8, 12));
        bar.setBackground(new Color(28, 28, 42));

        JLabel title = new JLabel("  RADIATION MONITORING SYSTEM");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(0, 220, 180));
        bar.add(title, BorderLayout.WEST);

        JButton btnSearch  = styledBtn("Search",  new Color(70, 130, 180));
        JButton btnRefresh = styledBtn("Refresh", new Color(55, 120, 55));
        btnSearch.addActionListener(e  -> searchStations());
        btnRefresh.addActionListener(e -> { tfStationSearch.setText(""); loadStations(); });

        JLabel lbl = new JLabel("Search stations:");
        lbl.setForeground(Color.LIGHT_GRAY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(lbl); right.add(tfStationSearch);
        right.add(btnSearch); right.add(btnRefresh);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JSplitPane buildSplitPane() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildStationPanel(), buildReadingPanel());
        split.setDividerLocation(260);
        split.setResizeWeight(0.35);
        split.setDividerSize(6);
        return split;
    }

    private JPanel buildStationPanel() {
        stationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stationTable.setRowHeight(22);
        stationTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        stationTable.setAutoCreateRowSorter(true);
        stationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onStationSelected();
        });
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(titledBorder("Monitoring Stations"));
        p.add(new JScrollPane(stationTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildReadingPanel() {
        readingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        readingTable.setRowHeight(22);
        readingTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        readingTable.setAutoCreateRowSorter(true);
        readingTable.setDefaultRenderer(Object.class, new AlertStatusRenderer());
        readingTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editReading();
            }
        });

        JButton btnAdd    = styledBtn("+ Add",  new Color(50, 140, 80));
        JButton btnEdit   = styledBtn("Edit",   new Color(70, 130, 180));
        JButton btnDelete = styledBtn("Delete", new Color(185, 55, 55));
        JButton btnSearch = styledBtn("Search", new Color(100, 100, 145));

        btnAdd.addActionListener(e    -> addReading());
        btnEdit.addActionListener(e   -> editReading());
        btnDelete.addActionListener(e -> deleteReading());
        btnSearch.addActionListener(e -> searchReadings());

        JLabel lbl = new JLabel("Filter: ");
        lbl.setForeground(Color.DARK_GRAY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.add(btnAdd); toolbar.add(btnEdit); toolbar.add(btnDelete);
        toolbar.add(Box.createHorizontalStrut(14));
        toolbar.add(lbl); toolbar.add(tfReadingSearch); toolbar.add(btnSearch);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(titledBorder("Radiation Readings  (select a station above)"));
        p.add(new JScrollPane(readingTable), BorderLayout.CENTER);
        p.add(toolbar, BorderLayout.SOUTH);
        return p;
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadStations() {
        try {
            stations = stationService.findAll();
            fillStationTable(stations);
            readingModel.setRowCount(0);
            selectedStationId = -1;
        } catch (SQLException ex) { dbError("Failed to load stations", ex); }
    }

    private void searchStations() {
        String kw = tfStationSearch.getText().trim();
        if (kw.isEmpty()) { loadStations(); return; }
        try {
            stations = stationService.search(kw);
            fillStationTable(stations);
        } catch (SQLException ex) { dbError("Station search failed", ex); }
    }

    private void fillStationTable(List<MonitoringStation> list) {
        stationModel.setRowCount(0);
        for (MonitoringStation s : list) {
            stationModel.addRow(new Object[]{
                    s.getId(), s.getLocation(), s.getType(), s.getStatus(),
                    s.getEstablishedDate() != null ? s.getEstablishedDate().toString() : "",
                    s.getOperator()
            });
        }
    }

    private void onStationSelected() {
        int vr = stationTable.getSelectedRow();
        if (vr < 0) return;
        int mr = stationTable.convertRowIndexToModel(vr);
        selectedStationId = (int) stationModel.getValueAt(mr, 0);
        loadReadings(selectedStationId);
    }

    private void loadReadings(int stationId) {
        try {
            readings = readingService.findByStation(stationId);
            fillReadingTable(readings);
        } catch (SQLException ex) { dbError("Failed to load readings", ex); }
    }

    private void searchReadings() {
        if (selectedStationId < 0) { info("Please select a station first."); return; }
        String kw = tfReadingSearch.getText().trim();
        if (kw.isEmpty()) { loadReadings(selectedStationId); return; }
        try {
            readings = readingService.search(selectedStationId, kw);
            fillReadingTable(readings);
        } catch (SQLException ex) { dbError("Reading search failed", ex); }
    }

    private void fillReadingTable(List<RadiationReading> list) {
        readingModel.setRowCount(0);
        for (RadiationReading r : list) {
            readingModel.addRow(new Object[]{
                    r.getId(),
                    r.getTimestamp() != null ? r.getTimestamp().format(DT_FMT) : "",
                    String.format("%.4f", r.getRadiationLevel()),
                    r.getRadiationType(),
                    r.getAlertStatus(),
                    r.getNotes()
            });
        }
    }

    // ── CRUD actions ──────────────────────────────────────────────────────

    private void addReading() {
        if (selectedStationId < 0) { info("Please select a station first."); return; }
        ReadingFormDialog dlg = new ReadingFormDialog(this, selectedStationId);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            RadiationReading r = dlg.getReading();
            try {
                readingService.create(selectedStationId, r.getTimestamp(),
                        r.getRadiationLevel(), r.getRadiationType(),
                        r.getAlertStatus(), r.getNotes());
                loadReadings(selectedStationId);
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) { dbError("Add failed", ex); }
        }
    }

    private void editReading() {
        int vr = readingTable.getSelectedRow();
        if (vr < 0) { info("Please select a reading to edit."); return; }
        int mr  = readingTable.convertRowIndexToModel(vr);
        int rid = (int) readingModel.getValueAt(mr, 0);
        RadiationReading existing = readings.stream()
                .filter(r -> r.getId() == rid).findFirst().orElse(null);
        if (existing == null) return;
        ReadingFormDialog dlg = new ReadingFormDialog(this, existing, selectedStationId);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            RadiationReading r = dlg.getReading();
            try {
                readingService.edit(rid, selectedStationId, r.getTimestamp(),
                        r.getRadiationLevel(), r.getRadiationType(),
                        r.getAlertStatus(), r.getNotes());
                loadReadings(selectedStationId);
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) { dbError("Update failed", ex); }
        }
    }

    private void deleteReading() {
        int vr = readingTable.getSelectedRow();
        if (vr < 0) { info("Please select a reading to delete."); return; }
        int mr  = readingTable.convertRowIndexToModel(vr);
        int rid = (int) readingModel.getValueAt(mr, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete reading ID " + rid + "?\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                readingService.delete(rid);
                loadReadings(selectedStationId);
            } catch (SQLException ex) { dbError("Delete failed", ex); }
        }
    }

    // ── Styling helpers ──────────────────────────────────────────────────

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void dbError(String ctx, Exception ex) {
        JOptionPane.showMessageDialog(this, ctx + ":\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 115), 1),
                "  " + title + "  ");
        b.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        b.setTitleColor(new Color(55, 100, 165));
        return b;
    }

    private void applyTheme() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        Color grid = new Color(210, 210, 222);
        stationTable.setGridColor(grid);
        readingTable.setGridColor(grid);
    }

    // ── Alert-status row colouring ────────────────────────────────────────

    private static class AlertStatusRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_NORMAL   = new Color(220, 245, 220);
        private static final Color COLOR_WARNING  = new Color(255, 245, 180);
        private static final Color COLOR_CRITICAL = new Color(255, 200, 200);
        private static final Color COLOR_SELECTED = new Color(175, 210, 245);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setBackground(COLOR_SELECTED);
            } else {
                int mr = table.convertRowIndexToModel(row);
                Object status = table.getModel().getValueAt(mr, 4);
                if ("Critical".equals(status))     c.setBackground(COLOR_CRITICAL);
                else if ("Warning".equals(status)) c.setBackground(COLOR_WARNING);
                else                               c.setBackground(COLOR_NORMAL);
            }
            return c;
        }
    }
}