package org.manathome.totask2.service;


import static org.manathome.totask2.util.AAssert.checkNotNull;
import static org.manathome.totask2.util.AAssert.checkNotNullOrEmpty;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.manathome.totask2.controller.ProjectController;
import org.manathome.totask2.controller.WeekEntryController;
import org.manathome.totask2.util.AAssert;
import org.manathome.totask2.util.EnvironmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.AbstractJasperReportsSingleFormatView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsXlsView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.validation.constraints.NotNull;


/**
 * report generation with jasper report.
 * 
 * @see <a href="http://de.wikipedia.org/wiki/JasperReports">http://de.wikipedia.org/wiki/JasperReports</a>
 * @see WeekEntryController#getWeekEntryReport(String, String)
 * @see ProjectController#getProjectsReport(String)
 * 
 * 
 * @author man-at-home
 * */
@Service
public class ReportGenerator {

    
    /** output format of report. */
    public static enum ReportOutputFormat {
        excel,
        pdf
    }
    

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    
    @Autowired private ApplicationContext   appContext;   
    @Autowired private CounterService       counterService;

    /** render. 
     * @exception EnvironmentException 
     */ 
    public void render(@NotNull final String reportTemplateName, final ReportOutputFormat outputFormat, final Collection<?> pojoDataSource, OutputStream outputStream) throws EnvironmentException {
        try {
            String reportName = "/reports/" + AAssert.checkNotNull(reportTemplateName, "report template required");
            
            HashMap<String, Object> reportParams = new HashMap<String, Object>();
            reportParams.put("reportTitle", "totask2 User Report");

            // 2. Retrieve template
            LOG.trace("loading report..: " + this.getClass().getResource(reportName));
            InputStream reportStream = this.getClass().getResourceAsStream(reportName);

            // 3. Convert template to JasperDesign
            JasperDesign jd = JRXmlLoader.load(reportStream);
            LOG.debug("compiling report: " + jd.getName());

            // 4. Compile design to JasperReport
            JasperReport jr = JasperCompileManager.compileReport(jd);
            LOG.debug("filling report..: " + jr.getName() + " with " + pojoDataSource.size() + " rows.");    
            
            // 5. Create the JasperPrint object
            // Make sure to pass the JasperReport, report parameters, and data
            // source
            JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, buildDataSource(pojoDataSource));
            
            LOG.debug("exporting report.: " + jp.getName());    
            
            switch(outputFormat) {
               case excel:
                   JRXlsExporter exporter = new JRXlsExporter();
                   exporter.setExporterInput(new SimpleExporterInput(jp));
                   exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));                  
                   exporter.exportReport();
                   if (counterService != null) {
                       counterService.increment("TOTASK2XX.service.reporting.excel.created");
                   }
                   break;
               case pdf:
                   JasperExportManager.exportReportToPdfStream(jp, outputStream);
                   if (counterService != null) {
                       counterService.increment("TOTASK2XX.service.reporting.pdf.created");
                   }
                   break;                 
               default:
                   throw new IllegalArgumentException("not a valid report output format: " + outputFormat);
            }
            
 
        } catch (JRException jrex) {
            LOG.error("Error render Report", jrex);
            throw new EnvironmentException("Error generation Report: " + jrex.getMessage(), jrex);
        }
    }

    /**
     * Returns a data source that's wrapped within {@link JRDataSource}.
     * 
     * @return
     */
    private JRDataSource buildDataSource(Collection<?> data) {
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data, true);
        LOG.debug("  DataSource[" + ds.getRecordCount() + "]");
        return ds;
    }
    
    /** creates a spring modelview with jasperreport report. 
     * 
     * @param reportName jasperReports template filename (example: reportGeneratorTestReport.jrxml)
     * @param reportData pojo collection as datasource generating the report.
     * */
    public ModelAndView createReportModelView(@NotNull final String reportName, final ReportOutputFormat reportOutputFormat, @NotNull final Collection<?> reportData)
    {
        checkNotNullOrEmpty(reportName, "reportName");
        checkNotNull(reportData, "reportData");
        
        AbstractJasperReportsSingleFormatView view = reportOutputFormat == ReportOutputFormat.excel ? 
                    new JasperReportsXlsView() : 
                    new JasperReportsPdfView();
        
        view.setUrl("classpath:reports/" + reportName);
        view.setApplicationContext(appContext);
        view.setReportDataKey("jasperReportsDataKey");
        
        if (reportOutputFormat == ReportOutputFormat.excel) {
            view.setContentType("application/vnd.ms-excel");
            
            Properties headers = new Properties();
            headers.setProperty("Content-Disposition", "attachment; filename=" + reportName + ".xls");
            view.setHeaders(headers);
        } else {
            view.setContentType("application/pdf");
            Properties headers = new Properties();
            headers.setProperty("Content-Disposition", "attachment; filename=" + reportName + ".pdf");
            view.setHeaders(headers);
        }
       
        
        Map<String, Object> params = new HashMap<>();
        params.put("jasperReportsDataKey", reportData);
        
        if (this.counterService != null) { 
            counterService.increment("TOTASK2XX.service.report.invoked");
        }
    
        LOG.debug("rendering " + reportOutputFormat + ":" + view.getUrl() + " with " + reportData.size() + " rows of data");
    
        return new ModelAndView(view, params);
    }
}
