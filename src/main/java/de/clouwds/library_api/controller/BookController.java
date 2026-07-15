package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.BookPatchRequest;
import de.clouwds.library_api.dto.BookRequest;
import de.clouwds.library_api.dto.BookResponse;
import de.clouwds.library_api.dto.PagedResponse;
import de.clouwds.library_api.model.Book;
import de.clouwds.library_api.service.BookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class BookController {

    Logger logger = LoggerFactory.getLogger(BookController.class);

    BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public ResponseEntity<PagedResponse<BookResponse>> getAllBooks(
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String genre,
            @RequestParam(name = "from", required = false) Integer publicationFrom,
            @RequestParam(name = "to", required = false) Integer publicationTo
    ) {
        logger.info("Accessed by client: " + clientId);

        Page<BookResponse> bookResponsePage = bookService.getAllBooks(authorId, genre, publicationFrom, publicationTo);

        ResponseEntity<PagedResponse<BookResponse>> responseEntity = new ResponseEntity<>(PagedResponse.from(bookResponsePage), HttpStatus.OK);
        responseEntity.getHeaders().add("X-Total-Count", String.valueOf(bookResponsePage.getTotalElements()));
        return responseEntity;
    }

    @GetMapping("/books/{id}")
    public Book findBookById(@PathVariable long id) {
        //find by id
        return bookService.findBookById(id);
    }

    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookRequest request) {
        Book book = bookService.createBook(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(book.getId())
                .toUri();

        return ResponseEntity.created(location).body(book);
    }

    @PutMapping("/books/{id}")
    public void updateBook(@Valid @RequestBody BookRequest request, @PathVariable long id) {
        //update book
        bookService.updateBook(request, id);
    }

    @PatchMapping("/books/{id}")
    public void patchBook(@Valid @RequestBody BookPatchRequest request, @PathVariable long id) {
        //patch book
        bookService.patchBook(request, id);
    }

    @DeleteMapping("/books/{id}")
    public void deleteBook(@PathVariable long id) {
        //patch book
        bookService.deleteBook(id);
    }

}
