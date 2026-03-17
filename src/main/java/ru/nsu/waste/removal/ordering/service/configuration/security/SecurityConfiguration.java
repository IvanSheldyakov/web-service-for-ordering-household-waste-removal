package ru.nsu.waste.removal.ordering.service.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;

@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(Paths.REGISTRATION + "/**").permitAll()
                        .requestMatchers(Paths.COURIER_REGISTRATION + "/**").permitAll()
                        .requestMatchers(Paths.USER_LOGIN).permitAll()
                        .requestMatchers(Paths.COURIER_LOGIN).permitAll()
                        .requestMatchers(Paths.USER + "/**").hasRole("USER")
                        .requestMatchers(Paths.COURIER + "/**").hasRole("COURIER")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authenticationException) -> {
                            String path = request.getRequestURI();
                            if (path.startsWith(Paths.COURIER + "/")) {
                                response.sendRedirect(Paths.COURIER_LOGIN);
                                return;
                            }
                            if (path.startsWith(Paths.USER + "/")) {
                                response.sendRedirect(Paths.USER_LOGIN);
                                return;
                            }
                            response.sendError(401);
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }
}
