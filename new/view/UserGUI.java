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

        public String getCode() { return code; }
        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }
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

        public Currency getCurrency(String code) { return currencies.get(code); }
        public Collection<Currency> getAllCurrencies() { return currencies.values(); }
    }

    // ---------- CurrencyConverter ----------
    static class CurrencyConverter {
        private CurrencyManager manager;
        public CurrencyConverter(CurrencyManager manager) { this.manager = manager; }

        public double convert(String fromCode, String toCode, double amount) {
            Currency from = manager.getCurrency(fromCode);
            Currency to = manager.getCurrency(toCode);
            if (from == null || to == null) throw new IllegalArgumentException("Invalid currency code");
            double thbAmount = amount * from.getRate();
            return thbAmount / to.getRate();
        }
    }

    // ---------- HistoryRecord ----------
    static class HistoryRecord {
        private String fromCurrency, toCurrency;
        private double amount, result;
        private LocalDateTime timestamp;

        public HistoryRecord(String from, String to, double amount, double result) {
            this.fromCurrency = from; this.toCurrency = to;
            this.amount = amount; this.result = result;
            this.timestamp = LocalDateTime.now();
        }

        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "[" + timestamp.format(fmt) + "] " +
                   String.format("%.2f %s → %.2f %s", amount, fromCurrency, result, toCurrency);
        }

        public String toCSV() {
            return String.format("%s,%s,%.2f,%.2f,%s", fromCurrency, toCurrency, amount, result, timestamp.toString());
        }

        public static HistoryRecord fromCSV(String line) {
            String[] parts = line.split(",");
            if (parts.length != 5) return null;
            HistoryRecord r = new HistoryRecord(parts[0], parts[1],
                    Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
            r.timestamp = LocalDateTime.parse(parts[4]);
            return r;
        }

        public String getFromCurrency() { return fromCurrency; }
        public String getToCurrency() { return toCurrency; }
        public double getResult() { return result; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    // ---------- ExchangeHistory ----------
    static class ExchangeHistory {
        private List<HistoryRecord> records = new ArrayList<>();
        public void addRecord(HistoryRecord r) { records.add(r); }
        public List<HistoryRecord> getAllRecords() { return records; }

        public void saveToFile(String filename) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (HistoryRecord r : records) { writer.write(r.toCSV()); writer.newLine(); }
            } catch (IOException e) { System.err.println("Error saving history: " + e.getMessage()); }
        }

        public void loadFromFile(String filename) {
            records.clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    HistoryRecord r = HistoryRecord.fromCSV(line);
                    if (r != null) records.add(r);
                }
            } catch (IOException e) { System.out.println("No existing history found."); }
        }
    }

    // ---------- SplitColorPanel ----------
    static class SplitColorPanel extends JPanel {
        private Color leftColor, rightColor;
        public SplitColorPanel(Color leftColor, Color rightColor) {
            this.leftColor = leftColor; this.rightColor = rightColor;
            setLayout(null);
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            int lw = (int)(w*0.3);
            g.setColor(leftColor); g.fillRect(0,0,lw,h);
            g.setColor(rightColor); g.fillRect(lw,0,w-lw,h);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.BLACK); g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(lw,0,lw,h);
        }
    }

    // ---------- CurrencyChartFrame ----------
    static class CurrencyChartFrame extends JFrame {
        public CurrencyChartFrame(Map<String,Currency> data) {
            setTitle("Currency Exchange Rate Chart");
            setSize(900,500); setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            add(new CurrencyChartPanel(data));
        }
        static class CurrencyChartPanel extends JPanel {
            private Map<String,Currency> data;
            public CurrencyChartPanel(Map<String,Currency> data) { this.data=data; setPreferredSize(new Dimension(700,450)); }
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                int w=getWidth(), h=getHeight(), margin=50, barWidth=(w-2*margin)/data.size();
                double maxVal=data.values().stream().mapToDouble(Currency::getRate).max().orElse(1);
                int x=margin;
                Graphics2D g2d=(Graphics2D)g; g2d.setFont(new Font("Arial",Font.BOLD,12));
                for(Currency c:data.values()){
                    int barH=(int)((c.getRate()/maxVal)*(h-2*margin));
                    g2d.setColor(new Color(70,130,180));
                    g2d.fill(new Rectangle2D.Double(x,h-margin-barH,barWidth-10,barH));
                    g2d.setColor(Color.BLACK);
                    g2d.draw(new Rectangle2D.Double(x,h-margin-barH,barWidth-10,barH));
                    g2d.drawString(c.getCode(),x,h-margin+15);
                    g2d.drawString(String.format("%.2f",c.getRate()),x,h-margin-barH-5);
                    x+=barWidth;
                }
                g2d.drawLine(margin,h-margin,w-margin,h-margin);
                g2d.drawLine(margin,margin,margin,h-margin);
                g2d.drawString("Currency",w/2,h-10); g2d.drawString("Rate",10,h/2);
            }
        }
    }

    // ---------- CurrencyHistoryChartFrame ----------
    static class CurrencyHistoryChartFrame extends JFrame {
        public CurrencyHistoryChartFrame(ExchangeHistory history, String currencyCode){
            setTitle("Currency History Chart - "+currencyCode);
            setSize(900,500); setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            add(new CurrencyHistoryChartPanel(history,currencyCode));
        }
        static class CurrencyHistoryChartPanel extends JPanel {
            private List<HistoryRecord> records; private DateTimeFormatter fmt=DateTimeFormatter.ofPattern("MM-dd HH:mm");
            public CurrencyHistoryChartPanel(ExchangeHistory history, String code){
                records=new ArrayList<>();
                for(HistoryRecord r:history.getAllRecords())
                    if(r.getFromCurrency().equals(code)||r.getToCurrency().equals(code)) records.add(r);
                setPreferredSize(new Dimension(800,450));
            }
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight(), margin=60;
                if(records.isEmpty()){ g2.drawString("No data for this currency",50,50); return; }
                double min=records.stream().mapToDouble(HistoryRecord::getResult).min().orElse(0);
                double max=records.stream().mapToDouble(HistoryRecord::getResult).max().orElse(1);

                // Grid lines
                g2.setColor(new Color(220,220,220));
                int grids=5;
                for(int i=0;i<=grids;i++){
                    int y=margin+i*(h-2*margin)/grids;
                    g2.drawLine(margin,y,w-margin,y);
                    double val=max-i*(max-min)/grids;
                    g2.setColor(Color.BLACK);
                    g2.drawString(String.format("%.2f",val),5,y+5);
                    g2.setColor(new Color(220,220,220));
                }

                // Axes
                g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(2));
                g2.drawLine(margin,h-margin,w-margin,h-margin);
                g2.drawLine(margin,margin,margin,h-margin);
                g2.drawString("Time",w/2,h-10); g2.drawString("Rate",10,h/2);

                int n=records.size(), prevX=margin;
                int prevY=h-margin-(int)((records.get(0).getResult()-min)/(max-min)*(h-2*margin));
                for(int i=0;i<n;i++){
                    HistoryRecord r=records.get(i);
                    double val=r.getResult();
                    int x=margin+i*(w-2*margin)/(n-1);
                    int y=h-margin-(int)((val-min)/(max-min)*(h-2*margin));

                    if(i>0){
                        GradientPaint gp=new GradientPaint(prevX,prevY,Color.RED,x,y,Color.ORANGE,true);
                        g2.setPaint(gp); g2.setStroke(new BasicStroke(2));
                        g2.drawLine(prevX,prevY,x,y);
                    }

                    g2.setColor(new Color(30,144,255)); g2.fillOval(x-5,y-5,10,10);
                    g2.setColor(Color.BLACK); g2.drawString(String.format("%.2f",val),x-15,y-10);

                    if(i % Math.max(1,n/8)==0) g2.drawString(r.getTimestamp().format(fmt),x-25,h-margin+20);

                    prevX=x; prevY=y;
                }
            }
        }
    }

    // ---------- main ----------
    public static void main(String[] args){
        CurrencyManager manager=new CurrencyManager();
        manager.loadRatesFromFile("rates.csv");
        CurrencyConverter converter=new CurrencyConverter(manager);
        ExchangeHistory history=new ExchangeHistory();
        history.loadFromFile("history.csv");

        JFrame f=new JFrame("Exchange Money");
        f.setSize(620,430); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); f.setLocationRelativeTo(null);

        JPanel p=new SplitColorPanel(new Color(102,204,255), new Color(153,204,255));
        p.setBounds(0,0,620,430);

        JLabel l1=new JLabel("Currency Money"); l1.setFont(new Font("Comic Sans MS",Font.BOLD,25)); l1.setBounds(300,15,200,50);

        JTextField af=new JTextField(); af.setBounds(250,92,150,35);
        af.addKeyListener(new KeyAdapter(){ public void keyTyped(KeyEvent e){
            if(!Character.isDigit(e.getKeyChar())&&e.getKeyChar()!='.') e.consume(); }
        });

        String[] codes=manager.getAllCurrencies().stream().map(Currency::getCode).toArray(String[]::new);
        JComboBox<String> currency1=new JComboBox<>(codes); currency1.setBounds(440,93,100,35);
        JComboBox<String> currency2=new JComboBox<>(codes); currency2.setBounds(440,193,100,35);

        JButton b1=new JButton("Convert"); b1.setFont(new Font("Comic Sans MS",Font.BOLD,15)); b1.setBounds(350,250,100,35);
        JLabel rl=new JLabel("Result"); rl.setFont(new Font("Comic Sans MS",Font.BOLD,20)); rl.setBounds(292,163,300,30);

        JLabel l4=new JLabel("History"); l4.setFont(new Font("Comic Sans MS",Font.BOLD,20)); l4.setBounds(60,15,200,50);
        JButton b2=new JButton("Show"); b2.setFont(new Font("Comic Sans MS",Font.BOLD,15)); b2.setBounds(54,65,85,30);

        JLabel l5=new JLabel("Exchange rate"); l5.setFont(new Font("Comic Sans MS",Font.BOLD,19)); l5.setBounds(30,130,200,50);
        JButton b3=new JButton("Show"); b3.setFont(new Font("Comic Sans MS",Font.BOLD,15)); b3.setBounds(54,180,85,30);

        JLabel l6=new JLabel("History Chart"); l6.setFont(new Font("Comic Sans MS",Font.BOLD,19)); l6.setBounds(30,250,200,50);
        JButton b4=new JButton("Show"); b4.setFont(new Font("Comic Sans MS",Font.BOLD,15)); b4.setBounds(54,290,85,30);

        p.add(l1); p.add(af); p.add(currency1); p.add(currency2); p.add(b1); p.add(rl);
        p.add(l4); p.add(b2); p.add(l5); p.add(b3);
        p.add(l6); p.add(b4);

        f.add(p); f.setLayout(null); f.setVisible(true);

        b1.addActionListener(e->{
            String from=(String)currency1.getSelectedItem();
            String to=(String)currency2.getSelectedItem();
            String amountStr=af.getText();
            if(amountStr.isEmpty()){ JOptionPane.showMessageDialog(f,"Please enter an amount"); return; }
            try{
                double amount=Double.parseDouble(amountStr);
                if(amount<=0){ JOptionPane.showMessageDialog(f,"Amount must be positive"); return; }
                double result=converter.convert(from,to,amount);
                rl.setText(String.format("%.2f %s = %.2f %s",amount,from,result,to));

                HistoryRecord record=new HistoryRecord(from,to,amount,result);
                history.addRecord(record); history.saveToFile("history.csv");
            } catch(NumberFormatException ex){ JOptionPane.showMessageDialog(f,"Invalid amount");
            } catch(IllegalArgumentException ex){ JOptionPane.showMessageDialog(f,ex.getMessage()); }
        });

        b2.addActionListener(e->{
            List<HistoryRecord> records=history.getAllRecords();
            if(records.isEmpty()){ JOptionPane.showMessageDialog(f,"No history available"); return; }
            StringBuilder sb=new StringBuilder();
            for(HistoryRecord r:records) sb.append(r.toString()).append("\n");
            JTextArea ta=new JTextArea(sb.toString()); ta.setEditable(false);
            JScrollPane scroll=new JScrollPane(ta); scroll.setPreferredSize(new Dimension(400,300));
            JOptionPane.showMessageDialog(f,scroll,"Exchange History",JOptionPane.INFORMATION_MESSAGE);
        });

        b3.addActionListener(e->{
            CurrencyChartFrame chart=new CurrencyChartFrame(manager.currencies); chart.setVisible(true);
        });

        b4.addActionListener(e->{
            String code=(String)JOptionPane.showInputDialog(f,"Select currency:","Currency Selection",
                    JOptionPane.PLAIN_MESSAGE,null,codes,codes[0]);
            if(code!=null){
                CurrencyHistoryChartFrame histChart=new CurrencyHistoryChartFrame(history,code);
                histChart.setVisible(true);
            }
        });
    }
}




