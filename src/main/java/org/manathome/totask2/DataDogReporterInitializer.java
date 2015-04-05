package org.manathome.totask2;

import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.coursera.metrics.datadog.transport.UdpTransport;
import org.manathome.totask2.util.AAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

/*
* @startuml doc-files/totask2.metrics.datadog.png
*
* [Metric]          ..> [MetricRegistry]   
* [MetricRegistry]  ..> [DataDogReporter]   
* [DataDogReporter] ..> UDP : use
* UDP               ..> [local statsd agent DataDog]
* [DataDog Agent]   ..> HTTPS : use
* HTTPS             ..> [http://datadoghq.com]
*
* note right of [DataDogReporter]
*   sending metrics with statsd protocol 
* end note
* 
* @enduml
* 
*/    


/** 
 * configure metrics forwarding dropWizard metrics -> local dataDog "statsd" agent. 
 * 
 * on startup adding an reporter to dropWizard metrics library. Data flows via local agent to 
 * datadog metrics monitoring into the cloud.
 * 
 * <img alt="package-uml" src="doc-files/totask2.metrics.datadog.png">
 * 
 * @see    MetricRegistry  metric registry containing sent metrics
 * @see    DatadogReporter forwarding metric reporter
 * @see    <a href="http://www.datadoghq.com/">homepage of datadog.com</a>
 * 
 * @author man-at-home
 * @since  2015-04-05
 * */
@Configuration
public class DataDogReporterInitializer implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(DataDogReporterInitializer.class);

    @Autowired private MetricRegistry metricRegistry; 
    public boolean isInitialized = false;

    /** InitalizingBean, will be called on startup. */
    @Override
    public void afterPropertiesSet() throws Exception {
        initDataDogReporter();
    }
    
    /** hook dataDog reporter into dropWizard metric registry. */
    public void initDataDogReporter() {
        
        LOG.debug("DataDogReporter initializing for metrics reporting");
        
        List<String> tags = new ArrayList<String>();
        tags.add("totask2");
        
        EnumSet<Expansion> expansions = EnumSet.of(Expansion.COUNT, Expansion.RATE_1_MINUTE, Expansion.RATE_15_MINUTE, 
                                                   Expansion.MEDIAN, Expansion.P95, Expansion.P99);
        
        UdpTransport udpTransport = new UdpTransport.Builder()
//                .withStatsdHost("localhost")
//                .withPort(8125)
                .build();
        
        ScheduledReporter reporter = DatadogReporter.forRegistry(AAssert.checkNotNull(metricRegistry))
//                .withEC2Host()
                  .withTransport(udpTransport)
                  .withExpansions(expansions)
//                  .withMetricNameFormatter(ShortenedNameFormatter)
                  .build();

         reporter.start(10, TimeUnit.SECONDS);      
         this.isInitialized = true;
         
         metricRegistry.counter("TOTASK2XX.metricRegistry.reportingInitialized.count").inc();
         LOG.info("DataDogReporter started local reporting for metrics");
         return;
    }
    
}
