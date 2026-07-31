package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.BookPatchRequest;
import de.clouwds.library_api.dto.BookRequest;
import de.clouwds.library_api.dto.BookResponse;
import de.clouwds.library_api.dto.PagedResponse;
import de.clouwds.library_api.service.BookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class BookController {

    Logger logger = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public ResponseEntity<PagedResponse<BookResponse>> getAllBooks(
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String genre,
            @RequestParam(name = "from", required = false) Integer publicationFrom,
            @RequestParam(name = "to", required = false) Integer publicationTo,
            @RequestParam(name = "sort", required = false) String sortParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        logger.info("Accessed by client: " + clientId);

        Page<BookResponse> bookResponsePage = bookService.getAllBooks(authorId, genre, publicationFrom, publicationTo, sortParams, page, size);

        ResponseEntity<PagedResponse<BookResponse>> responseEntity = new ResponseEntity<>(PagedResponse.from(bookResponsePage), HttpStatus.OK);
        responseEntity.getHeaders().add("X-Total-Count", String.valueOf(bookResponsePage.getTotalElements()));
        return responseEntity;
    }

    @GetMapping("/books/{id}")
    public BookResponse findBookById(@PathVariable long id) {
        return bookService.findBookById(id);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/books")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse book = bookService.createBook(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(book.id())
                .toUri();

        return ResponseEntity.created(location).body(book);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PutMapping("/books/{id}")
    public ResponseEntity<BookResponse> updateBook(@Valid @RequestBody BookRequest request, @PathVariable long id) {
        return ResponseEntity.ok(bookService.updateBook(request, id));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PatchMapping("/books/{id}")
    public ResponseEntity<BookResponse> patchBook(@Valid @RequestBody BookPatchRequest request, @PathVariable long id) {
        return ResponseEntity.ok(bookService.patchBook(request, id));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

}
