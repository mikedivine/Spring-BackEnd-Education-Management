package com.cst438.controller;

import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.LoginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cst438.service.TokenService;

@RestController
@CrossOrigin
public class LoginController {
	
	@Autowired
	TokenService tokenService;

	@Autowired
	UserRepository userRepository;

	@GetMapping("/login")
	public LoginDTO token(Authentication authentication) {
		String name = authentication.getName();
		System.out.println("login authentication "+name);
		User user = userRepository.findByEmail(name);
		String token = tokenService.generateToken(authentication);
		return new LoginDTO(token, user.getType());
	}

}
