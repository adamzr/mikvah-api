package org.lamikvah.website;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${auth0.apiAudience}")
    private String apiAudience;
    
    @Value("${auth0.issuer}")
    private String issuer;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        JwtWebSecurityConfigurer
                .forRS256(apiAudience, issuer)
                .configure(http)
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/test-no-auth").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET, "/test-auth").hasAuthority("create:appointments")
                .antMatchers(HttpMethod.GET, "/user").authenticated()
                .antMatchers(HttpMethod.POST, "/user").authenticated()
                .anyRequest().authenticated();
    }
}
