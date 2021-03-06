package com.suveen.login.controller;

import com.suveen.login.exception.AppException;
import com.suveen.login.model.Role;
import com.suveen.login.model.RoleName;
import com.suveen.login.model.User;
import com.suveen.login.payload.ApiResponse;
import com.suveen.login.payload.JwtAuthenticationResponse;
import com.suveen.login.payload.LoginRequest;
import com.suveen.login.payload.SignUpRequest;
import com.suveen.login.repository.RoleRepository;
import com.suveen.login.repository.UserRepository;
import com.suveen.login.security.JwtTokenProvider;
import java.net.URI;
import java.util.Collections;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired AuthenticationManager authenticationManager;

  @Autowired UserRepository userRepository;

  @Autowired RoleRepository roleRepository;

  @Autowired PasswordEncoder passwordEncoder;

  @Autowired JwtTokenProvider tokenProvider;

  @PostMapping("/signin")
  public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    String jwt = tokenProvider.generateToken(authentication);
    return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
  }

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return new ResponseEntity(
          new ApiResponse(false, "Username is already taken!"), HttpStatus.BAD_REQUEST);
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return new ResponseEntity(
          new ApiResponse(false, "Email Address already in use!"), HttpStatus.BAD_REQUEST);
    }

    // Creating user's account
    User user =
        new User(
            signUpRequest.getName(),
            signUpRequest.getUsername(),
            signUpRequest.getEmail(),
            signUpRequest.getPassword());

    user.setPassword(passwordEncoder.encode(user.getPassword()));

    Role userRole =
        roleRepository
            .findByName(RoleName.ROLE_USER)
            .orElseThrow(() -> new AppException("User Role not set."));

    user.setRoles(Collections.singleton(userRole));

    User result = userRepository.save(user);

    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/users/{username}")
            .buildAndExpand(result.getUsername())
            .toUri();

    return ResponseEntity.created(location)
        .body(new ApiResponse(true, "User registered successfully"));
  }
}
