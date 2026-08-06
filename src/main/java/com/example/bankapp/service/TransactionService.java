package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public void recordDeposit(Account account, BigDecimal amount) {
        transactionRepository.save(new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        ));
    }

    public void recordWithdrawal(Account account, BigDecimal amount) {
        transactionRepository.save(new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                account
        ));
    }

    public void recordTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        transactionRepository.save(new Transaction(
                amount,
                "Transfer Out to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        ));

        transactionRepository.save(new Transaction(
                amount,
                "Transfer In from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        ));
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }
}
