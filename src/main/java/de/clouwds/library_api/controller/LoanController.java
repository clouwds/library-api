package de.clouwds.library_api.controller;

import de.clouwds.library_api.dto.LoanResponse;
import de.clouwds.library_api.service.LoanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
