package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.TokenResponse;
import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.model.RefreshToken;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MemberDetailsService memberDetailsService;
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;

    public AuthService(JwtService jwtService, RefreshTokenService refreshTokenService, MemberDetailsService memberDetailsService, MemberService memberService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.memberDetailsService = memberDetailsService;
        this.memberService = memberService;
        this.authenticationManager = authenticationManager;
    }

    public TokenResponse login (String email, String password) {
        MemberPrincipal principal = getAuthenticatedPrincipal(email, password);
        Member member = memberService.getMemberById(principal.getId());

        String refreshToken = refreshTokenService.generateRefreshToken();
        refreshTokenService.persistRefreshToken(refreshToken, member);

        return buildTokenResponse(principal, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenString) {
        RefreshToken oldToken = refreshTokenService.validateToken(refreshTokenString);
        refreshTokenService.markUsed(oldToken);

        String newToken = refreshTokenService.generateRefreshToken();
        refreshTokenService.persistRefreshToken(newToken, oldToken.getMember());

        MemberPrincipal principal = getAuthenticatedPrincipal(oldToken);
        return buildTokenResponse(principal, newToken);
    }

    private MemberPrincipal getAuthenticatedPrincipal(String email, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
        Authentication auth = authenticationManager.authenticate(token);
        return (MemberPrincipal) auth.getPrincipal();
    }

    private MemberPrincipal getAuthenticatedPrincipal(RefreshToken refreshToken) {
        Member member = refreshToken.getMember();
        return (MemberPrincipal) memberDetailsService.loadUserByUsername(member.getEmail());
    }

    @NonNull
    private TokenResponse buildTokenResponse(MemberPrincipal memberPrincipal, String refreshToken) {
        String accessToken = jwtService.generateToken(memberPrincipal);
        return new TokenResponse(accessToken, "Bearer", refreshToken);
    }

}
