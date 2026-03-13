import ui.MainFrame;
import util.DatabaseManager;
import javax.swing.*;

/**
 * Entry point for the Radiation Monitoring System desktop application.
 */
public class Main {

    public static void main(String[] args) {
        if (!DatabaseManager.testConnection()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot connect to the database.\n" +
                            "Please check your credentials in DatabaseManager.java.",
                    "Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}