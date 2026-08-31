package com.example.transactionstarter.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TransactionControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    private CreateTransactionRequest createTestRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "12345678",
                new BigDecimal("1234567890"),
                "USD",
                TransactionType.DEPOSIT
        );
        return request;
    }

        @NonNull
        private Transaction createTestTransaction() {
                Transaction transaction = new Transaction(
                "12345678",
                new BigDecimal("1234567890"),
                "USD",
                TransactionType.DEPOSIT
                );
                transaction.setTransactionId("TP1234567890");
                return transaction;
    }

    @Test
    void testCustomerIdPreservesLeadingZerosInDatabase() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "00000001",
                new BigDecimal("250.00"),
                "USD",
                TransactionType.DEPOSIT
        );

        ResponseEntity<Transaction> response = restTemplate.postForEntity(
                "/api/transactions",
                request,
                Transaction.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("00000001", Objects.requireNonNull(response.getBody()).getCustomerId());
        assertEquals("00000001", transactionRepository.findById(Objects.requireNonNull(response.getBody()).getTransactionId())
                .orElseThrow().getCustomerId());
    }

    /**
     * Test 1: Create Transaction
     * Verifies that a valid transaction is created successfully with HTTP 201
     */
    @Test
    void testCreateTransaction() {
        ResponseEntity<Transaction> response = restTemplate.postForEntity(
                "/api/transactions",
                createTestRequest(),
                Transaction.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Transaction body = Objects.requireNonNull(response.getBody());
        assertEquals("12345678", body.getCustomerId());
        assertTrue(body.getTransactionId().matches("[A-Z]{2}[0-9]{10}"));
        assertEquals(new BigDecimal("1234567890"), body.getAmount());
        assertEquals("USD", body.getCurrency());
        assertEquals(TransactionType.DEPOSIT, body.getTransactionType());
        assertEquals(TransactionStatus.PENDING, body.getTransactionStatus());
    }

    /**
     * Test 2: Get Transaction by ID
     * Verifies that an existing transaction can be retrieved by ID
     */
    @Test
    void testGetTransaction() {
        // First create a transaction
        Transaction created = transactionRepository.save(Objects.requireNonNull(createTestTransaction()));

        // Then retrieve it
        ResponseEntity<Transaction> response = restTemplate.getForEntity(
                "/api/transactions/" + created.getTransactionId(),
                Transaction.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Transaction body = Objects.requireNonNull(response.getBody());
        assertEquals(created.getTransactionId(), body.getTransactionId());
        assertEquals("12345678", body.getCustomerId());
    }

    /**
     * Test 3: Update Transaction Status
     * Verifies that transaction status can be updated successfully
     */
    @Test
    void testUpdateTransactionStatus() {
        // Create a transaction
        Transaction created = transactionRepository.save(Objects.requireNonNull(createTestTransaction()));

        // Update its status
        ResponseEntity<Transaction> response = restTemplate.exchange(
                "/api/transactions/" + created.getTransactionId() + "/status?status=COMPLETED",
                HttpMethod.PUT,
                null,
                Transaction.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Transaction body = Objects.requireNonNull(response.getBody());
        assertEquals(TransactionStatus.COMPLETED, body.getTransactionStatus());
    }

    /**
     * Test 4: Get All Transactions for a Customer
     * Verifies that all transactions for a specific customer can be retrieved
     */
    @Test
    void testGetCustomerTransactions() {
        // Create multiple transactions for the same customer
        Transaction tx1 = Objects.requireNonNull(
                new Transaction("12345678", new BigDecimal("1234567891"), "USD", TransactionType.DEPOSIT));
        Transaction tx2 = Objects.requireNonNull(
                new Transaction("12345678", new BigDecimal("1234567892"), "EUR", TransactionType.WITHDRAWAL));
        Transaction tx3 = Objects.requireNonNull(
                new Transaction("87654321", new BigDecimal("1234567893"), "GBP", TransactionType.TRANSFER));

        tx1.setTransactionId("TP1234567891");
        tx2.setTransactionId("TP1234567892");
        tx3.setTransactionId("TP1234567893");

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);
        transactionRepository.save(tx3);

        // Get transactions for customer 1
        ResponseEntity<Transaction[]> response = restTemplate.getForEntity(
                "/api/transactions/customer/12345678",
                Transaction[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Transaction[] body = Objects.requireNonNull(response.getBody());
        assertEquals(2, body.length);
        assertTrue(java.util.Arrays.stream(body)
                .allMatch(tx -> tx.getCustomerId().equals("12345678")));
    }

    /**
     * Test 5: Transaction Not Found
     * Verifies that requesting a non-existent transaction returns 404
     */
    @Test
    void testGetTransactionNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/transactions/TP9999999999",
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
        * Test 6: Validation - Invalid Amount (Negative)
        * Verifies that transaction with a negative amount is rejected
     */
    @Test
    void testCreateTransactionWithInvalidAmount() {
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                "12345678",
                new BigDecimal("-1"),
                "USD",
                TransactionType.DEPOSIT
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/transactions",
                invalidRequest,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Test 7: Validation - Invalid Currency Code
     * Verifies that transaction with invalid currency code is rejected
     */
    @Test
    void testCreateTransactionWithInvalidCurrency() {
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                "12345678",
                new BigDecimal("1000000000"),
                "INVALID",
                TransactionType.DEPOSIT
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/transactions",
                invalidRequest,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Test 8: Validation - Null Customer ID
     * Verifies that transaction without customer ID is rejected
     */
    @Test
    void testCreateTransactionWithNullCustomerId() {
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                null,
                new BigDecimal("1000000000"),
                "USD",
                TransactionType.DEPOSIT
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/transactions",
                invalidRequest,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Test 9: Initial Transaction Status
     * Verifies that newly created transactions have PENDING status by default
     */
    @Test
    void testNewTransactionHasPendingStatus() {
        Transaction created = transactionRepository.save(Objects.requireNonNull(createTestTransaction()));

        assertNotNull(created.getTransactionId());
        assertEquals(TransactionStatus.PENDING, created.getTransactionStatus());
        assertNotNull(created.getCreatedAt());
    }

    /**
     * Test 10: Empty Customer Transactions
     * Verifies that getting transactions for a customer with no transactions returns empty list
     */
    @Test
    void testGetTransactionsForCustomerWithNoTransactions() {
        ResponseEntity<Transaction[]> response = restTemplate.getForEntity(
                "/api/transactions/customer/99999998",
                Transaction[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Transaction[] body = Objects.requireNonNull(response.getBody());
        assertEquals(0, body.length);
    }

        @Test
        void testTransactionIdIsGeneratedAutomatically() {
                ResponseEntity<Transaction> first = restTemplate.postForEntity(
                                "/api/transactions", createTestRequest(), Transaction.class);
                ResponseEntity<Transaction> second = restTemplate.postForEntity(
                                "/api/transactions", createTestRequest(), Transaction.class);

                String firstId = Objects.requireNonNull(first.getBody()).getTransactionId();
                String secondId = Objects.requireNonNull(second.getBody()).getTransactionId();
                assertTrue(firstId.matches("TP[0-9]{10}"));
                assertTrue(secondId.matches("TP[0-9]{10}"));
                assertNotEquals(firstId, secondId);
        }

        @Test
        void testLowercaseCurrencyReturnsBadRequest() {
                CreateTransactionRequest request = createTestRequest();
                request.setCurrency("usd");

                ResponseEntity<String> response = restTemplate.postForEntity(
                                "/api/transactions", request, String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void testNonPendingInitialStatusReturnsBadRequest() {
                CreateTransactionRequest request = createTestRequest();
                request.setTransactionStatus(TransactionStatus.COMPLETED);

                ResponseEntity<String> response = restTemplate.postForEntity(
                                "/api/transactions", request, String.class);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void testUnchangedStatusReturnsConflict() {
                Transaction created = transactionRepository.save(createTestTransaction());

                ResponseEntity<String> response = restTemplate.exchange(
                                "/api/transactions/" + created.getTransactionId() + "/status?status=PENDING",
                                HttpMethod.PUT, null, String.class);

                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        void testCompletedTransactionIsImmutable() {
                Transaction created = transactionRepository.save(createTestTransaction());
                restTemplate.exchange(
                                "/api/transactions/" + created.getTransactionId() + "/status?status=COMPLETED",
                                HttpMethod.PUT, null, String.class);

                ResponseEntity<String> response = restTemplate.exchange(
                                "/api/transactions/" + created.getTransactionId() + "/status?status=FAILED",
                                HttpMethod.PUT, null, String.class);

                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        void testStatusUpdateReturnsHelpfulMessageBody() {
                Transaction created = transactionRepository.save(createTestTransaction());

                ResponseEntity<String> response = restTemplate.exchange(
                                "/api/transactions/" + created.getTransactionId() + "/status?status=PENDING",
                                HttpMethod.PUT,
                                null,
                                String.class);

                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().toLowerCase().contains("must change")
                                || response.getBody().toLowerCase().contains("not allowed"));
        }
}
