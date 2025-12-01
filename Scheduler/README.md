# Scheduler

Vehicle deployment scheduling system that monitors passenger demand and automatically deploys or removes vehicles based on predicted capacity thresholds.

## Contents

- `src/` - Java source files
- `dataIncrease.txt` - Sample data file with increasing passenger demand
- `dataDecrease.txt` - Sample data file with decreasing passenger demand

## How to Run

1. **Compile the Java files**:
   ```bash
   javac -d . src/*.java
   ```
   
   **Note**: If you're using Docker, the Java files are already compiled in the Docker image.

2. **Run the scheduler**:
   ```bash
   java Scheduler
   ```
   This uses dataIncrease.txt data by default
   
   Or specify a data file:
   ```bash
   java Scheduler dataIncrease.txt
   java Scheduler dataDecrease.txt
   ```

## Data File Format

The data files contain crowd count data in the format:
```
stopId,crowdCount,timestamp,state
```

The scheduler processes this data chronologically in batches and makes vehicle deployment decisions based on predicted capacity at future stops.

