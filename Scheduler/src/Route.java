import java.util.ArrayList;
import java.util.List;

public class Route {
    private String routeId;
    private String routeName;
    private List<Stop> stops;
    private double deploymentThreshold; // Threshold multiplier for deployment (default 1.2 = 120% of capacity)

    public Route(String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.stops = new ArrayList<>();
        this.deploymentThreshold = 1.2; // Default: 20% above capacity triggers deployment
    }

    public void addStop(Stop stop) {
        stops.add(stop);
    }

    public List<Stop> getStops() {
        return stops;
    }

    public String getRouteName() {
        return routeName;
    }

    public double getDeploymentThreshold() {
        return deploymentThreshold;
    }

}

