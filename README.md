# GameFlix — Login and Registration

This project provides user registration, login, and a protected user-profile endpoint. It uses Java 21, Spring Boot 4.1.1, Gradle, and MySQL.

## Database Schema
CREATE TABLE `accounts` (
`account_id` int NOT NULL AUTO_INCREMENT,
`created_time` timestamp NOT NULL,
`display_name` varchar(100) NOT NULL,
`email` varchar(255) NOT NULL,
`password_hash` varchar(255) NOT NULL,
`status` varchar(30) NOT NULL,
PRIMARY KEY (`account_id`),
UNIQUE KEY `uk_accounts_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

## Setup and Run Instructions

### Requirements
- JDK 21 installed and available in the terminal.
- MySQL Server running on localhost, port 3306.
- Postman for testing the API.

### 1. Create the database

Start MySQL and run this statement in MySQL Workbench:

CREATE DATABASE IF NOT EXISTS gameflix_db;

My local setup uses the MySQL `root` account. Another account can be used if it has permission to create and alter tables and read and write data in `gameflix_db`.

The database connection in `application.properties` points to `localhost:3306/gameflix_db`.

### 2. Configure the local settings

For a new setup, copy:

`src/main/resources/application.properties.example`

to:

`src/main/resources/application-local.properties`

Replace the placeholders with your MySQL username, MySQL password, and JWT signing secret:

spring.datasource.username=YOUR_DATABASE_USERNAME
spring.datasource.password=YOUR_DATABASE_PASSWORD
gameflix.jwt.secret=YOUR_BASE64_SIGNING_SECRET

The application imports this local file through `spring.config.import`. The local file is excluded from Git through `.gitignore`.

### 3. Generate a JWT signing secret

For a new setup, generate a random 32-byte key and encode it as Base64.

Open a powershell window and enter:

```powershell
$jwtBytes = New-Object byte[] 32; $jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create(); $jwtRandom.GetBytes($jwtBytes); [Convert]::ToBase64String($jwtBytes); $jwtRandom.Dispose()
```
Copy the printed Base64 value into `gameflix.jwt.secret` in your local properties file. Close powershell

Keep the signing secret in the local file.

### 4. Run the application

Open a terminal in the project root, where `build.gradle` is located.

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Alternatively, open the project in IntelliJ, select JDK 21, let Gradle sync, and run the main method in `GameFlixApplication`.

Hibernate creates or updates the tables because `spring.jpa.hibernate.ddl-auto` is set to `update`.

### 5. Confirm startup

Check the console for `Started GameFlixApplication`.

The API runs at `http://localhost:8080`. Use Postman to test:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`

The `/api/users/me` endpoint requires the JWT returned by login, supplied as a bearer token.

## Password Hashing and Work-Factor Justification

I chose bcrypt because it is recommended in the assignment and is available through the existing Spring Security dependency. I explicitly configured a strength of 12, above the minimum work factor of 10 in OWASP’s bcrypt guidance, to increase the work required to guess passwords if stored hashes are exposed. Bcrypt’s cost grows exponentially, so strength 12 requires approximately four times the hashing work of strength 10; I accepted the additional processing time during registration and login to make password guessing more expensive.

Spring Security’s bcrypt encoder generates a random salt for each password, which helps prevent attackers from reusing precomputed hashes across accounts. During registration, my service hashes the password before saving the account. During login, the password encoder checks the submitted password against the stored hash.

I use DelegatingPasswordEncoder so each stored hash includes the `{bcrypt}` prefix. This identifies the encoder needed to verify the password and allows the configuration to support additional algorithms later while retaining bcrypt verification.

## Hibernate Schema Setting

I chose `spring.jpa.hibernate.ddl-auto=update` for local development and testing. With this setting, Hibernate uses the entity mappings to create missing tables and apply supported schema changes, such as adding missing columns. This reduces the manual database setup needed while I develop the application.

Keeping existing account records between restarts is useful because I can repeat login, duplicate-registration, and protected-profile tests using the same accounts. The `create` and `create-drop` settings rebuild the schema and remove existing records, which would require me to register my test accounts again. The `update` setting fits this assignment because I need both automatic table setup and persistent test data.

## JWT Lifetime Justification

I chose a JWT lifetime of 900 seconds, or 15 minutes, to balance usability with limiting token reuse. This gives a user time to make several authenticated API requests after logging in. A very short lifetime would require frequent logins, while a longer lifetime would give someone who obtained a stolen token more time to use it.

My token service sets the expiration claim to 900 seconds after the token’s creation time. The decoder checks that expiration when validating protected requests. Spring Security also allows a default 60-second tolerance for clock differences, so rejection may occur shortly after the expiration timestamp.

Once the token is no longer accepted, protected requests return 401 Unauthorized. My application does not implement refresh tokens, so the user must log in again to receive a new access token.

## Test Evidence

I used Postman to test registration, login, and the protected user-profile endpoint. I checked successful requests and requests that should fail because of invalid input, duplicate information, incorrect credentials, or an invalid token.

| Test | Request | Observed Result |
|---|---|---|
| Register Success | POST /api/auth/register | 201 Created; Postman Test Account created; Display Name: Postman Test, Email: PostmanTest@gmail.com, Password: GameFlixPostManTest1000! |
| Duplicate email | POST /api/auth/register | 409 Conflict; duplicate account rejected; Display Name: Postman Test, Email: PostmanTest@gmail.com, Password: GameFlixPostManTest1000! |
| Missing display name | POST /api/auth/register | 400 Bad Request; No Email: Display Name: Postman Test, Email: , Password: GameFlixPostManTest1000! |
| Correct login credentials | POST /api/auth/login | 200 OK; access token returned; Email: PostmanTest@gmail.com, Password: GameFlixPostManTest1000!, Token generated: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnYW1lZmxpeCIsInN1YiI6IjQiLCJhdWQiOiJnYW1lZmxpeC1hcGkiLCJleHAiOjE3OTIwMDA1OTQsImlhdCI6MTc5MTk5OTY5NH0.TlFrIBmq6MSUwTbQxVFJwfQpvtF16Pr79_wCHpKzMwk |
| Incorrect password | POST /api/auth/login | 401 Unauthorized; generic credential error; Email: PostmanTest@gmail.com, Password: GameFlixPostManTest |
| Valid token | GET /api/users/me | 200 OK; authenticated account’s profile returned; Token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnYW1lZmxpeCIsInN1YiI6IjQiLCJhdWQiOiJnYW1lZmxpeC1hcGkiLCJleHAiOjE3OTIwMDA1OTQsImlhdCI6MTc5MTk5OTY5NH0.TlFrIBmq6MSUwTbQxVFJwfQpvtF16Pr79_wCHpKzMwk |
| Modified token | GET /api/users/me | 401 Unauthorized; Token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnYW1lZmxpeCIsInN1YiI6IjQiLCJhdWQiOiJnYW1lZmxpeC1hcGkiLCJleHAiOjE3OTIwMDA1OTQsImlhdCI6MTc5MTk5OTY5NH0.TlFrIBmq6MSUwTbQxVFJwfQpvtF16Pr79_wCHpKz |
| Allowed frontend origin | OPTIONS /api/users/me with Origin http://localhost:5173 | 200 OK; matching Access-Control-Allow-Origin header: Localhost:5137 |
| Disallowed frontend origin | OPTIONS /api/users/me with Origin http://localhost:3000 | 403 Forbidden; CORS request rejected: Localhost: 3000 |

Screenshots of all these tests are in the submitted word document

I also inspected the test account in MySQL and confirmed that password_hash contained a bcrypt hash with the `{bcrypt}` prefix instead of the original password.

## Module 3 Design-Change Note

This implementation follows the account-management responsibilities from my Module 3 design. I kept the Accounts structure containing Account_ID, Display_Name, Email, Password_Hash, Status, and Created_Time. I implemented the unique-email rule from the ERD as a database constraint and added registration handling that returns 409 Conflict when an email already exists.

I changed the registration endpoint from POST /accounts to POST /api/auth/register and the login endpoint from POST /sessions to POST /api/auth/login to follow this assignment’s required routes. Registration still returns 201 Created, and successful login returns a signed JWT.

For this assignment’s profile lookup, I implemented GET /api/users/me. It gets the account ID from the validated JWT subject, allowing the user to retrieve their own profile without supplying an account ID in the URL.

The User class also has a temporary password property for receiving registration and login input. That property is not stored in the database, so it does not add a column to the Module 3 schema. Only the password hash is persisted, and both password properties are excluded from JSON responses.

During my design review, I removed the uniqueness requirement for Display_Name because different users may choose the same display name. I kept Email unique because it identifies the account during registration and login. This allows users to share a display name while ensuring each email address belongs to only one account.

## AI Usage and Change Log
### AI Usage
#### Tools Used
    1. ChatGpt
#### Prompts Used
    1. We are going to use bcrypt as our password encoder method, breakdown the workflow and code needed to implement
    2. Help breakdown the securityfilterchain workflow/module needed for the build of GameFlix login and registration
    3. Provide a regular expression for email addresses in order to check if the email entered is following the standard format
    4. I created in my service, messages to make it clear what happens when something doesn't meet a certain criteria, email isn't correct, duplicate, account and so. How do I get json to output a message in my api tests?
    5. Help setup the jwt secret key for gameflix, step through the process to create one
    6. Now that the JWT is generated provide a code configuration to use it in gameFlix
    7. Ok now that JwTConfig is setup lets now get the service, provide me code that JwtService will need to work, this is an interface class
    8. Ok now that the JwtService is at least setup, lets now configure the JwtServiceImpl so it knows what to do with a JWT
    9. I need to verify when a user trys to login check the account or display name that is attached to the email, if no account exists throw an error
    10. Help put together the code for the /login endpoint using the JWT configuration in controller
    11. Provide the JWT authentication using oauth2ResourceServer within the securityfilterchain
    12. Help configure the endpoint for /api/users/me
    13. Help setup the configuration for cors, that only allows the following, "CORS is configured for your front-end origin only, not everything". This detail was overlooked during the build of our securityfilterchain
#### Output Generated
    1. @Bean
    public PasswordEncoder passwordEncoder() {

			Map<String, PasswordEncoder> encoders = Map.of("bcrypt", new BCryptPasswordEncoder(12));

			return new DelegatingPasswordEncoder("bcrypt", encoders);
		}"
    2. "@Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UrlBasedCorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                );

        return http.build();
    }"
	3. ^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
	4. If you wish to have your errors outputting in a json then you need to call out the logic in your controller, while the service understands it your api logic does not,
	 @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRequestError(
            ResponseStatusException exception
    ) {

        String message = exception.getReason();

        if (message == null) {
            message = "Unable to process the request.";
        }

        
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }
	5. Generate 32 random bytes on your computer using a cryptographically secure generator, then represent those bytes as Base64 text. On Windows, .NET’s RandomNumberGenerator can do this through PowerShell.
		HS256 requires a key of at least 256 bits, which equals 32 bytes. The requirement concerns the decoded bytes, not the number of characters in the Base64 text.
		
		$jwtBytes = New-Object byte[] 32; $jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create(); $jwtRandom.GetBytes($jwtBytes); [Convert]::ToBase64String($jwtBytes); $jwtRandom.Dispose()
	6. @Bean
    public SecretKey jwtSecretKey(@Value("${gameflix.jwt.secret}") String secret) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "gameflix.jwt.secret must be valid Base64.",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "gameflix.jwt.secret must decode to at least 32 bytes."
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${gameflix.jwt.issuer}") String issuer,
            @Value("${gameflix.jwt.audience}") String audience) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimValidator<List<String>> audienceValidator =
                new JwtClaimValidator<>(
                        JwtClaimNames.AUD,
                        audiences -> audiences != null
                                && audiences.contains(audience)
                );

        JwtClaimValidator<Instant> expirationValidator =
                new JwtClaimValidator<>(
                        JwtClaimNames.EXP,
                        expiration -> expiration != null
                );

        JwtClaimValidator<String> subjectValidator =
                new JwtClaimValidator<>(
                        JwtClaimNames.SUB,
                        subject -> subject != null && !subject.isBlank()
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        audienceValidator,
                        expirationValidator,
                        subjectValidator
                )
        );

        return decoder;
    }
	7. public interface JwtService { String generateToken(User user); }
	8. Create JwtServiceImpl.java as a regular class inside your service package. It implements generateToken(User user) and uses the encoder from JwtConfig to sign the token. Spring’s encoder accepts the token’s header and claims together,public class JwtServiceImpl implements JwtService {

    private final JwtEncoder jwtEncoder;

    private final String issuer;

    private final String audience;

    private final long expirationSeconds;

    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            @Value("${gameflix.jwt.issuer}") String issuer,
            @Value("${gameflix.jwt.audience}") String audience,
            @Value("${gameflix.jwt.expiration-seconds}") long expirationSeconds
    ) {

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String generateToken(User user) {

        if (user == null || user.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "A saved user is required to create a token."
            );
        }

        Instant now = Instant.now();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)

                .type("JWT")

                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()

                .issuer(issuer)

                .subject(user.getAccountId().toString())

                .audience(List.of(audience))

                .issuedAt(now)

                .expiresAt(now.plusSeconds(expirationSeconds))

                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    9. User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No Email Exists"));
    10. @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> login(@RequestBody User request) {

				User user = userService.login(request);

				String token = jwtService.generateToken(user);

				Map<String, String> response = Map.of(
						"accessToken", token,
						"tokenType", "Bearer"
				);

				HttpHeaders headers = new HttpHeaders();
				headers.setCacheControl("no-store");
				
				return new ResponseEntity<Map<String, String>>(
						response, headers, HttpStatus.OK);
			}
		11. .oauth2ResourceServer(oauth2 -> oauth2

                        .jwt(Customizer.withDefaults())
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
		12. We’ll add the account lookup to your existing service and create UserRestController. Spring will supply the verified JWT through @AuthenticationPrincipal; its subject contains the account ID we stored when creating the token. docs.spring.io
			First, update UserService.java: User getCurrentUser(String subject);
			Next, add this method inside UserServiceImpl, below your existing methods: @Override
				public User getCurrentUser(String subject) {

					Integer accountId;

					try {
						accountId = Integer.valueOf(subject);

					} catch (NumberFormatException exception) {

						throw new ResponseStatusException(
								HttpStatus.UNAUTHORIZED, "Authentication is required.");
					}

					User user = userRepository.findById(accountId).orElse(null);

					if (user == null || !"ACTIVE".equals(user.getStatus())) {
						throw new ResponseStatusException(
								HttpStatus.UNAUTHORIZED, "Authentication is required.");
					}
					
					return user;
				}
			Create UserRestController.java inside your controller package: @Autowired
				private UserService userService;

				@RequestMapping(value = "/me", method = RequestMethod.GET)
				public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {

					if (jwt == null) {
						throw new ResponseStatusException(
								HttpStatus.UNAUTHORIZED, "Authentication is required.");
					}

					User user = userService.getCurrentUser(jwt.getSubject());
					HttpHeaders headers = new HttpHeaders();
					headers.setCacheControl("no-store");
					
					return new ResponseEntity<User>(user, headers, HttpStatus.OK);
				}

				@ExceptionHandler(ResponseStatusException.class)
				public ResponseEntity<Map<String, String>> handleRequestError(
						ResponseStatusException exception) {

					String message = exception.getReason();

					if (message == null) {
						message = "Authentication is required.";
					}

					return new ResponseEntity<Map<String, String>>(
							Map.of("message", message),
							exception.getStatusCode());
				}
				}
		13. @Bean
			public UrlBasedCorsConfigurationSource corsConfigurationSource() {

			CorsConfiguration configuration = new CorsConfiguration();

			configuration.setAllowedOrigins(
					List.of("http://localhost:5173")
			);

			configuration.setAllowedMethods(
					List.of("GET", "POST", "OPTIONS")
			);

			configuration.setAllowedHeaders(
					List.of("Content-Type", "Authorization")
			);

			configuration.setAllowCredentials(false);

			UrlBasedCorsConfigurationSource source =
					new UrlBasedCorsConfigurationSource();

			source.registerCorsConfiguration("/api/**", configuration);

			return source;
			}


