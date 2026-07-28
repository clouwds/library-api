package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.LoanRequest;
import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.exception.ConflictException;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Book;
import de.clouwds.library_api.model.Loan;
import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.repository.BookRepository;
import de.clouwds.library_api.repository.LoanRepository;
import de.clouwds.library_api.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    LoanRepository loanRepository;
    BookRepository bookRepository;
    MemberRepository memberRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    private Loan createLoan(Book book, Member member) {
        Loan loan = new Loan(member, book, LocalDate.now(), LocalDate.now().plusWeeks(2));
        return loanRepository.save(loan);
    }

    private LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getTitle(),
                loan.getMember().getFirstName() + " " + loan.getMember().getLastName(),
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getReturnDate()
        );
    }

    public List<LoanResponse> getOverdueLoans() {
        return loanRepository.findByDueDateBeforeAndReturnDateIsNull(LocalDate.now())
                .stream()
                .map(this::toLoanResponse)
                .toList();
    }

    public List<LoanResponse> getLoansByMemberId(long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found - Id: " + memberId);
        }

        return loanRepository.findByMemberId(memberId)
                .stream()
                .map(this::toLoanResponse)
                .toList();
    }

    // method exists as example for pessimistic locking scenario, together with@Lock on findByIdForUpdate in BookRepository
    @Transactional
    public LoanResponse borrowBookPessimistic(LoanRequest loanRequest) {
        Long memberId = loanRequest.memberId();
        Long bookId = loanRequest.bookId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + memberId));
        Book book = bookRepository.findByIdForUpdate(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found - Id: " + bookId));

        if(!book.isAvailable()) {
            throw new ConflictException("Book is not available - Id: "  + bookId);
        }

        book.setAvailable(false);
        return toLoanResponse(createLoan(book, member));
    }

    @Transactional
    public LoanResponse borrowBookOptimistic(LoanRequest loanRequest) {
        Long memberId = loanRequest.memberId();
        Long bookId = loanRequest.bookId();
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found - Id: " + memberId));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found - Id: " + bookId));

        if(!book.isAvailable()) {
            throw new ConflictException("Book is not available - Id: "  + bookId);
        }

        book.setAvailable(false);
        return toLoanResponse(createLoan(book, member));
    }


    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found - Id: " + loanId));

        if (loan.getReturnDate() != null) {
            throw new ConflictException("Loan already returned - Id: " + loanId);
        }

        loan.setReturnDate(LocalDate.now());

        Book book = loan.getBook();
        book.setAvailable(true);
        bookRepository.save(book);

        return toLoanResponse(loanRepository.save(loan));
    }

}
