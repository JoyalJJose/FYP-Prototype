import java.util.List;

public class Vehicle {
    private String id;
    private Route route;
    private int capacity;
    private Stop currentStop;
    private VehicleState state;
    private int passengerCount;
    private int passengersWhenPresent; // Track passenger count when vehicle is present at stop (end of cycle 1)

    public Vehicle(String id, Route route, int capacity, Stop currentStop) {
        this.id = id;
        this.route = route;
        this.capacity = capacity;
        if (currentStop == null) {
            this.currentStop = route.getStops().get(0);
        } else {
            this.currentStop = currentStop;
        }
        this.state = VehicleState.PRESENT; // Start as PRESENT at initial stop
        this.passengerCount = 0;
        this.passengersWhenPresent = 0;
    }

    public Stop getCurrentStop() {
        return currentStop;
    }

    public void moveToNextStop() {
        if (route == null || route.getStops().isEmpty()) {
            return;
        }

        List<Stop> stops = route.getStops();
        int currentIndex = stops.indexOf(currentStop);

        // Move to the next stop (no wrapping around)
        if (currentIndex >= 0 && currentIndex < stops.size() - 1) {
            currentIndex = currentIndex + 1;
            currentStop = stops.get(currentIndex);
            this.state = VehicleState.ARRIVING;
            this.passengersWhenPresent = 0; // Reset for new stop
        }
    }

    public void markAsPresent() {
        this.state = VehicleState.PRESENT;
    }
    
    public void markAsDeparting() {
        this.state = VehicleState.DEPARTING;
    }
    
    public int getPassengersWhenPresent() {
        return passengersWhenPresent;
    }
    
    public void setPassengersWhenPresent(int count) {
        this.passengersWhenPresent = count;
    }

    public VehicleState getState() {
        return state;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public void pickUpPassengers(int count) {
        this.passengerCount += count;
    }

    public int getCapacity() {
        return capacity;
    }

    public Route getRoute() {
        return route;
    }

    public String getId() {
        return id;
    }
}

