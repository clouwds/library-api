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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BookService {

    private static final String CACHE_CONDITIONS = "#authorId == null && #genre == null && #publicationFrom == null && #publicationTo == null && #sortParams == null && #page == 0 && #size == 10";

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

    @Cacheable(value = "books", condition = CACHE_CONDITIONS)
    public Page<BookResponse> getAllBooks(Long authorId, String genre, Integer publicationFrom, Integer publicationTo, String sortParams, int page, int size) {
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

        Pageable pageable = PageRequest.of(page, size, parseSort(sortParams));

        Page<Book> bookPage = bookRepository.findAll(specification, pageable);
        return bookPage.map(this::toBookResponse);
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

    private BookResponse toBookResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getGenre(),
                book.getPublicationYear(),
                book.isAvailable(),
                book.getIsbn(),
                book.getAuthor().getName()
        );
    }

    public BookResponse findBookById(long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found - Id: " + id));
        return toBookResponse(book);
    }

    boolean existsByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn) != null;
    }

    @CacheEvict(value = "books",  allEntries = true)
    public BookResponse createBook(BookRequest request) {
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
        return toBookResponse(bookRepository.save(book));
    }

    @CacheEvict(value = "books",  allEntries = true)
    public BookResponse updateBook(BookRequest request, long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found - Id: " + id));

        Author author = authorRepository.findById(request.authorId()).orElseThrow(() -> new ResourceNotFoundException("Author not found - Id: " + request.authorId()));
        book.setTitle(request.title());
        book.setGenre(request.genre());
        book.setPublicationYear(request.publicationYear());
        book.setIsbn(request.isbn());
        book.setAvailable(request.available());
        book.setAuthor(author);
        return toBookResponse(bookRepository.save(book));
    }

    @CacheEvict(value = "books",  allEntries = true)
    public BookResponse patchBook(BookPatchRequest request, long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found - Id: " + id));

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
        return toBookResponse(bookRepository.save(book));
    }

    @CacheEvict(value = "books",  allEntries = true)
    public void deleteBook(long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found - Id: " + id);
        }
        bookRepository.deleteById(id);
    }
}
