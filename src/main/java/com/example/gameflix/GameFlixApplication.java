package com.example.gameflix;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class GameFlixApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameFlixApplication.class, args);
    }


    // Make the password encoder available to our services.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder(
                // Select BCrypt and add {bcrypt} to stored hashes.
                "bcrypt",

                // Change value to adjust the BCrypt hashing strength.
                Map.of("bcrypt", new BCryptPasswordEncoder(12))
        );
    }


    // Register the returned SecurityFilterChain as a Spring-managed bean.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        //HttpSecurity variable used for the builder Spring supplies for configuring security.
        http

                // Apply our existing corsConfigurationSource() bean automatically.
                // That bean defines the frontend origins, methods, and headers allowed.
                .cors(Customizer.withDefaults())

                // Disable the CSRF token check for our API.
                // Our planned authentication sends JWTs in the Authorization header.
                .csrf(csrf -> csrf.disable())

                // Disable Spring's default logout endpoint.
                .logout(logout -> logout.disable())

                // Configure how Spring stores authentication between requests.
                .sessionManagement(session -> session

                        // Do not save authentication in an HTTP session.
                        // Protected requests will supply their own JWT.
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define which requests are allowed.
                // Spring checks these rules in the order written.
                .authorizeHttpRequests(requests -> requests

                        // Allow Spring's internal error handling to display API errors.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Registration and login endpoints to test without a JWT.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()

                        // Only administrators can access these routes.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Accessing the current user's profile requires a valid JWT.
                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("CUSTOMER", "ADMIN")

                        // Customers and administrators can list genres and view genre details.
                        .requestMatchers(HttpMethod.GET, "/api/genres", "/api/genres/*").hasAnyRole("CUSTOMER", "ADMIN")

                        // Only administrators can create genres.
                        .requestMatchers(HttpMethod.POST, "/api/genres").hasRole("ADMIN")

                        // Deny all other requests.
                        .anyRequest().denyAll()
                )

                // Validate JWTs and convert the role into a Spring authority.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                // Return HTTP 401 when a protected request is not authenticated.
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        // Build the filter chain from the settings above and return it to Spring.
        return http.build();
    }

    // Define which frontend can send browser requests to our API.
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Allow our planned frontend origin, pointing to port 5173 so we aren't using 8080 all the time
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        // Allow our endpoint methods and browser preflight requests.
        configuration.setAllowedMethods(
                List.of("GET", "POST", "OPTIONS")
        );

        // Allow JSON content and the bearer-token header.
        configuration.setAllowedHeaders(
                List.of("Content-Type", "Authorization")
        );

        // Our API uses bearer tokens instead of cookie authentication.
        configuration.setAllowCredentials(false);

        // Apply these CORS rules to our API endpoints.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    // Tell Spring how to turn our JWT role into an authority.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        // Read the role claim instead of Spring's default scope claims.
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");

        // Add the prefix Spring expects when we use hasRole().
        // For example, ADMIN becomes ROLE_ADMIN.
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        // Use these authorities when creating the authenticated user.
        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

}
