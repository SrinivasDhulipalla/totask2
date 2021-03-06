package org.manathome.totask2.model;


import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.manathome.totask2.service.UserCachingService;
import org.manathome.totask2.service.UserDetailsServiceImpl;
import org.manathome.totask2.util.AAssert;
import org.manathome.totask2.util.Authorisation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

















import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


/**
 * User who can log his work into totask2 application.
 * 
 * spring-security: Provides core user information.
 * Implementations are not used directly by Spring Security for security purposes. They simply store user information 
 * which is later encapsulated into Authentication objects. This allows non-security related user information (such as email 
 * addresses, telephone numbers etc) to be stored in a convenient location. 
 * 
 * @see <a href="http://projects.spring.io/spring-security/">spring security</a>
 * @see UserCachingService
 * @see UserDetailsServiceImpl
 * 
 * @author man-at-home
 * @since  2015-08-17
 */
@Entity
@Audited
@ApiModel(description = "user")
@Table(name = "TT_USER", 
       indexes = {@Index(name = "IDX_TT_USER_NAME", unique = true, columnList = "USER_NAME")}
      )
@AuditTable("TT_USER_HISTORY")
public final class User implements UserDetails {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(User.class);    

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)    
    private long     id;
    
    @Size(min = 3, max = 50)
    @NotNull
    @Column(name = "USER_NAME", nullable = false, length = 50)
    private String userName;
    
    @Size(min = 3, max = 250)
    @NotNull
    @Column(name = "DISPLAY_NAME", nullable = false, length = 250)
    private String displayName;
    
    @Column(name = "ACTIVE", nullable = false)
    private boolean active;
    
    @Column(name = "PASSWORD", nullable = true, length = 100)
    private String password;
    
    @Column(name = "IS_ADMIN", nullable = false)
    private boolean isAdmin;
    
    @Version    
    private long version;
    
    /** ctor. */
    public User() {
        this.userName = ""; // never be null..
    }

    /** internal pk. */
    @ApiModelProperty(value = "unique id of user (PK)")
    public long getId() { 
        return id; 
    }
    
    /** internal pk. */
    public void setId(long id) { 
        AAssert.checkZero(this.id, "user.id already set"); 
        this.id = id;         
    }
    
    /** UserId used to login into this application.*/
    @Override
    @ApiModelProperty(value = "unique username, used for login into totask2.", required = true)
    public String getUsername() {
        return userName;
    }

    /** change name. */
    public void setUsername(@NotNull final String userName) {
        this.userName = userName;
    }

    /** display friendly name of this user, shown in dialogs. */
    @ApiModelProperty(value = "display friendly name of this user, shown in dialogs.", required = true)
    public String getDisplayName() {
        return displayName;
    }

    /** change display name. */
    public void setDisplayName(@NotNull final String displayName) {
        this.displayName = displayName;
    }

    /** may the given user login into this application? */
    @JsonIgnore
    public boolean isActive() {
        return active;
    }
    
    /** enable/disable user. */
    public void setActive(final boolean active) {
        this.active = active;
        if (!active) {            
            LOG.debug("deactivating {0}" + this.userName);
        }
    }
    
    /** is it a totask2 admin? */
    @JsonIgnore
    public boolean isAdmin() {
        return this.isAdmin;
    }    

    /** make user admin. */
    public void setAdmin(final boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    
    /** change password of given user. */
    public void changePasswort(String newUnencryptedPassword) {
        this.password = getPasswordEncoder().encode(newUnencryptedPassword);
    }
   
    
    /** debug. */
    @Override
    public String toString() {
        return "User [" + id + ", username=" + userName + ", active=" + active + "]";
    }

    /** returns the authorities granted to the user.
     *  active rights/roles of user (currently only admin). */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
                
        ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(); 

        authorities.add(new SimpleGrantedAuthority(Authorisation.ROLE_USER)); // anyone is user
        
        if (this.isAdmin()) {
            GrantedAuthority adminRole = new SimpleGrantedAuthority(Authorisation.ROLE_ADMIN);
            authorities.add(adminRole);
        }
        
        if (this.isAdmin() || this.getUsername().startsWith("monitor")) {
            GrantedAuthority adminRole = new SimpleGrantedAuthority(Authorisation.ROLE_MONITOR);
            authorities.add(adminRole);            
        }
        
        LOG.debug("authorities of user " + this.getDisplayName() + " requested: " + authorities.size());
        return authorities;
    }

    /** password, used by spring security. */
    @Override
    @JsonIgnore
    public String getPassword() {
        
        if (this.password == null || this.password.length() <= 0) {
            LOG.debug("START-password of user " + this.getDisplayName() + " requested.");        
            return getPasswordEncoder().encode("t123456");           
        } else {
            LOG.debug("custom-password of user " + this.getDisplayName() + " requested.");  
            return this.password;
        }
    }

    /** Indicates whether the user's account has expired. */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return this.isActive();
    }

    /** Indicates whether the user is locked or unlocked. */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return this.isActive();
    }

    /** Indicates whether the user's credentials (password) has expired.*/
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return this.isActive();
    }

    /** Indicates whether the user is enabled or disabled. */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return this.isActive();
    } 
    
    /** editTaskAssignment.html#user will be used for autocompletion JSON. */
    public String getValue() { 
        return this.getLabel(); 
    }
    
    /** editTaskAssignment.html#user will be used for autocompletion JSON. */
    public String getLabel() { 
        return this.getUsername() + ": " + this.getDisplayName(); 
    }
    
    /** 
     * password encoding algorithm.
     * 
     * @see #password
     * @see BCryptPasswordEncoder
     * @return algorithm
     */
    public static BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** needed implementation. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    /** needed implementation. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        User other = (User) obj;
        if (userName == null) {
            if (other.userName != null) {
                return false;            
            }
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }
    
    
    
    /** internal logging helper. */
    public static void dumpAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            LOG.debug("user: " + auth.getName() + " " + auth.isAuthenticated() + " roles " + auth.getAuthorities().size()); //get logged in username
 
            auth.getAuthorities()
            .stream().
            forEach(ga ->  
                LOG.debug("  user-role:" + ga)
            );
        } else {
            LOG.debug("no auth.user user");
        }           
    }
}
