package com.frauas.servicemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                                // -------------------------------------------------
                                // PUBLIC / STATIC RESOURCES
                                // -------------------------------------------------
                                .requestMatchers("/", "/login",
                                        "/css/**", "/js/**", "/images/**", "/webjars/**")
                                .permitAll()

                                // -------------------------------------------------
                                // EXTERNAL API INTEGRATIONS (Groups 1, 2, 4, 5)
                                // -------------------------------------------------
                                .requestMatchers("/api/integration/**").permitAll() // ✅ New Professional Path
                                .requestMatchers("/service-requests/api/**").permitAll() // Allow read-only for reporting

                                // -------------------------------------------------
                                // SWAGGER DOCS (For sharing with other groups)
                                // -------------------------------------------------
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                // -------------------------------------------------

                                // PROJECT MANAGER (PM)
                                // -------------------------------------------------
                                .requestMatchers(HttpMethod.POST, "/service-requests").hasRole("PM")
                                .requestMatchers("/service-requests").hasRole("PM")
                                .requestMatchers("/service-requests/**").hasAnyRole("PM", "PO", "RP")

                                // -------------------------------------------------
                                // PROCUREMENT OFFICER (PO)
                                // -------------------------------------------------
                                .requestMatchers("/camunda/tasks/procurement_officer").hasRole("PO")
                                .requestMatchers(HttpMethod.POST, "/camunda/task/*/complete")
                                .hasAnyRole("PO", "RP", "PM")

                                // -------------------------------------------------
                                // RESOURCE PLANNER (RP)
                                // -------------------------------------------------
                                .requestMatchers("/camunda/tasks/resource_planner").hasRole("RP")
                                .requestMatchers("/offers/**").hasAnyRole("RP", "PM")

                                // -------------------------------------------------
                                // SHARED DASHBOARD / VIEWS
                                // -------------------------------------------------
                                .requestMatchers("/dashboard").hasAnyRole("PM", "PO", "RP")
                                .requestMatchers("/service-requests/view/**")
                                .hasAnyRole("PM", "PO", "RP")

                                // -------------------------------------------------
                                // DEFAULT RULE
                                // -------------------------------------------------
                                .anyRequest().authenticated()
                )

                // -------------------------------------------------
                // LOGIN CONFIG
                // -------------------------------------------------
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .permitAll()
                )

                // -------------------------------------------------
                // LOGOUT CONFIG
                // -------------------------------------------------
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                // -------------------------------------------------
                // ACCESS DENIED
                // -------------------------------------------------
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                )

                // -------------------------------------------------
                // DEV SETTINGS
                // -------------------------------------------------
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                );


        return http.build();
    }

    // =====================================================
    // ROLE-BASED LOGIN REDIRECT
    // =====================================================
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {

            var authorities = authentication.getAuthorities();
            String redirectUrl = "/service-requests"; // PM default

            // ✅ FIXED: Redirect to 'po_user' instead of 'procurement_officer'
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_PO"))) {
                redirectUrl = "/camunda/tasks/po_user";
            }
            // ✅ FIXED: Redirect to 'rp_user' instead of 'resource_planner'
            else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RP"))) {
                redirectUrl = "/camunda/tasks/rp_user";
            }

            response.sendRedirect(redirectUrl);
        };
    }

    // =====================================================
    // IN-MEMORY USERS (DEV)
    // =====================================================
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {

        UserDetails pm = User.withDefaultPasswordEncoder()
                .username("pm_user")
                .password("password")
                .roles("PM")
                .build();

        UserDetails po = User.withDefaultPasswordEncoder()
                .username("po_user")
                .password("password")
                .roles("PO")
                .build();

        UserDetails rp = User.withDefaultPasswordEncoder()
                .username("rp_user")
                .password("password")
                .roles("RP")
                .build();

        return new InMemoryUserDetailsManager(pm, po, rp);
    }
}
