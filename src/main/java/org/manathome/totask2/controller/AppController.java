package org.manathome.totask2.controller;

import org.manathome.totask2.util.Totask2Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


/** 
 * main menu. 
 * 
 * @see ProjectController
 * @see WeekEntryController
 * 
 * */
@Controller
public class AppController {

    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);
    
    @Autowired
    private Totask2Environment environment;
 
    /** landing page /. */
    @RequestMapping("/")
    public String index(Model model) {        
        
        LOG.trace("index" + this.environment);
        
        model.addAttribute("name", this.environment.getName());
        model.addAttribute("description", this.environment.getDescription());
        model.addAttribute("version", this.environment.getVersion());
        
        
        return "index";
    }
    
    /** not allowed (spring security / http 403). */
    @RequestMapping("/403")
    public String notAllowed() {        
        LOG.trace("not allowed page");
        return "403";
    }
    
}