### Change Log
#### What I modified
    1. I removed the need to use a Hash Map for password encoder which seemed to overkill for hashing passwords
    2. I took the output on the securityfilterchain module and tested to prove it does work when sending responses through my endpoints, and it did work. However, even comparing to the https://spring.io/guides/gs/securing-web
    documentation it was not making sense and needed some clarification on what each was doing to understanding, so I took the time to slowly test each and document exactly what each was doing and remove unnecessary parts. I did not have much exposure to this and required assistance on making this work right and building it correctly
    3. The code to output my errors in json worked, however added detailed comments on what each line is doing to better understand it for other APIs that need error handling if I need to come back
    4. Much of the logic from the jwt code configuration did make some sense, but their was some inconsistent logic that either looked extra or lack of understanding what exactly its was doing. Slowly traced what the lines are doing and removed certain logic like !=null && !subject.isBlank, which both mean the same thing just written different ways, removed that and updated the entire code base with detailed comments on what a line is doing
    5. The JwtService code was pretty straight forward and simple, nothing that hasn't been seen before. Modified slightly to include comments on the lines and what their end goal is
    6. Not much truly needed to be changed here, tracing what its doing made sense, however as some elements were new to me this required some detailed comments on what exactly it was doing and why to fully understand things
    7. The logic to look up the email and check if its exists was straightforward, but didn't feel like a one-liner was best as I prefer in this case to lookup and then in a separate function throw an error. So this was changed to better fit that kind of approach
    8. The code base for the login endpoint was just about what I expected followed the same format as the registration endpoint. However, due to lack of comments certain lines didn't make much sense, such as the storing of the tokentype which was a hardcoded value didn't feel necessary here and the header line which after testing it out held no value into the actual output in json. That was removed so was the tokentype, with comments explaining the workflow
    9. Detailed comments were added to explain what the oauth2 is doing with the JWT authentication, it made sense from the documentation and followed what should be expected to configure this
    10. The code base for the /api/users/me was pretty straightforward, while it wasn't necessary to use AI to generate that. Felt it was good practice to see what its doing and not doing. It generated all the code needed to configure the api for /user/me, but it required some fine tuning, comments where added to explain certain lines and pointless lines were removed as they did seem necessary for this setup
    11. The cors configuration code was pretty straightforward walked through what frontend values we should or in some cases not be accepting, the code was easy to understand but lacked explanations on what each line was doing so that was modified to provide clarity with a note on how our filtersecuritychain uses it
