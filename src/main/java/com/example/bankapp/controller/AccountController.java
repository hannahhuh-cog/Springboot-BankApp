package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.security.CustomUserDetails;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class AccountController {

    @Autowired
    private AccountService accountService;

    private Account currentAccount(CustomUserDetails userDetails) {
        return accountService.findAccountByUsername(userDetails.getUsername());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("account", currentAccount(userDetails));
        return "dashboard";
    }

    @PostMapping("/deposit")
    public String deposit(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam BigDecimal amount) {
        accountService.deposit(currentAccount(userDetails), amount);
        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @RequestParam BigDecimal amount,
                           Model model) {
        Account account = currentAccount(userDetails);
        try {
            accountService.withdraw(account, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            return "dashboard";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactionHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Account account = currentAccount(userDetails);
        model.addAttribute("transactions", accountService.getTransactionHistory(account));
        return "transactions";
    }

    @PostMapping("/transfer")
    public String transferAmount(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam String toUsername,
                                 @RequestParam BigDecimal amount,
                                 Model model) {
        Account fromAccount = currentAccount(userDetails);
        try {
            accountService.transferAmount(fromAccount, toUsername, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", fromAccount);
            return "dashboard";
        }
        return "redirect:/dashboard";
    }
}
