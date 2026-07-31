package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.LoanRequest;
import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.exception.ConflictException;
import de.clouwds.library_api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @GetMapping("/loans/overdue")
    public List<LoanResponse> getOverdueLoans() {
        return loanService.getOverdueLoans();
    }

    @PreAuthorize("#id == authentication.principal.id || hasRole('LIBRARIAN')")
    @GetMapping("/members/{id}/loans")
    public List<LoanResponse> getLoansByMemberId(@PathVariable long id) {
        return loanService.getLoansByMemberId(id);
    }

    @PreAuthorize("#request.memberId() == authentication.principal.id || hasRole('LIBRARIAN')")
    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody LoanRequest request) {
        try {
            LoanResponse loan = loanService.borrowBookOptimistic(request);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(loan.id())
                    .toUri();

            return ResponseEntity.created(location).body(loan);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("Book is not available - Id: "  + request.bookId());
        }
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PatchMapping("/loans/{id}/return")
    public ResponseEntity<LoanResponse> returnBook(@PathVariable long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

}
