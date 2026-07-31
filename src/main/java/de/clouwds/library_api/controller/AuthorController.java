package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.AuthorPatchRequest;
import de.clouwds.library_api.dto.AuthorRequest;
import de.clouwds.library_api.dto.AuthorResponse;
import de.clouwds.library_api.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/authors")
    public List<AuthorResponse> getAllAuthors() {
        return authorService.getAllAuthors();
    }

    @GetMapping("/authors/{id}")
    public AuthorResponse findAuthorById(@PathVariable long id) {
        return authorService.findAuthorById(id);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/authors")
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        AuthorResponse author = authorService.createAuthor(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(author.id())
                .toUri();

        return ResponseEntity.created(location).body(author);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PutMapping("/authors/{id}")
    public ResponseEntity<AuthorResponse> updateAuthor(@Valid @RequestBody AuthorRequest request, @PathVariable long id) {
        return ResponseEntity.ok(authorService.updateAuthor(request, id));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PatchMapping("/authors/{id}")
    public ResponseEntity<AuthorResponse> patchAuthor(@Valid @RequestBody AuthorPatchRequest request, @PathVariable long id) {
        return ResponseEntity.ok(authorService.patchAuthor(request, id));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/authors/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

}
