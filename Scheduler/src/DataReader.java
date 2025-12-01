import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataReader {
    private String filePath;

    public DataReader(String filePath) {
        this.filePath = filePath;
    }

    public List<DataEntry> readData() {
        List<DataEntry> data = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Format: stop id, crowdCount, timestamp, state
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    try {
                        int stopId = Integer.parseInt(parts[0].trim());
                        int crowdCount = Integer.parseInt(parts[1].trim());
                        String timestamp = parts[2].trim();
                        String state = parts[3].trim();
                        data.add(new DataEntry(stopId, crowdCount, timestamp, state));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid data format in line: " + line);
                    }
                } else {
                    System.err.println("Invalid line format (expected 4 comma-separated values): " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading data file: " + e.getMessage());
        }
        
        return data;
    }

    // Inner class for data entries - will be replaced with database in future
    public static class DataEntry {
        private int stopId;
        private int crowdCount;
        private String timestamp;
        private String state;

        public DataEntry(int stopId, int crowdCount, String timestamp, String state) {
            this.stopId = stopId;
            this.crowdCount = crowdCount;
            this.timestamp = timestamp;
            this.state = state;
        }

        public int getStopId() {
            return stopId;
        }

        public int getCrowdCount() {
            return crowdCount;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getState() {
            return state;
        }
    }
}
