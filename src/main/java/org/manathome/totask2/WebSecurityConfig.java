package org.manathome.totask2;

import org.manathome.totask2.model.User;
import org.manathome.totask2.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;


/**
 * security configuration for the totask2 web application.
 * 
 * @see UserDetailsServiceImpl
 * @see <a href="http://projects.spring.io/spring-security/">http://projects.spring.io/spring-security/</a>
 * 
 * @author man-at-home
 * @since 2014-10-12
 */
@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired private UserDetailsServiceImpl userDetailsServiceImpl;
    
    
    /** secure most pages. Exceptions are index.html and javascript / css resources.  */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        
        LOG.info("WEB: enabling security (configure form based login)");
        
        http
            .authorizeRequests()
                .antMatchers("/", "/index.html", "/js/*", "/css/*", "/bootstrap/**/*", "/images/*", "/api-docs", "/api-docs/**/*")
                .permitAll()    // no login: static ressources (html, css, javascript) including swagger rest ddl.
                .anyRequest()
                .authenticated();  
        http
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .permitAll();
        
        http.csrf().disable();  // w/ unit-tests, waiting on spring-security-test!.
        
        http
            .exceptionHandling()
            .accessDeniedPage("/403");
    }  
    
    
    /** use own user implementation. */
    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {
        
        LOG.debug("WEB: configuring userDetailsService " + userDetailsServiceImpl);
        auth.userDetailsService(userDetailsServiceImpl)
            .passwordEncoder(User.getPasswordEncoder())
            ;  
    }
    
    
    /** second configuration: using basic authentication for totask2.mobile's REST API. */
    @Configuration
    @Order(1) 
    public static class RestApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        
        @Autowired private UserDetailsServiceImpl userDetailsServiceImpl;        
        
        /** use basic authentication for /APP/REST URLs. */
        protected void configure(HttpSecurity http) throws Exception {
            
            LOG.info("APP/REST REST Api: enabling security (configure basic auth)");
            
            http
                .antMatcher("/APP/REST/**")
                .authorizeRequests()
                .antMatchers("/APP/REST/**").hasRole("USER")
                .and()
                .httpBasic();
            
            http.csrf().disable();  // w/ rest posts APP/REST/WorkEntry
        }
        
        /** use same users as web application. */
        @Override
        protected void configure(AuthenticationManagerBuilder auth)
                throws Exception {
            
            LOG.debug("APP/REST REST Api: configuring userDetailsService " + userDetailsServiceImpl);
            auth.userDetailsService(userDetailsServiceImpl)
                .passwordEncoder(User.getPasswordEncoder())
                ;
        }
    }//static inner class

    
    /** third configuration: using basic authentication for metrics REST based below /monitor. 
     * 
     * Extra role "MONITOR" is needed with basic authentication, matches the url given in /application.yml for spring actuator endpointds. 
     * */
    @Configuration
    @Order(2) 
    public static class MonitoringApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        
        @Autowired private UserDetailsServiceImpl userDetailsServiceImpl;        
        
        /** use basic authentication for /APP/REST URLs. */
        protected void configure(HttpSecurity http) throws Exception {
            
            LOG.info("MONITOR/ REST Api: enabling security (configure basic auth)");
            
            http
                .antMatcher("/monitor/**")
                .authorizeRequests()
                .antMatchers("/monitor/**").hasRole("MONITOR")
                .and()
                .httpBasic();
            
            http.csrf().disable();  // w/ rest posts APP/REST/WorkEntry
        }
        
        /** use same users as web application. */
        @Override
        protected void configure(AuthenticationManagerBuilder auth)
                throws Exception {
            
            LOG.debug("MONITOR/ REST Api: configuring userDetailsService " + userDetailsServiceImpl);
            auth.userDetailsService(userDetailsServiceImpl)
                .passwordEncoder(User.getPasswordEncoder())
                ;
        }
    }//static inner class
}//class
