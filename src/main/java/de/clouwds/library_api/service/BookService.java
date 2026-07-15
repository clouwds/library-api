package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.BookPatchRequest;
import de.clouwds.library_api.dto.BookRequest;
import de.clouwds.library_api.dto.BookResponse;
import de.clouwds.library_api.exception.ConflictException;
import de.clouwds.library_api.exception.InvalidRequestException;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Author;
import de.clouwds.library_api.model.Book;
import de.clouwds.library_api.repository.AuthorRepository;
import de.clouwds.library_api.repository.BookRepository;
import de.clouwds.library_api.specification.BookSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BookService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "title", "title",
            "genre", "genre",
            "publicationYear", "publicationYear",
            "author", "author.name"
    );

    BookRepository bookRepository;
    AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    public Page<BookResponse> getAllBooks(Long authorId, String genre, Integer publicationFrom, Integer publicationTo, String sortParams) {
        Specification<Book> specification = Specification.unrestricted();

        if(authorId != null) {
            specification = specification.and(BookSpecifications.hasAuthorId(authorId));
        }
        if(genre != null) {
            specification = specification.and(BookSpecifications.hasGenre(genre));
        }

        //TODO: maybe fix, if only one is provided set the other to same value
        if(publicationFrom != null && publicationTo != null) {
            specification = specification.and(BookSpecifications.publicationYearBetween(publicationFrom, publicationTo));
        }

        Pageable pageable = PageRequest.of(0, 10, parseSort(sortParams));

        Page<Book> bookPage = bookRepository.findAll(specification, pageable);
        return bookPage.map(book -> new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getGenre(),
                book.getPublicationYear(),
                book.isAvailable(),
                book.getIsbn(),
                book.getAuthor().getName()
        ));
    }

    private Sort parseSort(String sortParams) {
        if (sortParams == null || sortParams.isBlank()) {
            return Sort.unsorted();
        }

        String[] sortParamsSplit = sortParams.split(",");
        String propertyName = sortParamsSplit[0];

        if (!SORTABLE_FIELDS.containsKey(propertyName)) {
            throw new InvalidRequestException("Cannot sort by '" + propertyName + "' - allowed fields: " + SORTABLE_FIELDS.keySet());
        }

        Sort.Direction sortDirection = Sort.DEFAULT_DIRECTION;
        if (sortParamsSplit.length > 1) {
            sortDirection = Sort.Direction.fromString(sortParamsSplit[1]);
        }

        return Sort.by(sortDirection, SORTABLE_FIELDS.get(propertyName));
    }

    public Book findBookById(long id) {
        return bookRepository.findById(id).orElse(null);
    }

    boolean existsByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn) != null;
    }

    /*
    TODO:
    - pagination (page, size)
    */
    public Book createBook(BookRequest request) {
        if(existsByIsbn(request.isbn())) {
            throw new ConflictException("Book with ISBN " + request.isbn() + " already exists");
        }
        Author author = authorRepository.findById(request.authorId()).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + request.authorId()));
        Book book = new Book(author);
        book.setTitle(request.title());
        book.setGenre(request.genre());
        book.setPublicationYear(request.publicationYear());
        book.setIsbn(request.isbn());
        book.setAvailable(request.available());
        return bookRepository.save(book);
    }

    public void updateBook(BookRequest request, long id) {
        /*
         only update if book already exists, otherwise new book with non-server
         generated id will be created that might lead to id inconsistencies in the future
        */
        Book book = bookRepository.findById(id).orElse(null);

        if (book != null) {
            Author author = authorRepository.findById(request.authorId()).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + request.authorId()));
            book.setTitle(request.title());
            book.setGenre(request.genre());
            book.setPublicationYear(request.publicationYear());
            book.setIsbn(request.isbn());
            book.setAvailable(request.available());
            book.setAuthor(author);
            bookRepository.save(book);
        }
    }

    public void patchBook(BookPatchRequest request, long id) {
        /*
         only update if book already exists, otherwise new book with non-server
         generated id will be created that might lead to id inconsistencies in the future
        */
        Book book = bookRepository.findById(id).orElse(null);

        if (book != null) {
            if (request.title() != null) {
                book.setTitle(request.title());
            }
            if (request.genre() != null) {
                book.setGenre(request.genre());
            }
            if (request.publicationYear() != null) {
                book.setPublicationYear(request.publicationYear());
            }
            if (request.available() != null) {
                book.setAvailable(request.available());
            }
            if (request.isbn() != null) {
                book.setIsbn(request.isbn());
            }
            if (request.authorId() != null) {
                Author author = authorRepository.findById(request.authorId()).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + request.authorId()));
                book.setAuthor(author);
            }
            bookRepository.save(book);
        }
    }

    public void deleteBook(long id) {
        bookRepository.deleteById(id);
    }
}
