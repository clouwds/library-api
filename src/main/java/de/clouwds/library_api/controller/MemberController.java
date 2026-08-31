package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.MemberPatchRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.dto.MemberUpdateRequest;
import de.clouwds.library_api.dto.PasswordUpdateRequest;
import de.clouwds.library_api.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/members")
    public List<MemberResponse> getAllMembers() {
        return memberService.getAllMembers();
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @GetMapping("/members/{id}")
    public MemberResponse findMemberById(@PathVariable long id) {
        return memberService.findMemberById(id);
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponse> updateMember(@Valid @RequestBody MemberUpdateRequest request, @PathVariable long id) {
        return ResponseEntity.ok(memberService.updateMember(request, id));
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @PutMapping("/members/{id}/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request, @PathVariable long id) {
        memberService.updatePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @PatchMapping("/members/{id}")
    public ResponseEntity<MemberResponse> patchMember(@Valid @RequestBody MemberPatchRequest request, @PathVariable long id) {
        return ResponseEntity.ok(memberService.patchMember(request, id));
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable long id) {
        //Todo: only if no open loans exist
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

}
