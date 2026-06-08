package com.scanpay.v3.ehos_ms_scanpay_service.PayU.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto.PaymentRequest;
import com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto.PaymentResponse;
import com.scanpay.v3.ehos_ms_scanpay_service.PayU.entity.PaymentTransaction;
import com.scanpay.v3.ehos_ms_scanpay_service.PayU.repository.PaymentTransactionRepository;
import com.scanpay.v3.ehos_ms_scanpay_service.PayU.service.PayuService;

@RestController
@RequestMapping("/payu")
public class PayuController {

	@Value("${payu.key}")
	private String KEY;

	@Value("${payu.salt}")
	private String SALT;

	@Value("${payu.uat_url}")
	private String PAYU_URL;
	
	@Value("${payu.successurl}")
	private String successurl;
	
	@Value("${payu.failureurl}")
	private String failureurl;

	@Autowired
	private PaymentTransactionRepository paymentTransactionRepository;
	@Autowired
	private PayuService payuService;
	
	private static final Logger log = LoggerFactory.getLogger(PayuController.class);

	@PostMapping("/create-payment")
	public Map<String, String> createPayment(@RequestBody PaymentRequest req) {

		String txnId = "TXN" + System.currentTimeMillis();

		String hashString = KEY + "|" + txnId + "|" + req.getAmount() + "|" + req.getProductInfo() + "|"
				+ req.getFirstName() + "|" + req.getEmail() + "|||||||||||" + SALT;

		String hash = payuService.generateHash(hashString);
		
		 // -------------------------------
	    // SAVE TO DATABASE (CREATE TIME)
	    // -------------------------------
	    PaymentTransaction txn = new PaymentTransaction();

	    txn.setId(UUID.randomUUID());
	    txn.setTxnId(txnId);
	    txn.setEmail(req.getEmail());
	    txn.setAmount(new BigDecimal(req.getAmount()));
	    txn.setProductInfo(req.getProductInfo());
	    txn.setStatus("INITIATED");
	    txn.setHash(hash);
	    txn.setCreatedAt(LocalDateTime.now());
	    txn.setUpdatedAt(LocalDateTime.now());

	    paymentTransactionRepository.save(txn);

		Map<String, String> response = new HashMap<>();
		response.put("key", KEY);
		response.put("txnid", txnId);
		response.put("amount", req.getAmount());
		response.put("productinfo", req.getProductInfo());
		response.put("firstname", req.getFirstName());
		response.put("email", req.getEmail());
		response.put("phone", req.getPhone());
		response.put("surl", successurl);
		response.put("furl", failureurl);
		response.put("hash", hash);
		response.put("payuUrl", PAYU_URL);

		return response;
	}

	@PostMapping("/payment-response")
	public ResponseEntity<PaymentResponse> handlePayuResponse(
	        @RequestParam Map<String, String> params) {

	    log.info("PayU Response Received: {}", params);
//	    {mihpayid=403993715537624773, mode=CASH, status=success, unmappedstatus=captured, key=sSaf0k, txnid=TXN1780898316246, amount=14.00, discount=0.00, net_amount_debit=14, addedon=2026-06-08 11:28:41, productinfo=Test Product, firstname=Himanshu sahoo, lastname=, address1=, address2=, city=, state=, country=, zipcode=, email=hsahoo204@gmail.com, phone=8144263108, udf1=, udf2=, udf3=, udf4=, udf5=, udf6=, udf7=, udf8=, udf9=, udf10=, hash=1613e5cbd5834e71439ab8a910579d86d8b00bffd3c4053c127c2b56220b10d219e9e77dc57dcfb2bc0ccdb68191726f8b7bf89a0a324235b899a1771763434e, field1=, field2=, field3=, field4=, field5=, field6=, field7=, field8=, field9=Transaction Completed Successfully, payment_source=payu, PG_TYPE=CASH-PG, bank_ref_num=e8d4e2bb-5481-4953-9670-8acf4510025a, bankcode=JIOM, error=E000, error_Message=No Error}

	    PaymentResponse response = new PaymentResponse();

	    // 1. Validate hash (security check)
	    
	    if (!payuService.validateResponseHash(params)) {
	        response.setStatus("FAILED");
	        response.setMessage("Invalid PayU response hash");

	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(response);
	    }

	    // 2. Extract required fields
	    String txnId = params.getOrDefault("txnid", null);
	    String statusRaw = params.getOrDefault("status", "pending");
	    String amountStr = params.getOrDefault("amount", null);

	    String bankRefId = params.getOrDefault("bank_ref_num", null);
	    String payuId = params.getOrDefault("mihpayid", null);
	    String mode = params.getOrDefault("mode", null);
	    String email = params.getOrDefault("email", null);

	    // 3. Validate txnId
	    if (txnId == null) {
	        response.setStatus("FAILED");
	        response.setMessage("Missing txnid");
	        return ResponseEntity.badRequest().body(response);
	    }

	    // 4. Fetch transaction from DB
	    PaymentTransaction txn = paymentTransactionRepository.findByTxnId(txnId)
	            .orElseThrow(() -> new RuntimeException("Transaction not found"));

	    // 5. Idempotency check
	    if ("SUCCESS".equalsIgnoreCase(txn.getStatus())) {
	        response.setTxnId(txnId);
	        response.setAmount(amountStr);
	        response.setStatus("SUCCESS");
	        response.setMessage("Already processed");

	        return ResponseEntity.ok(response);
	    }

	    // 6. Amount validation (BigDecimal safe)
	    if (amountStr == null) {
	        response.setStatus("FAILED");
	        response.setMessage("Missing amount");
	        return ResponseEntity.badRequest().body(response);
	    }

	    BigDecimal requestAmount;
	    try {
	        requestAmount = new BigDecimal(amountStr);
	    } catch (NumberFormatException e) {
	        response.setStatus("FAILED");
	        response.setMessage("Invalid amount format");
	        return ResponseEntity.badRequest().body(response);
	    }

	    if (txn.getAmount().compareTo(requestAmount) != 0) {
	        response.setStatus("FAILED");
	        response.setMessage("Amount mismatch");
	        return ResponseEntity.badRequest().body(response);
	    }

	    // 7. Normalize status
	    String status;
	    if ("success".equalsIgnoreCase(statusRaw)) {
	        status = "SUCCESS";
	    } else if ("failure".equalsIgnoreCase(statusRaw)) {
	        status = "FAILED";
	    } else {
	        status = "PENDING";
	    }

	    // 8. Build response
	    response.setTxnId(txnId);
	    response.setAmount(requestAmount.toString());
	    response.setEmail(email);
	    response.setStatus(status);
	    response.setBankRefId(bankRefId);
	    response.setPayuId(payuId);
	    response.setPaymentMode(mode);

	    // 9. Update DB
	    txn.setTxnId(txnId);
	    txn.setStatus(status);
	    txn.setPayuId(payuId);
	    txn.setBankRefNo(bankRefId);
	    txn.setAmount(requestAmount);

	    paymentTransactionRepository.save(txn);

	    // 10. Return response
	    return ResponseEntity.ok(response);
	}
	
	
	
