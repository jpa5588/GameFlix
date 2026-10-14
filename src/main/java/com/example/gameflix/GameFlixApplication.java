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
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
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

                        // Accessing the current user's profile requires a valid JWT.
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

                        // Deny all other requests.
                        .anyRequest().denyAll()
                )

                // Enable JWT authentication for requests containing a token.
                .oauth2ResourceServer(oauth2 -> oauth2

                        // Use the JwtDecoder bean configured in JwtConfig.
                        .jwt(Customizer.withDefaults())
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

}
