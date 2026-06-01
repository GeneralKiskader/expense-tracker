package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Игнорируем проверку безопасности для статических ресурсов
        return web -> web.ignoring()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/my-h2-admin/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Открытые пути
                        .requestMatchers("/register", "/login", "/").permitAll()
                        // Все остальные запросы требуют авторизации
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // Перенаправляем на Дашборд после успешного входа
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        // Разрешаем любой запрос на /logout (наши формы используют POST, что безопасно)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .userDetailsService(customUserDetailsService)
                // Отключаем frameOptions для H2 консоли
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                // CSRF отключаем только для консоли базы данных
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/my-h2-admin/**")
                );

        return http.build();
    }
}