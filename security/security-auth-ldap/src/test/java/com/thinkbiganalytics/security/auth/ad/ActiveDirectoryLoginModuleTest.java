/**
 * 
 */
package com.thinkbiganalytics.security.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.thinkbiganalytics.auth.config.SecurityConfig;
import com.thinkbiganalytics.auth.jaas.config.JaasAuthConfig;
import com.thinkbiganalytics.security.GroupPrincipal;
import com.thinkbiganalytics.security.UsernamePrincipal;

/**
 * Test for the ActiveDirectoryLoginModule.  Note that these tests are disabled by default as it 
 * requires an actual Active Directory instance running as configured in the file ad-test.properties.
 * 
 * @author Sean Felten
 */
@SpringApplicationConfiguration(classes = { 
                                            SecurityConfig.class, 
                                            JaasAuthConfig.class, 
                                            ActiveDirectoryAuthConfig.class, 
                                            ActiveDirectoryLoginModuleTestConfig.class, 
                                            })
@TestPropertySource("classpath:ad-test.properties")
@ActiveProfiles("auth-ad")
public class ActiveDirectoryLoginModuleTest extends AbstractTestNGSpringContextTests {
    
    @Inject
    private ActiveDirectoryLdapAuthenticationProvider authProvider;
    
    @Inject
    private UserDetailsContextMapper userMapper;

    
//    @Test
    public void testLoginAdmin() throws Exception {
        Subject subject = login("dladmin", "Th1nkb1g!");
        
        assertThat(subject.getPrincipals()).contains(new UsernamePrincipal("dladmin"), new GroupPrincipal("Admin"));
    }
    
//    @Test
    public void testLoginTest() throws Exception {
        Subject subject = login("test", "Th1nkb1g!");
        
        assertThat(subject.getPrincipals()).hasSize(3).contains(new UsernamePrincipal("test"), 
                                                                new GroupPrincipal("Admin"), 
                                                                new GroupPrincipal("Developer"));
    }
    
//    @Test(expectedExceptions=LoginException.class)
    public void testLoginBogus() throws Exception {
        login("bogus", "user");
    }
  
    private Subject login(String user, String password) throws LoginException {
        Map<String, Object> options = new HashMap<>();
        options.put(ActiveDirectoryLoginModule.AUTH_PROVIDER, this.authProvider);
        options.put(ActiveDirectoryLoginModule.USER_MAPPER, this.userMapper);
        
        Subject subject = new Subject();
        ActiveDirectoryLoginModule module = new ActiveDirectoryLoginModule();
        
        module.initialize(subject, 
                          createHandler(user, password), 
                          new HashMap<>(), 
                          options);
        
        try {
            boolean success = module.login();
            
            if (success) {
                module.commit();
            }
            
            return subject;
        } catch (LoginException e) {
            module.abort();
            throw e;
        }
    }
    
    private CallbackHandler createHandler(String user, String password) {
        return new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(user);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password.toCharArray());
                    }
                }
            }
        };
    }
}