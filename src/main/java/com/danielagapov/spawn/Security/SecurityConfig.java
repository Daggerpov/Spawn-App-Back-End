package com.danielagapov.spawn.Security;

//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
//import org.springframework.security.web.authentication.HttpStatusEntryPoint;
//
//import java.io.IOException;
//
//import static org.springframework.http.HttpStatus.UNAUTHORIZED;

//@Configuration
//@EnableWebSecurity
public class SecurityConfig {

    //@Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .authorizeHttpRequests(authorize -> authorize
//                .anyRequest().authenticated()
//            )
//            .exceptionHandling(e -> e
//                    .authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))
//            )
//            .oauth2Login(oauth2 -> {
//                oauth2.successHandler(new AuthenticationSuccessHandler() {
//                    @Override
//                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
//                        response.sendRedirect("/api/v1/oauth/google/sign-in");
//                    }
//                });
//            })
//        ;
//        return http.build();
//    }

}
