package de.clouwds.library_api.specification;

import de.clouwds.library_api.model.Book;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecifications {

    public static Specification<Book> hasAuthorId(Long authorId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Book> hasGenre(String genre) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("genre"), genre);
    }

    public static Specification<Book> publicationYearBetween(int from, int to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("publicationYear"), from, to);
    }

}
