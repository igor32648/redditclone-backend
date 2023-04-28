package com.igor32648.redditclone.service;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.igor32648.redditclone.dto.AuthenticationResponse;
import com.igor32648.redditclone.dto.LoginRequest;
import com.igor32648.redditclone.dto.RefreshTokenRequest;
import com.igor32648.redditclone.dto.RegisterRequest;
import com.igor32648.redditclone.exception.RedditCloneException;
import com.igor32648.redditclone.model.NotificationEmail;
import com.igor32648.redditclone.model.Profile;
import com.igor32648.redditclone.model.VerificationToken;
import com.igor32648.redditclone.repository.ProfileRepository;
import com.igor32648.redditclone.repository.VerificationTokenRepository;
import com.igor32648.redditclone.security.JwtProvider;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public void signup(RegisterRequest registerRequest) {
        Profile profile = new Profile();
        profile.setProfilename(registerRequest.getProfilename());
        profile.setEmail(registerRequest.getEmail());
        profile.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        profile.setCreatedInstant(Instant.now());
        profile.setEnabled(false);

        profileRepository.save(profile);

        String token = generateVerificationToken(profile);
        mailService.sendMail(new NotificationEmail("Please Activate your Account",
                profile.getEmail(), "Thank you for signing up to Spring Reddit, " +
                        "please click on the below url to activate your account : " +
                        "http://localhost:8080/api/auth/accountVerification/" + token));
    }

    @Transactional(readOnly = true)
    public Profile getCurrentprofile() {
        Jwt principal = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return profileRepository.findByProfilename(principal.getSubject())
                .orElseThrow(
                        () -> new UsernameNotFoundException("profile name not found - " + principal.getSubject()));
    }

    public void verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        fetchProfileAndEnable(verificationToken.orElseThrow(() -> new RedditCloneException("Invalid Token")));
    }

    private void fetchProfileAndEnable(VerificationToken verificationToken) {
        String profilename = verificationToken.getProfile().getProfilename();
        Profile profile = profileRepository.findByProfilename(profilename)
                .orElseThrow(() -> new RedditCloneException("profile not found with name - " + profilename));
        profile.setEnabled(true);
        profileRepository.save(profile);
    }

    private String generateVerificationToken(Profile profile) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setProfile(profile);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public AuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authenticate = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getProfilename(),
                        loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String token = jwtProvider.generateToken(authenticate);
        return AuthenticationResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshTokenService.generateRefreshToken().getToken())
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .profilename(loginRequest.getProfilename())
                .build();
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
        String token = jwtProvider.generateTokenWithUserName(refreshTokenRequest.getProfilename());
        return AuthenticationResponse.builder()
                .authenticationToken(token)
                .refreshToken(refreshTokenRequest.getRefreshToken())
                .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
                .profilename(refreshTokenRequest.getProfilename())
                .build();
    }

    public boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
    }
}