package de.clouwds.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.clouwds.library_api.config.SecurityConfig;
import de.clouwds.library_api.dto.BookPatchRequest;
import de.clouwds.library_api.dto.BookRequest;
import de.clouwds.library_api.dto.BookResponse;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.repository.MemberRepository;
import de.clouwds.library_api.service.BookService;
import de.clouwds.library_api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BookService bookService;

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

    private static BookRequest validRequest() {
        return new BookRequest("1984", "Dystopian", 1949, true, "9780451524935", 1L);
    }

    @Test
    void getAllBooks_isPublic() throws Exception {
        BookResponse book = new BookResponse(1L, "1984", "Dystopian", 1949, true, "9780451524935", "George Orwell");
        when(bookService.getAllBooks(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.content[0].title").value("1984"));
    }

    @Test
    void findBookById_notFound_returns404() throws Exception {
        when(bookService.findBookById(99L)).thenThrow(new ResourceNotFoundException("Book not found - Id: 99"));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_asLibrarian_succeeds() throws Exception {
        BookResponse response = new BookResponse(5L, "1984", "Dystopian", 1949, true, "9780451524935", "George Orwell");
        when(bookService.createBook(any())).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/books/5")));
    }

    @Test
    void createBook_asMember_isForbidden() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(user(member()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBook_missingTitle_returns400() throws Exception {
        BookRequest invalid = new BookRequest(null, "Dystopian", 1949, true, "9780451524935", 1L);

        mockMvc.perform(post("/api/books")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchBook_asLibrarian_succeeds() throws Exception {
        BookResponse response = new BookResponse(1L, "Animal Farm", "Satire", 1945, true, "9780451526342", "George Orwell");
        when(bookService.patchBook(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(patch("/api/books/1")
                        .with(user(librarian()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookPatchRequest("Animal Farm", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Animal Farm"));
    }

    @Test
    void deleteBook_unauthenticated_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden());
    }
}
