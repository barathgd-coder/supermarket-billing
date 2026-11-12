package com.supermart.config;

import com.supermart.model.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/logo", "/error").permitAll()
                .requestMatchers("/", "/access-denied").authenticated()
                // Cashiers: Billing, Customers and Sales History only
                .requestMatchers("/billing/**", "/customers/**", "/sales/**").hasAnyRole("ADMIN", "CASHIER")
                // everything else (dashboard, products, inventory, suppliers, reports, settings): Admin
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));
        return http.build();
    }

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(a -> Role.ADMIN.equals(a.getAuthority()));
            response.sendRedirect(request.getContextPath() + (admin ? "/dashboard" : "/billing"));
        };
    }
}
