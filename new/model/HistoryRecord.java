package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistoryRecord {
    public String fromCurrency;
    private String toCurrency;
    private double amount;
    private double result;
    public LocalDateTime timestamp;

    public HistoryRecord(String fromCurrency, String toCurrency, double amount, double result) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
        this.result = result;
        this.timestamp = LocalDateTime.now();
    }

    public String toCSV() {
        return String.format("%s,%s,%.2f,%.2f,%s", fromCurrency, toCurrency, amount, result, timestamp);
    }

    public static HistoryRecord fromCSV(String line) {
        String[] parts = line.split(",");
        if (parts.length != 5) return null;
        HistoryRecord hr = new HistoryRecord(parts[0], parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        hr.timestamp = LocalDateTime.parse(parts[4]);
        return hr;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] %.2f %s → %.2f %s", timestamp.format(fmt), amount, fromCurrency, result, toCurrency);
    }
}