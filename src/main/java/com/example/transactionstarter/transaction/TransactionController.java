package com.example.transactionstarter.transaction;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Create a new transaction
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = new Transaction(
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTransactionType()
        );
        transaction.setTransactionStatus(request.getTransactionStatus());
        Transaction createdTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    /**
     * Get transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getTransaction(@PathVariable String id) {
        if (id == null || !id.matches("[A-Z]{2}[0-9]{10}")) {
            return ResponseEntity.badRequest().body("Transaction ID must start with TP followed by 10 digits");
        }
        return transactionService.getTransactionById(id)
                .map(transaction -> ResponseEntity.<Object>ok(transaction))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found"));
    }

    /**
     * Update transaction status
     * PUT /api/transactions/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Object> updateTransactionStatus(
            @PathVariable String id,
            @RequestParam TransactionStatus status) {
        if (id == null || !id.matches("[A-Z]{2}[0-9]{10}")) {
            return ResponseEntity.badRequest().body("Transaction ID must start with TP followed by 10 digits");
        }
        if (transactionService.getTransactionById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
        }
        try {
            return transactionService.updateTransactionStatus(id, status)
                    .map(transaction -> ResponseEntity.<Object>ok(transaction))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found"));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
        }
    }

    /**
     * Get all transactions for a customer
     * GET /api/transactions/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Object> getCustomerTransactions(@PathVariable String customerId) {
        if (customerId == null || !customerId.matches("\\d+")) {
            return ResponseEntity.badRequest().body("Customer ID must contain only numbers");
        }
        List<Transaction> transactions = transactionService.getCustomerTransactions(customerId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get all transactions
     * GET /api/transactions
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }
}
