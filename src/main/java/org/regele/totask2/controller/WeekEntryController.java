package org.regele.totask2.controller;


import org.regele.totask2.model.TaskInWeek;
import org.regele.totask2.model.User;
import org.regele.totask2.model.UserRepository;
import org.regele.totask2.model.WorkEntryRepository;
import org.regele.totask2.service.ReportGenerator;
import org.regele.totask2.service.ReportGenerator.ReportOutputFormat;
import org.regele.totask2.service.WeekEntryService;
import org.regele.totask2.util.ApplicationAssert;
import org.regele.totask2.util.DurationConverter;
import org.regele.totask2.util.InvalidClientArgumentsException;
import org.regele.totask2.util.LocalDateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


/** main entry page for uses.
 * - display exactly one week of work
 * - all tasks, one entry per task an day.
 * 
 * @author Manfred
 * @since  2014-08-09
 */
@Controller
public class WeekEntryController {

    private static final Logger LOG = LoggerFactory.getLogger(WeekEntryController.class);

    /** work entry repository. */
    @Autowired private WorkEntryRepository workEntryRepository;
    
    /** user. */
    @Autowired private UserRepository userRepository;
    
    /** reporting code for excel export. */
    @Autowired private ReportGenerator reportGenerator;
    
    private WeekEntryService getService() {
        return new WeekEntryService(this.workEntryRepository);
    }
    
    private List<TaskInWeek> getWeek(final LocalDate dt) {
        User      user = userRepository.getOne(2L /* TestConstants.TestUser */);   // TBF current User.        
        List<TaskInWeek> tasksInWeek = getService().getWorkWeek(user, dt);      
        return tasksInWeek;
     }    

    
    /** add view data to model for a given work week. */
    private void buildWeekModel(final Model model, final LocalDate dt) {
        model.addAttribute("currentWeek",   dt.toString());
        model.addAttribute("previousWeek",  dt.minusWeeks(1).toString());
        model.addAttribute("nextWeek",      dt.plusWeeks(1).toString());
        model.addAttribute("date" ,         LocalDateConverter.toDate(dt));
        model.addAttribute("tasksInWeek",   getWeek(dt));        
        
        LOG.debug("model has weekEntry for " + dt);        
    }


    /** GET: provide workEntry data of current week. */
    @RequestMapping(value = "/weekEntry" , method = RequestMethod.GET)
    public String weekEntry(final Model model) {
        LOG.debug("weekEntry(default)");
        
        LocalDate dt   =  LocalDate.now();
        return weekEntry(model, dt.toString());
    }
    
    
    /** GET: provide all workEntry data for given week, enable editing on html page. */
    @RequestMapping(value = "/weekEntry/{dateString}", method = RequestMethod.GET)
    public String weekEntry(final Model model, @PathVariable final String dateString) {        
        LOG.debug("weekEntry( " + dateString + ")");
        
        buildWeekModel(model, LocalDate.parse(dateString));
        return "weekEntry";
    }
    
    
    /** POST: save all workEntry data for given week. */
    @RequestMapping(value = "/weekEntry/{dateString}", method = RequestMethod.POST)
    public String saveWeekEntry(
            final Model model, 
            @PathVariable final String dateString, 
            final RedirectAttributes redirectAttributes,
            final WebRequest request
            ) {        
        LOG.debug("saveWeekEntry( " + dateString + ") -> db");
       
        ApplicationAssert.assertNotNullOrEmpty("no date given", dateString);
        LocalDate dt   =  LocalDate.parse(dateString);
        
        List<TaskInWeek> tasksInWeek = getWeek(dt);
        
        // each task row
        for(TaskInWeek taskRow : tasksInWeek) {
            for(int i = 0; i <= 6; i++) {
                // each of 7 days for current task)
                String newDurationString = request.getParameter("workEntry_" + taskRow.getTask().getId() + "_" + i);
               
                try{
                    float newDuration = DurationConverter.parse(newDurationString).floatValue();
                    if( newDuration != taskRow.getDailyEntries()[i].getDuration())
                    {
                        LOG.debug( taskRow.getTask().getName() + "[" + i + "]: " + newDurationString + "h == " + newDuration + ", old value was:" + taskRow.getDailyEntries()[i] );
                        taskRow.getDailyEntries()[i].setDuration(DurationConverter.parse(newDurationString).floatValue());
                    }
                }catch(ParseException pex) {
                    // rely on client side validation for "pretty" user feedback..
                    throw new InvalidClientArgumentsException("invalid duration " + newDurationString + " for task " + taskRow.getTask().getName(), pex);
                }
            }
        }
        
        String changedTaskNames =tasksInWeek
            .stream()
            .filter(t -> t.isModifiedByUser())
            .map(tiw -> tiw.getTask().getName())
            .collect(Collectors.joining(", "));
            ;
        
        LOG.debug("updating: " + changedTaskNames);
        
        getService().saveWeek(tasksInWeek);
        

        String updateMessage = changedTaskNames != null && changedTaskNames.length() > 1 ? "changed values for: " + changedTaskNames : "no change.";
        redirectAttributes.addFlashAttribute("flashMessage", updateMessage);

        return "redirect:/weekEntry";
    }   
    
    
        
    /**  generates an excel report containing given weeks work data. */
    @RequestMapping(value = "/weekEntry/report/{reportFormat}/{dateString}", method = RequestMethod.GET, produces = "application/vnd.ms-excel")
    public ModelAndView getWeekEntryReport(@PathVariable final String reportFormat, @PathVariable final String dateString) {
        
        LOG.debug("getProjectsReport(" + reportFormat + ", " +  dateString + ")"); 
        LocalDate dt   =  LocalDate.parse(dateString);         
         
        return reportGenerator
                .createReportModelView(
                        "weekEntryReport.jrxml" , 
                        reportFormat == null || reportFormat.contains("pdf") ? ReportOutputFormat.pdf : ReportOutputFormat.excel,
                        getWeek(dt)
                        );
    }
        
}//class