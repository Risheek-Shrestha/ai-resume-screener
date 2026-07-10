package com.risheek.resume_screener.config;

import com.risheek.resume_screener.jwt.JwtAuthFilter;
import com.risheek.resume_screener.service.CustomUserDetailService;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailService customUserDetailService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CustomUserDetailService customUserDetailService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customUserDetailService = customUserDetailService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/v1/users/register").permitAll()
                        .requestMatchers(HttpMethod.PUT,"/api/v1/users/**").authenticated()
                        .requestMatchers(HttpMethod.GET,"/api/v1/users/**").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/open").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/me").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/resumes/**").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/resumes/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/resumes/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/resumes/**").hasRole("USER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/scores/**").hasRole("USER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/**").hasRole("USER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/me").hasRole("USER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/jobs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/notifications/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS",
                "PATCH"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}