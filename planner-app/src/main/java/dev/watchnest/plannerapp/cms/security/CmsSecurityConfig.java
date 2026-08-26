package dev.watchnest.plannerapp.cms.security;

import dev.watchnest.plannerapp.cms.auth.CmsSessionAuthenticationFilter;
import dev.watchnest.plannerapp.cms.auth.CmsSessionStore;
import dev.watchnest.plannerapp.security.JsonAccessDeniedHandler;
import dev.watchnest.plannerapp.security.JsonAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
public class CmsSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain cmsSecurityFilterChain(
            HttpSecurity http,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler,
            CmsSessionStore cmsSessionStore,
            @Value("${watchnest.session.cookie.secure:false}") boolean cookieSecure
    ) throws Exception {
        CsrfTokenRepository csrfTokenRepository = new CmsCookieCsrfTokenRepository(cookieSecure);

        http
                .securityMatcher("/cms/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CmsCsrfTokenRequestHandler())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/cms/api/v1/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cms/api/v1/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cms/api/v1/logout").permitAll()
                        .requestMatchers("/cms/api/v1/me").authenticated()
                        .requestMatchers("/cms/api/v1/titles", "/cms/api/v1/titles/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(
                        new CmsSessionAuthenticationFilter(cmsSessionStore),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
