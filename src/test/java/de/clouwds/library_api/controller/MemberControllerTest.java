package de.clouwds.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.clouwds.library_api.config.SecurityConfig;
import de.clouwds.library_api.dto.MemberPatchRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.dto.MemberUpdateRequest;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.model.Role;
import de.clouwds.library_api.repository.MemberRepository;
import de.clouwds.library_api.service.JwtService;
import de.clouwds.library_api.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MemberRepository memberRepository;

    private static MemberPrincipal memberWithId(long id) {
        return new MemberPrincipal(id, "member" + id + "@test.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    private static MemberPrincipal librarian() {
        return new MemberPrincipal(99L, "librarian@test.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));
    }

    @Test
    void getAllMembers_asLibrarian_succeeds() throws Exception {
        when(memberService.getAllMembers()).thenReturn(List.of(new MemberResponse(1L, "Jane", "Doe", "jane@test.com", Role.MEMBER)));

        mockMvc.perform(get("/api/members").with(user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@test.com"));
    }

    @Test
    void getAllMembers_asMember_isForbidden() throws Exception {
        mockMvc.perform(get("/api/members").with(user(memberWithId(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findMemberById_own_succeeds() throws Exception {
        when(memberService.findMemberById(1L)).thenReturn(new MemberResponse(1L, "Jane", "Doe", "jane@test.com", Role.MEMBER));

        mockMvc.perform(get("/api/members/1").with(user(memberWithId(1L))))
                .andExpect(status().isOk());
    }

    @Test
    void findMemberById_otherMember_isForbidden() throws Exception {
        mockMvc.perform(get("/api/members/2").with(user(memberWithId(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMember_own_succeeds() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest("Jane", "Doe", "jane@test.com", Role.MEMBER);
        when(memberService.updateMember(any(), eq(1L))).thenReturn(new MemberResponse(1L, "Jane", "Doe", "jane@test.com", Role.MEMBER));

        mockMvc.perform(put("/api/members/1")
                        .with(user(memberWithId(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void patchMember_otherMember_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/members/2")
                        .with(user(memberWithId(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberPatchRequest("Jane", null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMember_own_succeeds() throws Exception {
        mockMvc.perform(delete("/api/members/1").with(user(memberWithId(1L))))
                .andExpect(status().isNoContent());
    }
}
