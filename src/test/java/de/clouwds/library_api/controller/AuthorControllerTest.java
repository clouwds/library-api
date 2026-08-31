package de.clouwds.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.clouwds.library_api.config.SecurityConfig;
import de.clouwds.library_api.dto.AuthorRequest;
import de.clouwds.library_api.dto.AuthorResponse;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.repository.MemberRepository;
import de.clouwds.library_api.service.AuthorService;
import de.clouwds.library_api.service.JwtService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorController.class)
@Import(SecurityConfig.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MemberRepository memberRepository;

    private static MemberPrincipal member() {
        return new MemberPrincipal(1L, "member@test.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    private static MemberPrincipal librarian() {
        return new MemberPrincipal(2L, "librarian@test.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));
    }

    @Test
    void getAllAuthors_isPublic() throws Exception {
        when(authorService.getAllAuthors()).thenReturn(List.of(new AuthorResponse(1L, "Ursula K. Le Guin")));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ursula K. Le Guin"));
    }

    @Test
    void findAuthorById_notFound_returns404() throws Exception {
        when(authorService.findAuthorById(99L)).thenThrow(new ResourceNotFoundException("Author not found - Id: 99"));

        mockMvc.perform(get("/api/authors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAuthor_asLibrarian_succeeds() throws Exception {
        when(authorService.createAuthor(any())).thenReturn(new AuthorResponse(5L, "New Author"));

        mockMvc.perform(post("/api/authors")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthorRequest("New Author"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/authors/5")));
    }

    @Test
    void createAuthor_asMember_isForbidden() throws Exception {
        mockMvc.perform(post("/api/authors")
                        .with(user(member()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthorRequest("New Author"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAuthor_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/authors")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthorRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAuthor_unauthenticated_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/authors/1"))
                .andExpect(status().isForbidden());
    }
}
