package com.scanpay.v3.ehos_ms_scanpay_service.PayU.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto.PaymentRequest;
import com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto.PaymentResponse;

@RestController
@RequestMapping("/payu")
public class PayuController {

	private static final String KEY = "sSaf0k";
	private static final String SALT = "W22A1CzrHMitv0hsIjkBrApr9p4aIBHR";
	private static final String PAYU_URL = "https://test.payu.in/_payment";

	@PostMapping("/create-payment")
	public Map<String, String> createPayment(@RequestBody PaymentRequest req) {

		String txnId = "TXN" + System.currentTimeMillis();

		String hashString = KEY + "|" + txnId + "|" + req.getAmount() + "|" + req.getProductInfo() + "|"
				+ req.getFirstName() + "|" + req.getEmail() + "|||||||||||" + SALT;

		String hash = generateHash(hashString);

		Map<String, String> response = new HashMap<>();
		response.put("key", KEY);
		response.put("txnid", txnId);
		response.put("amount", req.getAmount());
		response.put("productinfo", req.getProductInfo());
		response.put("firstname", req.getFirstName());
		response.put("email", req.getEmail());
		response.put("phone", req.getPhone());
		response.put("surl", "http://localhost:8081/payu/payment-response");
		response.put("furl", "http://localhost:8081/payu/payment-response");
		response.put("hash", hash);
		response.put("payuUrl", PAYU_URL);

		return response;
	}

	private String generateHash(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));

			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@PostMapping("/payment-response")
	 public PaymentResponse handlePayuResponse(@RequestParam Map<String, String> params) {

        String status =params.containsKey("status")? params.get("status"):null;
        String txnId = params.containsKey("txnid")?params.get("txnid"):null;
        String amount =params.containsKey("amount")? params.get("amount"):null;
        String email =params.containsKey("email")? params.get("email"):null;
        String bankRefId =params.containsKey("bank_ref_num")? params.get("bank_ref_num"):null;
        String payuId = params.containsKey("mihpayid")?params.get("mihpayid"):null;
        String mode =params.containsKey("mode")? params.get("mode"):null;
        
        
        
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setTxnId(txnId);
        paymentResponse.setAmount(amount);
        paymentResponse.setEmail(email);
        paymentResponse.setStatus(status);
        paymentResponse.setBankRefId(bankRefId);
        paymentResponse.setPayuId(payuId);
        paymentResponse.setPaymentMode(mode);

        if ("success".equalsIgnoreCase(status)) {
        	paymentResponse.setStatus("SUCCESS");
        	paymentResponse.setMessage("Payment successful");
        } 
        else if ("failure".equalsIgnoreCase(status)) {
        	paymentResponse.setStatus("FAILED");
        	paymentResponse.setMessage("Payment failed");
        } 
        else {
        	paymentResponse.setStatus("PENDING");
        	paymentResponse.setMessage("Payment pending or unknown state");
        }

        return paymentResponse;
    }
}