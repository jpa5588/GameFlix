package com.example.gameflix.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

// Tells Spring to load the configuration defined in this class.
@Configuration
public class JwtConfig {

    // Spring creates this key once and makes it available to other beans.
    @Bean
    // Reads the secret supplied by our local properties file.
    public SecretKey jwtSecretKey(@Value("${gameflix.jwt.secret}") String secret) {

        // This variable will hold the decoded signing key in an array of bytes
        byte[] keyBytes;

        // Try decoding the secret so we can give a clear error if it is invalid.
        try {

            // trim removes extra whitespace from the beginning and end.
            // getDecoder gets Java's Base64 decoder.
            // decode converts the saved text back into the original bytes.
            keyBytes = Base64.getDecoder().decode(secret.trim());

            // Invalid Base64 causes an IllegalArgumentException.
        } catch (IllegalArgumentException exception) {

            // Stop startup because we cannot use an invalid signing key.
            throw new IllegalStateException(

                    // Explain which setting needs to be corrected.
                    "gameflix.jwt.secret must be valid Base64.",

                    // Keep the original exception to help identify the problem.
                    exception
            );
        }

        // length gives us the number of decoded bytes.
        // HS256 requires at least 32 bytes, which equals 256 bits.
        if (keyBytes.length < 32) {

            // Stop startup if the key is too short.
            throw new IllegalStateException(
                    "gameflix.jwt.secret must decode to at least 32 bytes."
            );
        }

        // Turn the decoded bytes into a key for HMAC SHA-256.
        // This key is for JWT signing return gives Spring the key that this bean provides.
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // Makes the encoder available for our token service to use.
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        // Spring supplies the SecretKey bean created from jwtSecretKey function.
        // ImmutableSecret makes that key available to Nimbus encoder.
        // NimbusJwtEncoder uses the key when signing a token.
        // <> lets Java work out the required generic type.
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    // Makes the decoder available for Spring Security to use later.
    @Bean
    public JwtDecoder jwtDecoder(

            // Spring supplies the same key used by the encoder.
            SecretKey jwtSecretKey,

            // Reads who we expect to have created the token.
            @Value("${gameflix.jwt.issuer}") String issuer,

            // Reads which API we expect the token to be meant for.
            @Value("${gameflix.jwt.audience}") String audience) {

        // Start setting up the decoder that will check incoming tokens.
        NimbusJwtDecoder decoder = NimbusJwtDecoder

                // Use our signing key to verify the token's signature.
                .withSecretKey(jwtSecretKey)

                // Only accept the HS256 algorithm for signature verification.
                .macAlgorithm(MacAlgorithm.HS256)

                // Finish building the decoder.
                .build();

        // Check if the claim is for our intended audience or api, expected is gameflix-api
        JwtClaimValidator<List<String>> audienceValidator =

                // Create a validator for one claim.
                new JwtClaimValidator<>(

                        // AUD is the standard name for the "aud" claim.
                        JwtClaimNames.AUD,

                        // -> defines a small function that checks the claim.
                        // Check to make sure an audience value actually exists.
                        audiences -> audiences.contains(audience)
                );

        // This check requires an expiration value to be present.
        // Instant is the type Spring uses for the decoded expiration.
        JwtClaimValidator<Instant> expirationValidator =

                // Create aa check for a single claim.
                new JwtClaimValidator<>(

                        // EXP is the standard name for the "exp" claim.
                        JwtClaimNames.EXP,

                        // Reject the token if it has no expiration.
                        // Spring's time checks below determine whether it expired.
                        expiration -> expiration != null
                );

        // The subject will contain the account ID as text.
        JwtClaimValidator<String> subjectValidator =

                // Create a check for a single claim.
                new JwtClaimValidator<>(

                        // SUB is the standard name for the "sub" claim.
                        JwtClaimNames.SUB,

                        // Require a subject value.
                        // Check if subject is not blank
                        subject -> !subject.isBlank()
                );

        // Tell the decoder which claim checks it must run.
        decoder.setJwtValidator(

                // All the validators included here must pass.
                new DelegatingOAuth2TokenValidator<>(

                        // Keep Spring's standard time checks.
                        // Also require the issuer to match our configured value.
                        JwtValidators.createDefaultWithIssuer(issuer),

                        // Require the token to be meant for our API.
                        audienceValidator,

                        // Require the token to include an expiration.
                        expirationValidator,

                        // Require the token to include an account identifier.
                        subjectValidator
                )
        );

        // Give Spring the decoder with these validation rules configured.
        return decoder;
    }
}