package de.clouwds.library_api.repository;

import de.clouwds.library_api.model.Book;
import de.clouwds.library_api.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {


    List<Loan> findByReturnDateIsNull();

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByDueDateBeforeAndReturnDateIsNull(LocalDate date);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId AND l.returnDate IS NULL")
    Long findCurrentlyBorrowedCountByMemberId(Long memberId);

    @Query("SELECT l.book.title, COUNT(l) FROM Loan l GROUP BY l.book.id, l.book.title ORDER BY COUNT(l) DESC")
    List<Object[]> findMostBorrowedBooks();

    @Query("SELECT DISTINCT l.book FROM Loan l WHERE l.book.author.id = :authorId AND l.returnDate IS NULL")
    List<Book> findBooksCurrentlyOnLoanByAuthor(Long authorId);

}

