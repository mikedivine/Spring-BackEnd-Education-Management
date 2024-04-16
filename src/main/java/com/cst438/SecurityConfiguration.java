package com.cst438;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfiguration {
    
    private final RsaKeyProperties rsaKeys;
    
    public SecurityConfiguration(RsaKeyProperties rsaKeys) {
    	this.rsaKeys = rsaKeys;
    }
    
    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();  //dW
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
    		HttpSecurity http,
    		HandlerMappingIntrospector introspector) throws Exception {

    	http
		    .headers(headers ->
         		    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))  // h2 console needs this
			.csrf((csrf)-> csrf.disable())
			.cors(withDefaults())
			.authorizeHttpRequests( auth -> auth
					.requestMatchers(
							// list of unsecured URLs for h2 console, and for  things needed in assignment 8 for AWS
							AntPathRequestMatcher.antMatcher("/h2-console/**"),
							AntPathRequestMatcher.antMatcher("/check"),
							AntPathRequestMatcher.antMatcher("/fail"),
							AntPathRequestMatcher.antMatcher("/ipaddr"),
							AntPathRequestMatcher.antMatcher("/favicon.ico"),
							AntPathRequestMatcher.antMatcher("/"),
							AntPathRequestMatcher.antMatcher("/index.html"),
							AntPathRequestMatcher.antMatcher("/manifest.json"),
							AntPathRequestMatcher.antMatcher("/*.png"),
							AntPathRequestMatcher.antMatcher("/static/**")
					).permitAll()
					.anyRequest().authenticated()
					)
			.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(withDefaults());
          
        return http.build();
    }
    
    @Bean
    JwtDecoder jwtDecoder() {
    	return NimbusJwtDecoder.withPublicKey(rsaKeys.publicKey()).build();
    }
    
    @Bean
    JwtEncoder jwtEncoder() {
    	JWK jwk = new RSAKey.Builder(rsaKeys.publicKey()).privateKey(rsaKeys.privateKey()).build();
    	JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
    	return new NimbusJwtEncoder(jwks);
    }
}