#### Why Changes were needed
    1. While the change wasn't necessarily required, I simply did not agree with the approach AI was giving and wanted to follow a similar approach in the documentation from spring boot where hardcode the bcrypt prefix and then use a simple mapping function to fine tune the hashing work factor
    2. The first securityfilterchain was a bit unclear and added too much complexity that I did not quite understand and could not justify, I had to slowly work through each part and get an understanding of what it was doing and what needed to change. In order to better comment what each is doing and why it is needed
    3. While the code looks straightforward, and it did work, I didn't want just place code in this project until I actually go through test what each line is really doing and have detailed comments so I know for future use, as there were certain elements I've never used so just want to be sure I understand what its doing exactly
    4. The configuration for jwt worked, there was no confusion about that, but as this is an area that can get confusing real quick this required detailed comments and explanation of what is going on and an update to certain logic that didn't make sense and needed to reflect not only me as the developer understands but also others as well. Detailed comments and changes to logic to test were updated to reflect this
    5. The code for JwtService made sense, this wasn't about what it was doing or not doing, but as this was AI generated I need to have explanations on what it was doing, so comments where added to explain things.
    6. The implementation code for Jwt needed clarification on what its doing, detailed comments were added to elaborate on the lines roles and how they work. Some logic was removed or fine-tuned to better understand it for someone who either has limited exposure to setting up jwt or no exposure. So explanations where added to the code base
    7. While the logic could work, my end goal was more just keeping it simple and not adding confusion, plus never been a fan of the -> when if statements can work just as well. This was updated to provide clarity and keep the functions separate and not try to make a line to too much, plus detailed comments were added to elaborate on this change
    8. The lack of comments made it a bit unclear what was happening, so provided comments to better justify and explain the work going on. Removed the hardcoded tokentype as it seemed unnecessary and took out the header information around caching which held no negative outcomes nor positive outcomes just seemed extra for this login/registration
    9. The lack of comments made it difficult to understand what each line was doing, so each line was traced, tested, and provided comments on the exact role so I can understand it and what the goal is for future build
    10. Comments were added to all the lines to understand what they were doing, which was pretty similar to auth endpoints, certain lines were removed like the header information which wasn't doing anything went sent in the postman tests, so they were removed to stay consistent with the other endpoints.
    11. The comments were added as to understand the logic behind the cors setup and how its working, so I can understand it at a later date and potentially others as well if they were being asked to look this over
