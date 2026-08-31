package com.example.transactionstarter.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Currency;
import java.util.Locale;
import java.security.SecureRandom;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SecureRandom random = new SecureRandom();

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Create a new transaction
     */
    public Transaction createTransaction(@NonNull Transaction transaction) {
        validateTransaction(transaction);
        transaction.setCustomerId(normalizeCustomerId(transaction.getCustomerId()));
        transaction.setTransactionId(generateUniqueTransactionId());
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        return transactionRepository.save(transaction);
    }

    /**
     * Get transaction by ID
     */
    public Optional<Transaction> getTransactionById(@NonNull String transactionId) {
        validateTransactionId(transactionId);
        return transactionRepository.findById(transactionId);
    }

    /**
     * Update transaction status
     */
    public Optional<Transaction> updateTransactionStatus(@NonNull String transactionId,
            @NonNull TransactionStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }
        return transactionRepository.findById(transactionId).map(transaction -> {
            if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
                throw new IllegalStateException("Only PENDING transactions can change status");
            }
            if (newStatus == TransactionStatus.PENDING) {
                throw new IllegalStateException("Transaction status must change");
            }
            transaction.setTransactionStatus(newStatus);
            return transactionRepository.save(transaction);
        });
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction is required");
        }
        if (transaction.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID must be from 00000001 to 99999998");
        }

        String normalizedCustomerId = normalizeCustomerId(transaction.getCustomerId());
        long customerIdValue = Long.parseLong(normalizedCustomerId);
        if (customerIdValue < 1L || customerIdValue > 99_999_998L) {
            throw new IllegalArgumentException("Customer ID must be from 00000001 to 99999998");
        }
        if (transaction.getAmount() == null || transaction.getAmount().scale() > 2
                || transaction.getAmount().compareTo(new java.math.BigDecimal("0.01")) < 0
                || transaction.getAmount().compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Amount must be greater than 0 and no more than 9999999999.99");
        }
        if (transaction.getCurrency() == null
                || !transaction.getCurrency().equals(transaction.getCurrency().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Currency must be uppercase");
        }
        try {
            Currency.getInstance(transaction.getCurrency());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Currency must be a valid ISO-4217 code", exception);
        }
        if (transaction.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (transaction.getTransactionStatus() != null
                && transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Initial status must be PENDING");
        }
    }

    private String normalizeCustomerId(String customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must be from 00000001 to 99999998");
        }

        String trimmedCustomerId = customerId.trim();
        if (!trimmedCustomerId.matches("\\d+")) {
            throw new IllegalArgumentException("Customer ID must be a valid numeric value");
        }

        long customerIdValue = Long.parseLong(trimmedCustomerId);
        if (customerIdValue < 1L || customerIdValue > 99_999_998L) {
            throw new IllegalArgumentException("Customer ID must be from 00000001 to 99999998");
        }

        return String.format("%08d", customerIdValue);
    }

    private String generateUniqueTransactionId() {
        String transactionId;
        do {
                    transactionId = String.format("TP%010d",
                    random.nextLong(10_000_000_000L));
        } while (transactionRepository.existsById(transactionId));
        return transactionId;
    }

    private void validateTransactionId(String transactionId) {
        if (transactionId == null || !transactionId.matches("TP[0-9]{10}")) {
            throw new IllegalArgumentException("Transaction ID must start with TP followed by 10 digits");
        }
    }

    /**
     * Get all transactions for a customer
     */
    public List<Transaction> getCustomerTransactions(@NonNull String customerId) {
        String normalizedCustomerId = normalizeCustomerId(customerId);
        return transactionRepository.findByCustomerId(normalizedCustomerId);
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
