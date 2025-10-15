package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserGUI {

    // ---------- Currency ----------
    static class Currency {
        private String code;
        private double rate;

        public Currency(String code, double rate) {
            this.code = code;
            this.rate = rate;
        }

        public String getCode() {
            return code;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }
    }

    // ---------- CurrencyManager ----------
    static class CurrencyManager {
        private Map<String, Currency> currencies = new HashMap<>();

        public void loadRatesFromFile(String filename) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String code = parts[0].trim();
                        double rate = Double.parseDouble(parts[1].trim());
                        currencies.put(code, new Currency(code, rate));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading rates: " + e.getMessage());
            }
        }

        public Currency getCurrency(String code) {
            return currencies.get(code);
        }

        public Collection<Currency> getAllCurrencies() {
            return currencies.values();
        }
    }

    // ---------- CurrencyConverter ----------
    static class CurrencyConverter {
        private CurrencyManager manager;

        public CurrencyConverter(CurrencyManager manager) {
            this.manager = manager;
        }

        public double convert(String fromCode, String toCode, double amount) {
            Currency from = manager.getCurrency(fromCode);
            Currency to = manager.getCurrency(toCode);

            if (from == null || to == null) {
                throw new IllegalArgumentException("Invalid currency code");
            }

            double thbAmount = amount * from.getRate(); // แปลงเป็น THB ก่อน
            return thbAmount / to.getRate(); // แปลงจาก THB ไปสกุลปลายทาง
        }
    }

    // ---------- HistoryRecord ----------
    static class HistoryRecord {
        private String fromCurrency;
        private String toCurrency;
        private double amount;
        private double result;
        private LocalDateTime timestamp;

        public HistoryRecord(String from, String to, double amount, double result) {
            this.fromCurrency = from;
            this.toCurrency = to;
            this.amount = amount;
            this.result = result;
            this.timestamp = LocalDateTime.now();
        }

        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "[" + timestamp.format(fmt) + "] " + String.format("%.2f %s → %.2f %s", amount, fromCurrency, result, toCurrency);
        }

        public String toCSV() {
            return String.format("%s,%s,%.2f,%.2f,%s", fromCurrency, toCurrency, amount, result, timestamp.toString());
        }

        public static HistoryRecord fromCSV(String line) {
            String[] parts = line.split(",");
            if (parts.length != 5) return null;

            String from = parts[0];
            String to = parts[1];
            double amount = Double.parseDouble(parts[2]);
            double result = Double.parseDouble(parts[3]);
            LocalDateTime time = LocalDateTime.parse(parts[4]);

            HistoryRecord record = new HistoryRecord(from, to, amount, result);
            record.timestamp = time;
            return record;
        }

        public String getFromCurrency() { return fromCurrency; }
        public String getToCurrency() { return toCurrency; }
        public double getAmount() { return amount; }
        public double getResult() { return result; }
    }

    // ---------- ExchangeHistory ----------
    static class ExchangeHistory {
        private List<HistoryRecord> records = new ArrayList<>();

        public void addRecord(HistoryRecord record) {
            records.add(record);
        }

        public List<HistoryRecord> getAllRecords() {
            return records;
        }

        public void saveToFile(String filename) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (HistoryRecord r : records) {
                    writer.write(r.toCSV());
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Error saving history: " + e.getMessage());
            }
        }

        public void loadFromFile(String filename) {
            records.clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    HistoryRecord r = HistoryRecord.fromCSV(line);
                    if (r != null) records.add(r);
                }
            } catch (IOException e) {
                System.out.println("No existing history found.");
            }
        }
    }

    // ---------- SplitColorPanel ----------
    static class SplitColorPanel extends JPanel {
        private Color leftColor;
        private Color rightColor;

        public SplitColorPanel(Color leftColor, Color rightColor) {
            this.leftColor = leftColor;
            this.rightColor = rightColor;
            setLayout(null);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();
            int leftWidth = (int)(width * 0.3);
            int rightWidth = width - leftWidth;

            g.setColor(leftColor);
            g.fillRect(0, 0, leftWidth, height);

            g.setColor(rightColor);
            g.fillRect(leftWidth, 0, rightWidth, height);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(leftWidth, 0, leftWidth, height);
        }
    }

    // ---------- CurrencyChartFrame (อัตราแลกเปลี่ยน) ----------
    static class CurrencyChartFrame extends JFrame {
        public CurrencyChartFrame(Map<String, Currency> currencies) {
            setTitle("Currency Exchange Rate Chart");
            setSize(900, 500);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            add(new CurrencyChartPanel(currencies));
        }

        static class CurrencyChartPanel extends JPanel {
            private Map<String, Currency> data;

            public CurrencyChartPanel(Map<String, Currency> data) {
                this.data = data;
                setPreferredSize(new Dimension(700, 450));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int width = getWidth();
                int height = getHeight();
                int margin = 50;
                int barWidth = (width - 2 * margin) / data.size();

                double maxVal = data.values().stream()
                .mapToDouble(Currency::getRate)
                .max().orElse(1);

                int x = margin;
                Graphics2D g2d = (Graphics2D) g;
                g2d.setFont(new Font("Arial", Font.BOLD, 12));

                for (Currency c : data.values()) {
                    int barHeight = (int) ((c.getRate() / maxVal) * (height - 2 * margin));
                    g2d.setColor(new Color(70, 130, 180)); // สีแท่งกราฟ
                    Rectangle2D bar = new Rectangle2D.Double(x, height - margin - barHeight, barWidth - 10, barHeight);
                    g2d.fill(bar);

                    g2d.setColor(Color.BLACK);
                    g2d.draw(bar);

                    g2d.drawString(c.getCode(), x, height - margin + 15);
                    g2d.drawString(String.format("%.2f", c.getRate()), x, height - margin - barHeight - 5);

                    x += barWidth;
                }

                g2d.drawLine(margin, height - margin, width - margin, height - margin);
                g2d.drawLine(margin, margin, margin, height - margin);
                g2d.drawString("Currency", width / 2, height - 10);
                g2d.drawString("Rate", 10, height / 2);
            }
        }
    }

    // ---------- CurrencyHistoryChartFrame (กราฟย้อนหลัง) ----------
    static class CurrencyHistoryChartFrame extends JFrame {
        public CurrencyHistoryChartFrame(ExchangeHistory history, String currencyCode) {
            setTitle("Currency History Chart - " + currencyCode);
            setSize(900, 500);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            add(new CurrencyHistoryChartPanel(history, currencyCode));
        }

        static class CurrencyHistoryChartPanel extends JPanel {
            private List<HistoryRecord> filteredRecords;
            private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

            public CurrencyHistoryChartPanel(ExchangeHistory history, String currencyCode) {
                filteredRecords = new ArrayList<>();
                for (HistoryRecord r : history.getAllRecords()) {
                    if (r.getFromCurrency().equals(currencyCode) || r.getToCurrency().equals(currencyCode)) {
                        filteredRecords.add(r);
                    }
                }
                setPreferredSize(new Dimension(800, 450));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (filteredRecords.isEmpty()) {
                    g.drawString("No data for this currency", 50, 50);
                    return;
                }

                int width = getWidth();
                int height = getHeight();
                int margin = 60;

                double minRate = filteredRecords.stream()
                        .mapToDouble(r -> r.getResult())
                        .min().orElse(0);
                double maxRate = filteredRecords.stream()
                        .mapToDouble(r -> r.getResult())
                        .max().orElse(1);

                Graphics2D g2d = (Graphics2D) g;
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.setStroke(new BasicStroke(2));

                g2d.drawLine(margin, height - margin, width - margin, height - margin);
                g2d.drawLine(margin, margin, margin, height - margin);
                g2d.drawString("Time", width / 2, height - 10);
                g2d.drawString("Rate", 10, height / 2);

                int n = filteredRecords.size();
                int prevX = margin;
                int prevY = height - margin - (int)((filteredRecords.get(0).getResult() - minRate) / (maxRate - minRate) * (height - 2*margin));

                for (int i = 0; i < n; i++) {
                    HistoryRecord r = filteredRecords.get(i);
                    double value = r.getResult();
                    int x = margin + i * (width - 2*margin) / (n - 1);
                    int y = height - margin - (int)((value - minRate) / (maxRate - minRate) * (height - 2*margin));

                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(x - 4, y - 4, 8, 8);

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(String.format("%.2f", value), x - 15, y - 10);

                    if (i > 0) {
                        g2d.setColor(Color.RED);
                        g2d.drawLine(prevX, prevY, x, y);
                    }

                    prevX = x;
                    prevY = y;

                    if (i % Math.max(1, n/10) == 0) {
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(r.timestamp.format(fmt), x - 20, height - margin + 20);
                    }
                }
            }
        }
    }

    // ---------- main ----------
    public static void main(String[] args) {
        CurrencyManager manager = new CurrencyManager();
        manager.loadRatesFromFile("rates.csv");

        CurrencyConverter converter = new CurrencyConverter(manager);

        ExchangeHistory history = new ExchangeHistory();
        history.loadFromFile("history.csv");

        JFrame f = new JFrame("Exchange Money");
        f.setSize(620, 500);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);

        JPanel p = new SplitColorPanel(new Color(102, 204, 255), new Color(153, 204, 255));
        p.setBounds(0, 0, 620, 500);

        JLabel l1 = new JLabel("Currency Money");
        l1.setFont(new Font("Comic Sans MS", Font.BOLD, 25));
        l1.setBounds(300, 15, 200, 50);

        JTextField af = new JTextField();
        af.setBounds(250, 92, 150, 35);
        af.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()) && e.getKeyChar() != '.') {
                    e.consume();
                }
            }
        });

        String[] codes = manager.getAllCurrencies().stream().map(Currency::getCode).toArray(String[]::new);
        JComboBox<String> currency1 = new JComboBox<>(codes);
        currency1.setBounds(440, 93, 100, 35);

        JComboBox<String> currency2 = new JComboBox<>(codes);
        currency2.setBounds(440, 193, 100, 35);

        JButton b1 = new JButton("Convert");
        b1.setFont(new Font("Comic Sans MS", Font.BOLD, 15));
        b1.setBounds(350,250,100,35);

        JLabel rl = new JLabel("Result");
        rl.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
        rl.setBounds(292, 163, 300, 30);

        JLabel l4 = new JLabel("History");
        l4.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
        l4.setBounds(60,15,200,50);

        JButton b2 = new JButton("Show");
        b2.setFont(new Font("Comic Sans MS", Font.BOLD, 15));
        b2.setBounds(54,65,85,30);

        JLabel l5 = new JLabel("Exchange rate");
        l5.setFont(new Font("Comic Sans MS", Font.BOLD, 19));
        l5.setBounds(30,130,200,50);

        JButton b3 = new JButton("Show");
        b3.setFont(new Font("Comic Sans MS", Font.BOLD, 15));
        b3.setBounds(54,180,85,30);

        JButton b4 = new JButton("Show History Chart");
        b4.setFont(new Font("Comic Sans MS", Font.BOLD, 15));
        b4.setBounds(54, 230, 150, 30);

        p.add(l1);
        p.add(af);
        p.add(currency1);
        p.add(currency2);
        p.add(b1);
        p.add(rl);
        p.add(l4);
        p.add(b2);
        p.add(l5);
        p.add(b3);
        p.add(b4);

        f.add(p);
        f.setLayout(null);
        f.setVisible(true);

        // Event: Convert button
        b1.addActionListener(e -> {
            String from = (String) currency1.getSelectedItem();
            String to = (String) currency2.getSelectedItem();
            String amountStr = af.getText();

            if (amountStr.isEmpty()) {
                JOptionPane.showMessageDialog(f, "Please enter an amount");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(f, "Amount must be positive");
                    return;
                }

                double result = converter.convert(from, to, amount);
                rl.setText(String.format("%.2f %s = %.2f %s", amount, from, result, to));

                HistoryRecord record = new HistoryRecord(from, to, amount, result);
                history.addRecord(record);
                history.saveToFile("history.csv");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(f, "Invalid amount");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(f, ex.getMessage());
            }
        });

        // Event: Show History button
        b2.addActionListener(e -> {
            List<HistoryRecord> records = history.getAllRecords();
            if (records.isEmpty()) {
                JOptionPane.showMessageDialog(f, "No history available");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (HistoryRecord r : records) {
                sb.append(r.toString()).append("\n");
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JScrollPane scroll = new JScrollPane(ta);
            scroll.setPreferredSize(new Dimension(400, 300));
            JOptionPane.showMessageDialog(f, scroll, "Exchange History", JOptionPane.INFORMATION_MESSAGE);
        });

        // Event: Show Exchange rate Chart button
        b3.addActionListener(e -> {
            CurrencyChartFrame chartFrame = new CurrencyChartFrame(manager.currencies);
            chartFrame.setVisible(true);
        });

        // Event: Show History Chart button
        b4.addActionListener(e -> {
            String currency = (String) JOptionPane.showInputDialog(
                    f,
                    "Select currency:",
                    "Choose Currency",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    codes,
                    codes[0]
            );

            if (currency != null) {
                CurrencyHistoryChartFrame chartFrame = new CurrencyHistoryChartFrame(history, currency);
                chartFrame.setVisible(true);
            }
        });
    }
}




