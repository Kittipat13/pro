package model;

import java.io.*;
import java.util.*;

public class ExchangeHistory {
    private List<HistoryRecord> records = new ArrayList<>();

    public void addRecord(HistoryRecord record) { records.add(record); }
    public List<HistoryRecord> getAllRecords() { return records; }

    public void saveToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (HistoryRecord r : records) writer.println(r.toCSV());
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException ignored) {}
    }
}