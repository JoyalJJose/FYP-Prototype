import java.util.ArrayList;
import java.util.List;

public class Prediction {
    private List<Vehicle> allVehicles;
    
    public Prediction(List<Vehicle> allVehicles) {
        this.allVehicles = new ArrayList<>(allVehicles);
    }
    
    /**
     * Get all stops that come after the vehicle's current position in the route.
     * Route ends at stop D, so no wrapping around.
     */
    public List<Stop> getFutureStops(Vehicle vehicle) {
        List<Stop> futureStops = new ArrayList<>();
        List<Stop> routeStops = vehicle.getRoute().getStops();
        int currentIndex = routeStops.indexOf(vehicle.getCurrentStop());
        
        if (currentIndex >= 0) {
            // Get all stops after current stop (no wrapping)
            // Exclude the last stop from future stops (vehicles don't predict for the last stop)
            // Only include stops that come after the current position in the route
            for (int i = currentIndex + 1; i < routeStops.size() - 1; i++) {
                futureStops.add(routeStops.get(i));
            }
        }
        
        return futureStops;
    }
    
    /**
     * Get vehicles that are ahead of the given vehicle on the same route.
     * A vehicle is "ahead" if it's at a later stop index (further along the route),
     * or at the same stop but in a later stage (PRESENT/DEPARTING while current is ARRIVING).
     */
    private List<Vehicle> getVehiclesAhead(Vehicle vehicle) {
        List<Vehicle> vehiclesAhead = new ArrayList<>();
        List<Stop> routeStops = vehicle.getRoute().getStops();
        int currentIndex = routeStops.indexOf(vehicle.getCurrentStop());
        
        for (Vehicle otherVehicle : allVehicles) {
            // Only consider vehicles on the same route
            if (otherVehicle.getRoute() != vehicle.getRoute() || 
                otherVehicle == vehicle) {
                continue;
            }
            
            int otherIndex = routeStops.indexOf(otherVehicle.getCurrentStop());
            
            // Vehicle is ahead if:
            // 1. It's at a later stop index (further along the route), OR
            // 2. It's at the same stop but in PRESENT or DEPARTING state
            //    while the current vehicle is ARRIVING
            if (otherIndex >= 0 && currentIndex >= 0) {
                if (otherIndex > currentIndex) {
                    vehiclesAhead.add(otherVehicle);
                } else if (otherIndex == currentIndex) {
                    // Same stop: vehicle ahead if it's PRESENT/DEPARTING and current is ARRIVING
                    if ((otherVehicle.getState() == VehicleState.PRESENT || 
                         otherVehicle.getState() == VehicleState.DEPARTING) && 
                        vehicle.getState() == VehicleState.ARRIVING) {
                        vehiclesAhead.add(otherVehicle);
                    }
                }
            }
        }
        
        return vehiclesAhead;
    }
    
    /**
     * Calculate how many passengers a vehicle ahead will pick up at a given stop.
     * This considers:
     * 1. The vehicle's current passenger count
     * 2. Passengers it will pick up from stops between its current position and the target stop
     * 3. Its remaining capacity
     */
    private int calculatePassengersPickedUpByVehicle(Vehicle vehicle, Stop stop) {
        // First, predict how many passengers the vehicle will have when it reaches this stop
        // by accounting for passengers it will pick up from intermediate stops
        int predictedPassengerCount = vehicle.getPassengerCount();
        
        List<Stop> routeStops = vehicle.getRoute().getStops();
        int vehicleIndex = routeStops.indexOf(vehicle.getCurrentStop());
        int stopIndex = routeStops.indexOf(stop);
        
        // Add passengers from stops between vehicle's current position and target stop
        if (vehicleIndex >= 0 && stopIndex > vehicleIndex) {
            for (int i = vehicleIndex + 1; i < stopIndex; i++) {
                Stop intermediateStop = routeStops.get(i);
                // For intermediate stops, we use the original passenger count
                // (we don't recursively account for other vehicles here to avoid complexity)
                int passengersAtStop = intermediateStop.getPeopleCount();
                int remainingCapacity = vehicle.getCapacity() - predictedPassengerCount;
                int pickedUp = Math.min(passengersAtStop, remainingCapacity);
                predictedPassengerCount += pickedUp;
            }
        }
        
        // Now calculate how many passengers it can pick up at the target stop
        int passengersWaiting = stop.getPeopleCount();
        int remainingCapacity = vehicle.getCapacity() - predictedPassengerCount;
        
        // Vehicle can only pick up as many as are waiting or as much as its remaining capacity
        return Math.min(passengersWaiting, Math.max(0, remainingCapacity));
    }
    
