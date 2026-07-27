package de.clouwds.library_api.service;

import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.exception.ResourceNotFoundException;
import de.clouwds.library_api.model.Book;
import de.clouwds.library_api.model.Loan;
import de.clouwds.library_api.repository.BookRepository;
import de.clouwds.library_api.repository.LoanRepository;
import de.clouwds.library_api.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
}
