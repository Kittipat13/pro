package model;
import java.io.*;
import java.util.*;

public class CurrencyManager {
    private Map<String, Double> rates = new HashMap<>();
    private String filename;

    public CurrencyManager(String filename) {
        this.filename = filename;
        loadRatesFromFile();
    }

    // โหลดเรทจากไฟล์
    public void loadRatesFromFile() {
        rates.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String currency = parts[0].trim();
                    double rate = Double.parseDouble(parts[1].trim());
                    rates.put(currency, rate);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading rates: " + e.getMessage());
        }
    }

    // เพิ่มหรือแก้ไขเรทของสกุลเงิน
    public void setRate(String currency, double rate) {
        rates.put(currency.toUpperCase(), rate);
    }

    // บันทึกเรททั้งหมดกลับไปที่ไฟล์
    public void saveRatesToFile() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (Map.Entry<String, Double> entry : rates.entrySet()) {
                bw.write(entry.getKey() + "," + entry.getValue());
                bw.newLine();
            }
        }
    }

    // แปลงค่าเงิน
    public double convert(String fromCurrency, String toCurrency, double amount) {
        fromCurrency = fromCurrency.toUpperCase();
        toCurrency = toCurrency.toUpperCase();
        if (!rates.containsKey(fromCurrency) || !rates.containsKey(toCurrency)) {
            throw new IllegalArgumentException("Unknown currency");
        }
        double fromRate = rates.get(fromCurrency);
        double toRate = rates.get(toCurrency);
        return amount * (toRate / fromRate);
    }

    public Set<String> getCurrencies() {
        return new TreeSet<>(rates.keySet());  // Sorted set
    }

    public Double getRate(String currency) {
        return rates.get(currency.toUpperCase());
    }

    public Currency getCurrency(String fromCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrency'");
    }
}