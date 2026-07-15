package de.clouwds.library_api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String genre;

    private int publicationYear;

    private boolean available = true;

    @Column(unique = true)
    private String isbn;

    @ManyToOne
    private Author author;

    public Book() {}

    public Book(Author author) {
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", publicationYear=" + publicationYear +
                ", available=" + available +
                ", isbn='" + isbn + '\'' +
                ", author=" + author +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Book book)) return false;
        return getPublicationYear() == book.getPublicationYear() && isAvailable() == book.isAvailable() && Objects.equals(getTitle(), book.getTitle()) && Objects.equals(getGenre(), book.getGenre()) && Objects.equals(getIsbn(), book.getIsbn()) && Objects.equals(getAuthor(), book.getAuthor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getGenre(), getPublicationYear(), isAvailable(), getIsbn(), getAuthor());
    }
}
