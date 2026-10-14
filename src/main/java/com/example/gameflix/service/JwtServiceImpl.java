package com.example.gameflix.service;

import com.example.gameflix.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

// Spring uses this class to carry out the work listed in JwtService.
@Service
public class JwtServiceImpl implements JwtService {

    // Store the encoder that JwtConfig provides for signing tokens.
    private final JwtEncoder jwtEncoder;

    // Store the name of the application creating the token.
    private final String issuer;

    // Store the API that the token is intended for.
    private final String audience;

    // Store how long the token lasts, measured in seconds.
    private final long expirationSeconds;

    // Supply the encoder and reads these settings when creating this service.
    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            @Value("${gameflix.jwt.issuer}") String issuer,
            @Value("${gameflix.jwt.audience}") String audience,
            @Value("${gameflix.jwt.expiration-seconds}") long expirationSeconds
    ) {

        // Save the supplied encoder and settings for use in generateToken().
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationSeconds = expirationSeconds;
    }

    // Provide the code for the method declared in JwtService.
    @Override
    public String generateToken(User user) {

        // Check to ensure the account in question has an account ID
        // If user doesn't have one, then throw error
        if (user == null || user.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "A saved user is required to create a token."
            );
        }

        // Capture the current time once for the token's timestamps.
        Instant now = Instant.now();

        // Build the header using the same HS256 algorithm as JwtConfig.
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)

                // Identify the token type as JWT.
                .type("JWT")

                // Build the header
                .build();

        // Build the claims, using the information inside the token.
        JwtClaimsSet claims = JwtClaimsSet.builder()

                // Identify GameFlix as the application creating the token.
                .issuer(issuer)

                // Use the account ID as the subject and convert it to text.
                .subject(user.getAccountId().toString())

                // Include the audience so JwtConfig can check it later.
                .audience(List.of(audience))

                // Record when the token was created.
                .issuedAt(now)

                // Add the configured lifetime to determine when it expires.
                .expiresAt(now.plusSeconds(expirationSeconds))

                // Build the Jwt claim
                .build();

        // Give the encoder the header and claims so it can sign the token.
        // Return the finished token as text for the login response.
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}