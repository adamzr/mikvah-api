package org.lamikvah.website;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired private MikvahConfiguration config;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        JwtWebSecurityConfigurer
                .forRS256(config.getAuth0().getApiAudience(), config.getAuth0().getIssuer())
                .configure(http)
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/test-no-auth").permitAll()
                .antMatchers(HttpMethod.GET, "/appointments/availability").permitAll()
                .antMatchers(HttpMethod.GET, "/hours").permitAll()
                .antMatchers(HttpMethod.POST, "/donate").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET, "/test-auth").hasAuthority("create:appointments")
                .antMatchers(HttpMethod.GET, "/user").authenticated()
                .antMatchers(HttpMethod.POST, "/user").authenticated()
                .antMatchers(HttpMethod.GET, "/health").permitAll()
                .antMatchers(HttpMethod.GET, "/metrics").permitAll()
                .antMatchers(HttpMethod.GET, "/info").permitAll()
                .antMatchers(HttpMethod.POST, "/webhook").permitAll()
                .antMatchers(HttpMethod.GET, "/attendent-daily-list").permitAll()
                .antMatchers(HttpMethod.GET, "/health").permitAll()
                .anyRequest().authenticated();

    }

    @Override
    public void configure(WebSecurity web) throws Exception {

        web
        .ignoring()
            .antMatchers(HttpMethod.GET, "/hours")
            .antMatchers(HttpMethod.POST, "/donate-guest")
            .antMatchers(HttpMethod.GET, "/appointments/availability");

    }


}
