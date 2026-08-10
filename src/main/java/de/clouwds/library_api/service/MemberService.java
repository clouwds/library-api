package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.*;
import de.clouwds.library_api.exception.ConflictException;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll().stream().map(this::toMemberResponse).toList();
    }

    public MemberResponse findMemberById(long id) {
        return toMemberResponse(getMemberOrThrow(id));
    }

    public Member getMemberById(long id) {
        return getMemberOrThrow(id);
    }

    public MemberResponse findMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Member not found - Email: " + email));
        return toMemberResponse(member);
    }

    public MemberResponse createMember(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ConflictException("Member with email " + request.email() + " already exists");
        }

        Member member = new Member(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role());
        return toMemberResponse(memberRepository.save(member));
    }

    public MemberResponse updateMember(MemberUpdateRequest request, long id) {
        Member member = getMemberOrThrow(id);
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setRole(request.role());
        return toMemberResponse(memberRepository.save(member));
    }

    public void updatePassword(long id, PasswordUpdateRequest request) {
        Member member = getMemberOrThrow(id);
        member.setPassword(passwordEncoder.encode(request.password()));
        memberRepository.save(member);
    }

    public MemberResponse patchMember(MemberPatchRequest request, long id) {
        Member member = getMemberOrThrow(id);

        if (request.firstName() != null) {
            member.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            member.setLastName(request.lastName());
        }
        if (request.email() != null) {
            member.setEmail(request.email());
        }
        if (request.password() != null) {
            member.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            member.setRole(request.role());
        }

        return toMemberResponse(memberRepository.save(member));
    }

    public void deleteMember(long id) {
        if (!memberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Member not found - Id: " + id);
        }
        memberRepository.deleteById(id);
    }

    private Member getMemberOrThrow(long id) {
        return memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + id));
    }

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(member.getId(), member.getFirstName(), member.getLastName(), member.getEmail(), member.getRole());
    }
}
