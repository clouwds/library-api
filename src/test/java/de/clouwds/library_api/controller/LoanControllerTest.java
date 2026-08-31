package de.clouwds.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.clouwds.library_api.config.SecurityConfig;
import de.clouwds.library_api.dto.LoanRequest;
import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.repository.MemberRepository;
import de.clouwds.library_api.service.JwtService;
import de.clouwds.library_api.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(SecurityConfig.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private LoanService loanService;

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

    private static LoanResponse loanResponse() {
        return new LoanResponse(1L, "1984", "Test Member", LocalDate.now(), LocalDate.now().plusWeeks(2), null);
    }

    @Test
    void getOverdueLoans_asLibrarian_succeeds() throws Exception {
        when(loanService.getOverdueLoans()).thenReturn(List.of(loanResponse()));

        mockMvc.perform(get("/api/loans/overdue").with(user(librarian())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("1984"));
    }

    @Test
    void getOverdueLoans_asMember_isForbidden() throws Exception {
        mockMvc.perform(get("/api/loans/overdue").with(user(memberWithId(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLoansByMemberId_ownData_succeeds() throws Exception {
        when(loanService.getLoansByMemberId(1L)).thenReturn(List.of(loanResponse()));

        mockMvc.perform(get("/api/members/1/loans").with(user(memberWithId(1L))))
                .andExpect(status().isOk());
    }

    @Test
    void getLoansByMemberId_otherMembersData_isForbidden() throws Exception {
        mockMvc.perform(get("/api/members/2/loans").with(user(memberWithId(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLoan_ownRequest_succeeds() throws Exception {
        when(loanService.borrowBookOptimistic(any())).thenReturn(loanResponse());

        mockMvc.perform(post("/api/loans")
                        .with(user(memberWithId(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequest(1L, 10L))))
                .andExpect(status().isCreated());
    }

    @Test
    void createLoan_forOtherMember_isForbidden() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(user(memberWithId(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequest(2L, 10L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLoan_bookAlreadyBorrowed_returns409() throws Exception {
        when(loanService.borrowBookOptimistic(any())).thenThrow(new ObjectOptimisticLockingFailureException(Object.class, 10L));

        mockMvc.perform(post("/api/loans")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequest(1L, 10L))))
                .andExpect(status().isConflict());
    }

    @Test
    void returnBook_asLibrarian_succeeds() throws Exception {
        when(loanService.returnBook(1L)).thenReturn(loanResponse());

        mockMvc.perform(patch("/api/loans/1/return").with(user(librarian())))
                .andExpect(status().isOk());
    }

    @Test
    void returnBook_asMember_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/loans/1/return").with(user(memberWithId(1L))))
                .andExpect(status().isForbidden());
    }
}
