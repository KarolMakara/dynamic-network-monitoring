package pl.edu.agh.kt.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class NetworkMonitorWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/metrics/json", NetworkMonitorMetricsResource.class);
        router.attach("/dashboard", NetworkMonitorDashboardResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/networkmonitor";
    }
}