	/**
	 * PayU Webhook Endpoint
	 *
	 * This endpoint is called by PayU to notify payment status updates.
	 *
	 * Responsibilities:
	 * - Validates PayU response hash for security
	 * - Fetches transaction using txnId
	 * - Prevents duplicate processing (idempotency check)
	 * - Validates payment amount against stored transaction
	 * - Verifies payment status with PayU (server-side verification)
	 * - Updates transaction status in database
	 *
	 * Security checks:
	 * - Hash validation (to ensure request is from PayU)
	 * - Amount mismatch protection
	 * - Transaction existence validation
	 *
	 * Response:
	 * - Returns HTTP 200 for successful processing
	 * - Returns HTTP 400 for invalid or tampered requests
	 */
	
	// Webhook URL to be configured in PayU merchant dashboard:
	// http://localhost:8081/payu/webhook
	// This endpoint receives asynchronous payment status notifications from PayU.
	@PostMapping("/webhook")
	public ResponseEntity<PaymentResponse> webhook(@RequestParam Map<String, String> params) {
		PaymentResponse response = new PaymentResponse();

		if (!payuService.validateResponseHash(params)) {
			response.setStatus("FAILED");
			response.setMessage("Invalid Hash");
			return ResponseEntity.badRequest().body(response);
		}

		String txnId = params.get("txnid");

		PaymentTransaction txn = paymentTransactionRepository.findByTxnId(txnId).orElseThrow();

		if (txn.getStatus().equalsIgnoreCase("SUCCESS")) {
			response.setTxnId(txnId);
			response.setStatus("SUCCESS");
			response.setMessage("Already Processed");
			return ResponseEntity.ok(response);
		}
		String amountStr = params.get("amount");

		if (amountStr == null) {
			response.setStatus("FAILED");
			response.setMessage("Missing amount");
			return ResponseEntity.badRequest().body(response);
		}

		BigDecimal requestAmount;
		try {
			requestAmount = new BigDecimal(amountStr);
		} catch (NumberFormatException e) {
			response.setStatus("FAILED");
			response.setMessage("Invalid amount format");
			return ResponseEntity.badRequest().body(response);
		}

		if (txn.getAmount().compareTo(requestAmount) != 0) {
			response.setStatus("FAILED");
			response.setMessage("Amount Mismatch");
			return ResponseEntity.badRequest().body(response);
		}

		boolean verified = payuService.verifyPayment(txnId);

		if (!verified) {

			txn.setStatus("FAILED");
			response.setStatus("FAILED");
			response.setMessage("Payment verification failed");

		} else {
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setTxnId(txnId);
			paymentResponse.setAmount(params.get("amount"));
			paymentResponse.setStatus(txn.getStatus());
			paymentResponse.setBankRefId(params.get("bank_ref_num"));
			paymentResponse.setPayuId(params.get("mihpayid"));
			response.setPaymentMode(params.getOrDefault("mode", null));
		}

		paymentTransactionRepository.save(txn);

		return ResponseEntity.ok(response);
	}
}