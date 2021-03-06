package org.manathome.totask2.service;


import org.manathome.totask2.controller.ProjectPlanController;
import org.manathome.totask2.model.Project;
import org.manathome.totask2.model.Task;
import org.manathome.totask2.model.TaskAssignmentRepository;
import org.manathome.totask2.model.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

/** 
 * loading and converting {@link Project} data into suitable JSON Objects for gantt chart. 
 * 
 * @see Project
 * @see ProjectPlanData
 * @see ProjectPlanController
 * 
 * @author man-at-home 
 */
@Service
public class ProjectPlanDataService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectPlanDataService.class);    
    
    @Autowired private TaskRepository  taskRepository;   
    @Autowired private TaskAssignmentRepository taskAssignmentRepository;
    
    public ProjectPlanDataService() {
    }

    
    
    /** create plan data from project model. */
    public List<ProjectPlanData> createProjectPlanData(@NotNull final Project project)
    {
        LOG.debug("retrieving project plan data: " + project);
        
        List<Task> tasks = taskRepository.findByProjectId(project.getId());                  
         
        List<ProjectPlanData> list = tasks
                .stream()
                .map(t -> new ProjectPlanData(t))
                .collect(Collectors.toList());
        
        list.forEach(ppd -> ppd.setSeries(taskAssignmentRepository));
        return list;
    }
}
