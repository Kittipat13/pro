package view;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;

public class AdminGUI extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private String filename = "rates.csv";

    public AdminGUI() {
        setTitle("Admin - Manage Exchange Rates");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // สร้างตารางพร้อม model
        tableModel = new DefaultTableModel(new Object[]{"Currency", "Rate"}, 0) {
            // กำหนดให้แค่คอลัมน์ Rate สามารถแก้ไขได้
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // โหลดข้อมูลจากไฟล์ rates.csv
        loadRatesFromFile();

        // ปุ่มบันทึก
        JButton saveButton = new JButton("Save Rates");
        saveButton.addActionListener(e -> {
            if (saveRatesToFile()) {
                JOptionPane.showMessageDialog(this, "Saved successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Error saving file.");
            }
        });

        // จัด layout
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);
    }

    private void loadRatesFromFile() {
        tableModel.setRowCount(0); // เคลียร์ข้อมูลเก่า
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String currency = parts[0].trim();
                    String rate = parts[1].trim();
                    tableModel.addRow(new Object[]{currency, rate});
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading rates file: " + e.getMessage());
        }
    }

    private boolean saveRatesToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String currency = tableModel.getValueAt(i, 0).toString();
                String rateStr = tableModel.getValueAt(i, 1).toString();
                // ตรวจสอบว่าค่า rate เป็นตัวเลข
                double rate = Double.parseDouble(rateStr);
                pw.println(currency + "," + rate);
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    // สำหรับทดสอบเปิด GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdminGUI().setVisible(true);
        });
    }
}