    /**
     * Calculate how many passengers will be waiting at a stop after vehicles ahead
     * have picked up passengers.
     */
    private int getAdjustedPassengerCount(Stop stop, Vehicle currentVehicle) {
        int originalCount = stop.getPeopleCount();
        int totalPickedUp = 0;
        
        // Find all vehicles ahead that will reach this stop before the current vehicle
        List<Vehicle> vehiclesAhead = getVehiclesAhead(currentVehicle);
        List<Stop> routeStops = currentVehicle.getRoute().getStops();
        int stopIndex = routeStops.indexOf(stop);
        int currentIndex = routeStops.indexOf(currentVehicle.getCurrentStop());
        
        // Only consider vehicles ahead that will reach this stop before current vehicle
        for (Vehicle vehicleAhead : vehiclesAhead) {
            int aheadIndex = routeStops.indexOf(vehicleAhead.getCurrentStop());
            
            // Vehicle ahead will reach this stop if:
            // 1. The stop is after the vehicle ahead's current position (vehicle ahead will pass through it), OR
            // 2. The vehicle ahead is already at this stop (it will pick up passengers there first)
            if (aheadIndex >= 0) {
                if (stopIndex > aheadIndex) {
                    // Vehicle ahead will reach this stop before current vehicle
                    // Calculate how many it will pick up
                    int pickedUp = calculatePassengersPickedUpByVehicle(vehicleAhead, stop);
                    totalPickedUp += pickedUp;
                } else if (stopIndex == aheadIndex) {
                    // Vehicle ahead is already at this stop
                    // If it's PRESENT, it will pick up passengers when it departs
                    // If it's DEPARTING, the stop count already reflects passengers remaining after departure
                    // (data from sample file already encapsulates the reduction)
                    if (vehicleAhead.getState() == VehicleState.PRESENT) {
                        // Vehicle is PRESENT: will pick up passengers when it departs (next cycle 2)
                        // Calculate based on current stop count and vehicle's remaining capacity
                        int currentStopCount = stop.getPeopleCount();
                        int remainingCapacity = vehicleAhead.getCapacity() - vehicleAhead.getPassengerCount();
                        // Vehicle will pick up as many as it can (up to remaining capacity)
                        int pickedUp = Math.min(currentStopCount, Math.max(0, remainingCapacity));
                        totalPickedUp += pickedUp;
                    }
                    // Note: DEPARTING vehicles are not handled here because the stop count
                    // from the data file already reflects passengers remaining after their departure
                }
            }
        }
        
        // Return the adjusted count (original minus what vehicles ahead will pick up)
        // But don't go below 0
        return Math.max(0, originalCount - totalPickedUp);
    }
    
    /**
     * Predict the fullness of a vehicle when it reaches a target stop.
     * This accounts for:
     * 1. Current passenger count
     * 2. Passengers at current stop (if vehicle is ARRIVING or PRESENT)
     * 3. Passengers at future stops (adjusted for vehicles ahead picking them up)
     */
    public int predictFullnessAtStop(Vehicle vehicle, Stop targetStop) {
        int predictedCount = vehicle.getPassengerCount();
        
        // If vehicle is ARRIVING or PRESENT, include passengers from current stop
        // If vehicle is DEPARTING, it has already picked up passengers from current stop
        // (passenger count already includes the pickup, so don't add it again)
        if (vehicle.getState() == VehicleState.ARRIVING || vehicle.getState() == VehicleState.PRESENT) {
            Stop currentStop = vehicle.getCurrentStop();
            // Adjust for vehicles ahead that might pick up passengers at current stop
            int adjustedCount = getAdjustedPassengerCount(currentStop, vehicle);
            predictedCount += adjustedCount;
        }
        // Note: For DEPARTING vehicles, passenger count already includes pickup from current stop
        // so we don't need to add it again
        
        // Add passengers from all stops between current and target stop
        // Only consider stops that are AFTER the current position (route doesn't wrap)
        List<Stop> routeStops = vehicle.getRoute().getStops();
        int currentIndex = routeStops.indexOf(vehicle.getCurrentStop());
        int targetIndex = routeStops.indexOf(targetStop);
        
        // Only calculate for stops that are after the current position
        // Route ends at stop D, so no wrapping around
        if (currentIndex >= 0 && targetIndex > currentIndex) {
            // Add passengers from stops after current stop up to and including target stop
            for (int i = currentIndex + 1; i <= targetIndex; i++) {
                Stop stop = routeStops.get(i);
                // Use adjusted passenger count (accounting for vehicles ahead)
                int adjustedCount = getAdjustedPassengerCount(stop, vehicle);
                predictedCount += adjustedCount;
            }
        }
        // If targetIndex <= currentIndex, the stop has already been passed or is the current stop
        // Don't calculate fullness for these stops
        
        return predictedCount;
    }
}

