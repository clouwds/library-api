package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.MemberPatchRequest;
import de.clouwds.library_api.dto.MemberRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(member.getId(), member.getFirstName(), member.getLastName());
    }

    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll().stream().map(this::toMemberResponse).toList();
    }

    public MemberResponse findMemberById(long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + id));
        return toMemberResponse(member);
    }

    public MemberResponse createMember(MemberRequest request) {
        Member member = new Member();
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        return toMemberResponse(memberRepository.save(member));
    }

    public MemberResponse updateMember(MemberRequest request, long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + id));
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        return toMemberResponse(memberRepository.save(member));
    }

    public MemberResponse patchMember(MemberPatchRequest request, long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + id));

        if (request.firstName() != null) {
            member.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            member.setLastName(request.lastName());
        }

        return toMemberResponse(memberRepository.save(member));
    }

    public void deleteMember(long id) {
        if (!memberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Member not found - Id: " + id);
        }
        memberRepository.deleteById(id);
    }
}
