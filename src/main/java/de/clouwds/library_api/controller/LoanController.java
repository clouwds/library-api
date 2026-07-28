package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.LoanRequest;
import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.exception.ConflictException;
import de.clouwds.library_api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LoanController {

    LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/loans/overdue")
    public List<LoanResponse> getOverdueLoans() {
        return loanService.getOverdueLoans();
    }

    @GetMapping("/members/{id}/loans")
    public List<LoanResponse> getLoansByMemberId(@PathVariable long id) {
        return loanService.getLoansByMemberId(id);
    }

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

    @PatchMapping("/loans/{id}/return")
    public ResponseEntity<LoanResponse> returnBook(@PathVariable long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

}
