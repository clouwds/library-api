package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.AuthorPatchRequest;
import de.clouwds.library_api.dto.AuthorRequest;
import de.clouwds.library_api.dto.AuthorResponse;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Author;
import de.clouwds.library_api.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    private AuthorResponse toAuthorResponse(Author author) {
        return new AuthorResponse(author.getId(), author.getName());
    }

    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAll().stream().map(this::toAuthorResponse).toList();
    }

    public AuthorResponse findAuthorById(long id) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + id));
        return toAuthorResponse(author);
    }

    public AuthorResponse createAuthor(AuthorRequest request) {
        Author author = new Author();
        author.setName(request.name());
        return toAuthorResponse(authorRepository.save(author));
    }

    public AuthorResponse updateAuthor(AuthorRequest request, long id) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + id));
        author.setName(request.name());
        return toAuthorResponse(authorRepository.save(author));
    }

    public AuthorResponse patchAuthor(AuthorPatchRequest request, long id) {
        Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + id));

        if (request.name() != null) {
            author.setName(request.name());
        }

        return toAuthorResponse(authorRepository.save(author));
    }

    public void deleteAuthor(long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author not found - Id: " + id);
        }
        authorRepository.deleteById(id);
    }
}
