package dog.del.app.metrics

object DogbinCollectors {
    fun register() {
        XodusCollector().register<XodusCollector>()
        DogbinDbMetricsCollector().register<DogbinDbMetricsCollector>()
    }
}