import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheduler {
    // ANSI color codes for terminal output
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String CYAN = "\033[36m";
    private static final String YELLOW = "\033[33m";
    private static final String GREEN = "\033[32m";
    private static final String RED = "\033[31m";
    private static final String BLUE = "\033[34m";
    private static final String MAGENTA = "\033[35m";
    
    private List<Route> routes;
    private List<Vehicle> vehicles; // Active vehicles on the route
    private List<Vehicle> scheduledVehicles; // Vehicles scheduled for deployment but not yet active
    private int removalState = 0; // Tracks removal state: 0 = normal, -1 = removal happened when no scheduled vehicles
    private DataReader dataReader;
    private Prediction prediction; // Prediction engine for vehicle fullness
    private int cycleCount = 0; // Track cycle count for 3-stage progression
    private static final double REMOVAL_THRESHOLD = 0.2; // 20% below average capacity triggers removal
    int nextVehicleId = 1; // Package-private for initialization in main
    
    // Helper methods for colored output
    private String colorize(String text, String color) {
        return color + text + RESET;
    }
    
    private String bold(String text) {
        return BOLD + text + RESET;
    }
    
    private String header(String text) {
        return bold(colorize(text, CYAN));
    }
    
    private String info(String text) {
        return colorize(text, BLUE);
    }
    
    private String success(String text) {
        return colorize(text, GREEN);
    }
    
    private String warning(String text) {
        return colorize(text, YELLOW);
    }
    
    private String error(String text) {
        return colorize(text, RED);
    }
    
    private String highlight(String text) {
        return colorize(text, MAGENTA);
    }

    public Scheduler(String dataFilePath) {
        this.routes = new ArrayList<>();
        this.vehicles = new ArrayList<>();
        this.scheduledVehicles = new ArrayList<>();
        this.removalState = 0;
        this.dataReader = new DataReader(dataFilePath);
        this.prediction = new Prediction(vehicles);
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
        updatePrediction();
    }
    
    private void updatePrediction() {
        prediction = new Prediction(vehicles);
    }

    /**
     * Check if a stop is the last stop in its route.
     */
    private boolean isLastStop(Stop stop, Route route) {
        List<Stop> stops = route.getStops();
        return !stops.isEmpty() && stops.get(stops.size() - 1) == stop;
    }

    public void updateStopCounts(Map<Integer, Integer> stopCounts) {
        // Update stop counts in all routes from the data file
        // Skip updates for the last stop (passengers only get off, no counting)
        for (Route route : routes) {
            for (Stop stop : route.getStops()) {
                if (!isLastStop(stop, route) && stopCounts.containsKey(stop.getId())) {
                    stop.updatePeopleCount(stopCounts.get(stop.getId()));
                }
            }
        }
        
        // Process vehicles based on 3-stage cycle
        // Note: cycleStage is passed from processDataCycle to ensure correct timing
        int cycleStage = cycleCount % 3;
        
        // Use a list to track vehicles to remove (to avoid concurrent modification)
        List<Vehicle> vehiclesToRemove = new ArrayList<>();
        
        for (Vehicle vehicle : vehicles) {
            Stop currentStop = vehicle.getCurrentStop();
            Route vehicleRoute = vehicle.getRoute();
            boolean atLastStop = isLastStop(currentStop, vehicleRoute);
            int currentStopCount = stopCounts.getOrDefault(currentStop.getId(), 0);
            
            if (cycleStage == 0) {
                // Stage 0: DEPARTING → ARRIVING (vehicle moves to next stop and becomes arriving)
                // OR initialize vehicles starting at stops
                if (vehicle.getState() == VehicleState.DEPARTING) {
                    if (atLastStop) {
                        // Vehicle is DEPARTING from last stop: remove it from the route
                        vehiclesToRemove.add(vehicle);
                    } else {
                        // Vehicle was departing, now move to next stop and become ARRIVING
                        // (stays in ARRIVING for this full cycle 0, will become PRESENT in cycle 1)
                        vehicle.moveToNextStop();
                    }
                } else if (vehicle.getState() == VehicleState.PRESENT && vehicle.getPassengersWhenPresent() == 0) {
                    // Initialize passengersWhenPresent for vehicles starting at stops
                    // Skip for last stop (no passenger counting at last stop)
                    if (!atLastStop) {
                        vehicle.setPassengersWhenPresent(currentStopCount);
                    }
                }
            } else if (cycleStage == 1) {
                // Stage 1: ARRIVING → PRESENT (vehicle becomes present after arriving)
                // OR PRESENT (vehicle stays at stop)
                if (vehicle.getState() == VehicleState.ARRIVING) {
                    // Vehicle was arriving, now becomes present
                    if (atLastStop) {
                        // At last stop: empty all passengers when becoming PRESENT
                        vehicle.setPassengerCount(0);
                        vehicle.markAsPresent();
                    } else {
                        vehicle.setPassengersWhenPresent(currentStopCount);
                        vehicle.markAsPresent();
                    }
                } else if (vehicle.getState() == VehicleState.PRESENT) {
                    // Vehicle is PRESENT: update passengersWhenPresent at END of cycle 1
                    // This captures accumulated passengers that will be available when vehicle departs
                    // Skip for last stop (no passenger counting at last stop)
                    if (!atLastStop) {
                        vehicle.setPassengersWhenPresent(currentStopCount);
                    }
                }
            } else if (cycleStage == 2) {
                // Stage 2: PRESENT → DEPARTING
                // Vehicle departs, picks up passengers (stays in DEPARTING for full cycle)
                if (vehicle.getState() == VehicleState.PRESENT) {
                    vehicle.markAsDeparting();
                    
                    if (!atLastStop) {
                        // Normal stop: calculate passengers picked up
                        // Calculate passengers picked up = passengers when present - passengers remaining after departure
                        // passengersWhenPresent was set at END of cycle 1 (after accumulation)
                        int passengersWhenPresent = vehicle.getPassengersWhenPresent();
                        int passengersRemainingAfterDeparture = currentStopCount;
                        int passengersPickedUp = Math.max(0, passengersWhenPresent - passengersRemainingAfterDeparture);
                        
                        // Respect vehicle capacity
                        int remainingCapacity = vehicle.getCapacity() - vehicle.getPassengerCount();
                        int actualPickup = Math.min(passengersPickedUp, remainingCapacity);
                        
                        if (actualPickup > 0) {
                            vehicle.pickUpPassengers(actualPickup);
                        }
                        
                        // Update stop count (remaining passengers after vehicle departure)
                        currentStop.updatePeopleCount(passengersRemainingAfterDeparture);
                    }
                    // At last stop: skip pickup logic (passengers only get off, no pickup)
                    // Vehicle stays in DEPARTING state for full cycle 2
                    // Will be removed in next cycle 0
                }
            }
        }
        
        // Remove vehicles that departed from last stop
        for (Vehicle vehicle : vehiclesToRemove) {
            vehicles.remove(vehicle);
            updatePrediction();
            System.out.println("\n" + success("[OK] [VEHICLE REMOVED]") + " Vehicle " + highlight(vehicle.getId()) + 
                             " has completed the route and been removed.");
        }
    }



    private boolean checkIfDeploymentNeeded() {
        updatePrediction();
        for (Vehicle vehicle : vehicles) {
            for (Stop futureStop : prediction.getFutureStops(vehicle)) {
                int predictedFullness = prediction.predictFullnessAtStop(vehicle, futureStop);
                int threshold = (int) (vehicle.getCapacity() * vehicle.getRoute().getDeploymentThreshold());
                if (predictedFullness > threshold) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printPredictedFullness() {
        updatePrediction();
        System.out.println("  " + header("Predicted Fullness:"));
        for (Vehicle vehicle : vehicles) {
            Stop currentStop = vehicle.getCurrentStop();
            Route vehicleRoute = vehicle.getRoute();
            boolean atLastStop = isLastStop(currentStop, vehicleRoute);
            
            // Color code vehicle state
            String stateColor = "";
            String stateText = vehicle.getState().toString();
            switch (vehicle.getState()) {
                case ARRIVING:
                    stateColor = YELLOW;
                    break;
                case PRESENT:
                    stateColor = GREEN;
                    break;
                case DEPARTING:
                    stateColor = MAGENTA;
                    break;
            }
            
            System.out.println("    " + highlight("Vehicle " + vehicle.getId()) + 
                             " (" + info("Capacity: " + vehicle.getCapacity()) + 
                             ", " + info("Current Passengers: " + vehicle.getPassengerCount()) + 
                             ", " + colorize("State: " + stateText, stateColor) + 
                             ", " + info("Current Stop: " + currentStop.getName()) + "):");
            
            List<Stop> futureStops = prediction.getFutureStops(vehicle);
            
            if (atLastStop && vehicle.getState() == VehicleState.PRESENT) {
                // Vehicle is at the final stop and present
                System.out.println("      " + warning("[*] At final stop - passengers will be unloaded here"));
            } else if (atLastStop && vehicle.getState() == VehicleState.DEPARTING) {
                // Vehicle is departing from the final stop
                System.out.println("      " + warning("[*] Route completed..Heading back to depot..will be removed from route"));
            } else if (futureStops.isEmpty()) {
                // No future stops means only final stop remains
                Stop lastStop = vehicleRoute.getStops().get(vehicleRoute.getStops().size() - 1);
                System.out.println("      " + info("Final stop remaining: " + lastStop.getName() + " (no predictions for final stop)"));
            } else {
                for (Stop futureStop : futureStops) {
                    int predictedFullness = prediction.predictFullnessAtStop(vehicle, futureStop);
                    int threshold = (int) (vehicle.getCapacity() * vehicle.getRoute().getDeploymentThreshold());
                    boolean exceedsThreshold = predictedFullness > threshold;
                    String status = exceedsThreshold ? " " + error("[EXCEEDS THRESHOLD!]") : "";
                    String fullnessColor = exceedsThreshold ? RED : (predictedFullness > vehicle.getCapacity() * 0.8 ? YELLOW : GREEN);
                    System.out.println("      " + futureStop.getName() + ": " + 
                                     colorize(predictedFullness + " / " + vehicle.getCapacity(), fullnessColor) + 
                                     " (" + info("Threshold: " + threshold) + ")" + status);
                }
            }
        }
    }

    private void deployNewVehicle(Route route) {
        // Create and schedule a new vehicle
        String vehicleId = "V" + nextVehicleId++;
        Stop startingStop = route.getStops().get(0);
        Vehicle newVehicle = new Vehicle(vehicleId, route, 50, startingStop); // Default capacity 50
        scheduledVehicles.add(newVehicle);
        removalState = 0; // Reset removal state when new vehicle is deployed
        System.out.println("\n" + success("[DEPLOYMENT]") + " New vehicle " + highlight(vehicleId) + 
                          " scheduled for deployment to " + info(route.getRouteName()) + 
                          " starting at " + info(startingStop.getName()));
    }

    private void removeScheduledVehicle() {
        if (scheduledVehicles.isEmpty()) {
            // No scheduled vehicles, set removal state to -1 to prevent further removals
            removalState = -1;
            System.out.println("\n" + warning("[!] [REMOVAL]") + " Removal requested but no scheduled vehicles available. Removal state set to prevent further removals.");
        } else {
            Vehicle removed = scheduledVehicles.remove(0);
            removalState = 0; // Reset removal state when vehicle is successfully removed
            System.out.println("\n" + warning("[!] [REMOVAL]") + " Scheduled vehicle " + highlight(removed.getId()) + 
                            " removed from deployment schedule for " + info(removed.getRoute().getRouteName()));
        }
    }

    private boolean checkIfRemovalNeeded() {
        // Check if removal already happened when no scheduled vehicles (prevent further removals)
        if (removalState == -1) {
            return false;
        }
        
        int totalPassengers = 0;
        for (Route route : routes) {
            for (Stop stop : route.getStops()) {
                totalPassengers += stop.getPeopleCount();
            }
        }
        
        // Calculate average capacity of active vehicles
        int totalCapacity = 0;
        for (Vehicle vehicle : vehicles) {
            totalCapacity += vehicle.getCapacity();
        }
        
        if (totalCapacity == 0) {
            return false;
        }
        
        // If total passengers are significantly below average capacity, removal is needed
        double utilizationRatio = (double) totalPassengers / totalCapacity;
        return utilizationRatio < REMOVAL_THRESHOLD;
    }

    private void processDataCycle(Map<Integer, Integer> stopCounts, Map<Integer, Integer> previousCounts) {
        updateStopCounts(stopCounts);
        cycleCount++; // Increment cycle count for 3-stage progression AFTER processing
        
        // Check for deployment needs
        if (checkIfDeploymentNeeded() && scheduledVehicles.isEmpty() && !routes.isEmpty()) {
            deployNewVehicle(routes.get(0));
        }
        
        // Check for removal needs (if counts decreased significantly)
        if (previousCounts != null && checkIfRemovalNeeded() && hasSignificantDecrease(stopCounts, previousCounts)) {
            removeScheduledVehicle();
        }
    }
    
    private boolean hasSignificantDecrease(Map<Integer, Integer> current, Map<Integer, Integer> previous) {
        for (Map.Entry<Integer, Integer> entry : current.entrySet()) {
            int previousCount = previous.getOrDefault(entry.getKey(), 0);
            if (previousCount > 0 && entry.getValue() < previousCount * 0.5) {
                return true;
            }
        }
        return false;
    }


    public List<Route> getRoutes() {
        return routes;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Vehicle> getScheduledVehicles() {
        return scheduledVehicles;
    }

    public void startScheduling() {
        List<DataReader.DataEntry> data = dataReader.readData();

        // Since our sample data is already grouped by timestamp and chronologically sorted,
        // we can simply iterate in order and process whenever the timestamp changes.

        Map<Integer, Integer> stopCounts = new HashMap<>();
        Map<Integer, Integer> previousCounts = null;
        String previousTimestamp = null;

        System.out.println(bold(colorize("===============================================================", CYAN)));
        System.out.println(bold(colorize("           Starting Scheduling System", CYAN)));
        System.out.println(bold(colorize("===============================================================", CYAN)) + "\n");

        int processedCycles = 0;

        for (DataReader.DataEntry entry : data) {
            String timestamp = entry.getTimestamp();

            // On new timestamp boundary, process previous cycle if any
            if (previousTimestamp != null && !previousTimestamp.equals(timestamp)) {
                System.out.println(bold(colorize("-------------------------------------------------------------------", CYAN)));
                System.out.println(header("  Processing timestamp: " + previousTimestamp));
                System.out.println(bold(colorize("-------------------------------------------------------------------", CYAN)));
                System.out.println("  " + info("Stop counts: ") + stopCounts);
                System.out.println();

                processDataCycle(stopCounts, previousCounts);
                printPredictedFullness();

                previousCounts = new HashMap<>(stopCounts);
                stopCounts.clear();

                System.out.println();
                System.out.println("  " + info("Active vehicles: ") + highlight(String.valueOf(vehicles.size())));
                System.out.println("  " + info("Scheduled vehicles: ") + highlight(String.valueOf(scheduledVehicles.size())));
                System.out.println();
                processedCycles++;
            }

            // Collect stop crowd counts for current timestamp
            stopCounts.put(entry.getStopId(), entry.getCrowdCount());
            previousTimestamp = timestamp;
        }

        // Process last group of entries if any left
        if (!stopCounts.isEmpty() && previousTimestamp != null) {
            System.out.println(bold(colorize("-------------------------------------------------------------------", CYAN)));
            System.out.println(header("  Processing timestamp: " + previousTimestamp));
            System.out.println(bold(colorize("-------------------------------------------------------------------", CYAN)));
            System.out.println("  " + info("Stop counts: ") + stopCounts);
            System.out.println();

            processDataCycle(stopCounts, previousCounts);
            printPredictedFullness();

            System.out.println();
            System.out.println("  " + info("Active vehicles: ") + highlight(String.valueOf(vehicles.size())));
            System.out.println("  " + info("Scheduled vehicles: ") + highlight(String.valueOf(scheduledVehicles.size())));
            System.out.println();
            processedCycles++;
        }

        System.out.println(bold(colorize("===============================================================", CYAN)));
        System.out.println(success("  [OK] Processed " + processedCycles + " time cycles"));
        System.out.println(bold(colorize("===============================================================", CYAN)));
        System.out.println(bold(colorize("           Scheduling Complete", GREEN)) + "\n");
    }

    public static void main(String[] args) {
        // Determine which data file to use based on command line argument
        String dataFile = "dataIncrease.txt";
        if (args.length > 0) {
            dataFile = args[0];
        }
        
        // Initialize system
        Scheduler scheduler = new Scheduler(dataFile);
        
        // Create route with 5 stops
        Route route = new Route("R1", "Route 1");
        Stop stop1 = new Stop(1, "Stop A");
        Stop stop2 = new Stop(2, "Stop B");
        Stop stop3 = new Stop(3, "Stop C");
        Stop stop4 = new Stop(4, "Stop D");
        Stop stop5 = new Stop(5, "Stop E");
        
        route.addStop(stop1);
        route.addStop(stop2);
        route.addStop(stop3);
        route.addStop(stop4);
        route.addStop(stop5);
        
        // Add route to scheduler
        scheduler.getRoutes().add(route);
        
        // Create initial vehicles and attach to route
        // Vehicles start in PRESENT state at their initial stops
        Vehicle vehicle1 = new Vehicle("V1", route, 60, stop1);
        Vehicle vehicle2 = new Vehicle("V2", route, 50, stop2);
        
        // V2 is at Stop B (second stop), so it should have already picked up passengers from Stop A
        // Initialize V2 with passengers from Stop A (assuming it picked up most passengers)
        // Based on initial data: Stop A has 12 people, so V2 likely picked up ~10-12 passengers
        vehicle2.pickUpPassengers(10); // V2 already has passengers from Stop A
        
        // Initialize passengersWhenPresent for vehicles starting at stops
        // V1 at Stop A: will track when cycle starts
        // V2 at Stop B: should track the current count at Stop B (8 from initial data)
        vehicle2.setPassengersWhenPresent(8);
        
        // Add vehicles to scheduler system
        scheduler.addVehicle(vehicle1);
        scheduler.addVehicle(vehicle2);
        
        // Initialize next vehicle ID to 3 since we already have V1 and V2
        scheduler.nextVehicleId = 3;

        // Start scheduling system - processes data chronologically
        scheduler.startScheduling();
    }
}

