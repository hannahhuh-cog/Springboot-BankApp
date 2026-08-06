package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account(String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setBalance(BigDecimal.ZERO);
        return account;
    }

    @Test
    void recordDepositSavesDepositRow() {
        Account account = account("alice");

        transactionService.recordDeposit(account, new BigDecimal("25.00"));

        Transaction saved = capture(1).get(0);
        assertThat(saved.getType()).isEqualTo("Deposit");
        assertThat(saved.getAmount()).isEqualByComparingTo("25.00");
        assertThat(saved.getAccount()).isSameAs(account);
    }

    @Test
    void recordWithdrawalSavesWithdrawalRow() {
        Account account = account("alice");

        transactionService.recordWithdrawal(account, new BigDecimal("10"));

        Transaction saved = capture(1).get(0);
        assertThat(saved.getType()).isEqualTo("Withdrawal");
        assertThat(saved.getAccount()).isSameAs(account);
    }

    @Test
    void recordTransferSavesDebitAndCreditRows() {
        Account from = account("alice");
        Account to = account("bob");

        transactionService.recordTransfer(from, to, new BigDecimal("5"));

        List<Transaction> saved = capture(2);
        assertThat(saved.get(0).getType()).isEqualTo("Transfer Out to bob");
        assertThat(saved.get(0).getAccount()).isSameAs(from);
        assertThat(saved.get(1).getType()).isEqualTo("Transfer In from alice");
        assertThat(saved.get(1).getAccount()).isSameAs(to);
    }

    @Test
    void getTransactionHistoryQueriesByAccountId() {
        Account account = account("alice");
        account.setId(7L);
        List<Transaction> expected = List.of(new Transaction());
        when(transactionRepository.findByAccountId(7L)).thenReturn(expected);

        assertThat(transactionService.getTransactionHistory(account)).isEqualTo(expected);
    }

    private List<Transaction> capture(int expectedSaves) {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(expectedSaves)).save(captor.capture());
        return captor.getAllValues();
    }
}
