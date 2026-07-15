package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.AuthorPatchRequest;
import de.clouwds.library_api.dto.AuthorRequest;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Author;
import de.clouwds.library_api.repository.AuthorRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Author findAuthorById(long id) {
        return authorRepository.findById(id).orElse(null);
    }

    public void createAuthor(AuthorRequest request) {
        Author author = new Author();
        author.setName(request.name());
        authorRepository.save(author);
    }

    public void updateAuthor(AuthorRequest request, long id) {
        /*
         only update if author already exists, otherwise new author with non-server
         generated id will be created that might lead to id inconsistencies in the future
        */
        Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + id));
        author.setName(request.name());
        authorRepository.save(author);
    }

    public void patchAuthor(AuthorPatchRequest request, long id) {
        /*
         only update if author already exists, otherwise new author with non-server
         generated id will be created that might lead to id inconsistencies in the future
        */
        Author author = authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + id));

        if (request.name() != null) {
            author.setName(request.name());
        }

        authorRepository.save(author);
    }

    public void deleteAuthor(long id) {
        authorRepository.deleteById(id);
    }
}
