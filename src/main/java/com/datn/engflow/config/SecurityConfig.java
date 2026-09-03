package com.datn.engflow.config;

import com.datn.engflow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
// audit-v5: @PreAuthorize("hasRole('ADMIN')") annotations exist across admin
// controllers but were inert without this — URL rules masked it. Restore
// defense-in-depth so a future path refactor cannot silently drop protection.
@EnableMethodSecurity
@RequiredArgsConstructor
/**
 * class SecurityConfig.
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vocabulary/search", "/api/vocabulary/dictionary/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/leaderboard").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/lessons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/speaking-prompts", "/api/v1/speaking-prompts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/video-prompts", "/api/v1/video-prompts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/video-lessons", "/api/v1/video-lessons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/decks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/shop/items").permitAll()
                .requestMatchers("/api/webhook/sepay").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ai/generate-vocab", "/api/ai/enrich-word").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/ai/save-vocab").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/lessons/*/exercises/submit").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/lessons/*/exercises/grade").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/lessons/*/exercises/attempts/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/lessons", "/api/lessons/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/lessons/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/lessons/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/vocabulary", "/api/vocabulary/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/vocabulary/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/vocabulary/**").hasRole("ADMIN")
                .requestMatchers("/api/exercises/submit/**").authenticated()
                .requestMatchers("/api/exercises/submissions/**").authenticated()
                .requestMatchers("/api/exercises/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/audio/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/media/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resources/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' https://fonts.googleapis.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com; " +
                        "img-src 'self' data: blob: https:; " +
                        "font-src 'self' https://fonts.gstatic.com data:; " +
                        "media-src 'self' blob: data: https:; " +
                        "connect-src 'self' https://api.dictionaryapi.dev http://localhost:* ws://localhost:*")
                )
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Authentication required to access this resource\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Access Denied\"}");
                })
            );

        return http.build();
    }
}