#### What AI got or missed
    1. AI did a decent job of setting up some things early for this assignment, however making the securityfilterchain far more complicated than it needed to be. I had to do extensive rewrite of that section to better understand the end goal.
    2. AI was extremely helpful with the JWT setup, the documentation at times was a bit unclear due to my lack of exposure to these things. AI was able to break down what I was trying to set up those tokens and offer suggestions on how to setup. For example, it repeatable suggested for the JWT should be stored as part of a configuration package in my project, which after carefully following that structure, I fully agreed with that approach and was able to carefully implement this in a way that works.
    3. AI did overlook something during my build of the configuration chain, it completely missed the cors configuration I had to consistently tell it what it did wrong as I did not realize this was missing until I asked it for an example to test until it finally got the idea of what we missed during the build. While it provided the code base for it, it got a bit frustrating that I had to do that multiple times to get AI to understand.


### Reflection
#### Did AI help or hinder your work?
AI was a valuable tool in this work, setting up a login and registration process is no easy task, while not hard as it gets into an area you really need to understand. Prior to this I knew nothing about how to hash passwords, implement password hashing and JWT authentication, setting up things like jwt tokens, and other things. While the documentation helped explain how it works, AI gave me better clarity and a careful walkthrough on setting these things up and how I should be testing this. AI in this assignment was a very helpful resource and has given me a much better understanding of how to set this up. While it wasn't perfect all the time, but I can now understand what to approach carefully if I ever need to do this again
#### What would you do differently?
If I were to do anything differently I would make sure AI has the documentation I've been referencing to provide code that made sense to me. While yes, it was a very valuable resource in this process it had times were the code didn't make sense to me and I have to carefully trace and provide a detailed explanations in my comments. Which is not a bad thing, just had I pointed AI to how and why I want it done this way, it could've had saved me a lot of time and tedious effort to trying to reverse engineer and explain every detail. I could've had also made AI tell me what each line is doing also, had I done that could've saved me a lot of work as well.

