package com.example.transactionstarter.transaction;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateTransactionRequest {

    @NotNull(message = "Customer ID cannot be null")
    @Pattern(regexp = "\\d{1,8}", message = "Customer ID must be a valid numeric value up to 8 digits")
    private String customerId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999999.99", message = "Amount must not exceed 9999999999.99")
    @Digits(integer = 10, fraction = 2, message = "Amount must have no more than 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g., USD, EUR)")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be uppercase")
    private String currency;

    @NotNull(message = "Transaction Type cannot be null")
    private TransactionType transactionType;

    private TransactionStatus transactionStatus;

    // Constructors
    public CreateTransactionRequest() {
    }

    public CreateTransactionRequest(String customerId, BigDecimal amount, String currency,
                                   TransactionType transactionType) {
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
}
