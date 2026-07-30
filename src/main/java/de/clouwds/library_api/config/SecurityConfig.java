package de.clouwds.library_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //Todo: maybe custom success Handler (SavedRequestAwareAuthenticationSuccessHandler)
        http.formLogin(form -> form
                .defaultSuccessUrl("/api/books", false));
        http.logout(logoutCustomizer -> logoutCustomizer.invalidateHttpSession(true));
        //Todo: matchers by role
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/authors/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

}
