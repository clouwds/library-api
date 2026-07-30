package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.MemberPatchRequest;
import de.clouwds.library_api.dto.MemberRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.dto.MemberUpdateRequest;
import de.clouwds.library_api.dto.PasswordUpdateRequest;
import de.clouwds.library_api.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MemberController {

    private MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public List<MemberResponse> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/members/{id}")
    public MemberResponse findMemberById(@PathVariable long id) {
        return memberService.findMemberById(id);
    }

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberRequest request) {
        MemberResponse member = memberService.createMember(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(member.id())
                .toUri();

        return ResponseEntity.created(location).body(member);
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponse> updateMember(@Valid @RequestBody MemberUpdateRequest request, @PathVariable long id) {
        return ResponseEntity.ok(memberService.updateMember(request, id));
    }

    @PutMapping("/members/{id}/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request, @PathVariable long id) {
        memberService.updatePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/members/{id}")
    public ResponseEntity<MemberResponse> patchMember(@Valid @RequestBody MemberPatchRequest request, @PathVariable long id) {
        return ResponseEntity.ok(memberService.patchMember(request, id));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

}
