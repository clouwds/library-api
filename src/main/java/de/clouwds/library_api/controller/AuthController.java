package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.LoginRequest;
import de.clouwds.library_api.dto.LoginResponse;
import de.clouwds.library_api.dto.MemberRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.service.JwtService;
import de.clouwds.library_api.service.MemberDetailsService;
import de.clouwds.library_api.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final MemberService memberService;
    private final MemberDetailsService memberDetailsService;

    public AuthController(JwtService jwtService, MemberService memberService, MemberDetailsService memberDetailsService) {
        this.jwtService = jwtService;
        this.memberService = memberService;
        this.memberDetailsService = memberDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody MemberRequest request) {
        MemberResponse member = memberService.createMember(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(member.id())
                .toUri();

        return ResponseEntity.created(location).body(member);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        MemberPrincipal memberPrincipal = jwtService.authenticate(loginRequest.email(), loginRequest.password());
        String token = jwtService.generateToken(memberPrincipal);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }

    @PostMapping("/logout")
    public void logout() {
    }

}
