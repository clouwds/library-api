package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.AuthorPatchRequest;
import de.clouwds.library_api.dto.AuthorRequest;
import de.clouwds.library_api.model.Author;
import de.clouwds.library_api.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthorController {

    private AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/authors")
    public List<Author> getAllAuthors() {
        //Return all authors
        return authorService.getAllAuthors();
    }

    @GetMapping("/authors/{id}")
    public Author findAuthorById(long id) {
        //find by id
        return authorService.findAuthorById(id);
    }

    @PostMapping("/authors")
    public void createAuthor(@Valid @RequestBody AuthorRequest request) {
        //create author
        authorService.createAuthor(request);
    }

    @PutMapping("/authors/{id}")
    public void updateAuthor(@Valid @RequestBody AuthorRequest request, long id) {
        //update author
        authorService.updateAuthor(request, id);
    }

    @PatchMapping("/authors/{id}")
    public void patchAuthor(@Valid @RequestBody AuthorPatchRequest request, @PathVariable long id) {
        //patch author
        authorService.patchAuthor(request, id);
    }

    @DeleteMapping("/authors/{id}")
    public void deleteAuthor(long id) {
        //patch author
        authorService.deleteAuthor(id);
    }



}
