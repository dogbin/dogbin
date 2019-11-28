package dog.del.app.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram

class DogbinMetrics {
    val activeRequests = Gauge.build("http_requests_active", "Active requests").register()
    val requestDuration = Gauge.build("http_requests_duration", "Request duration").register()
    val exceptions = Counter.build("http_requests_exceptions", "Number of exceptions.").labelNames("class").register()
}