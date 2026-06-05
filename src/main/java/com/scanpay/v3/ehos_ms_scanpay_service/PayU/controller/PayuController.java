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
		System.out.println(params);
		
//      WALLET

//      {mihpayid=403993715537613072, mode=CASH, status=success, unmappedstatus=captured, 
//      key=sSaf0k, txnid=TXN1780650313168, amount=10.00, discount=0.00, net_amount_debit=10, 
//              addedon=2026-06-05 14:35:14, productinfo=Test Product, firstname=Priyanshi, lastname=, 
//              address1=, address2=, city=, state=, country=, zipcode=, email=priyanshig341@gmail.com, 
//              phone=916283548482, udf1=, udf2=, udf3=, udf4=, udf5=, udf6=, udf7=, udf8=, udf9=, udf10=, 
//              hash=d4c3a57014882fbf9aa9240fd714609ef8c671f28d029d0f820dc9e70e33766feb800c1043a4d6f890f3bfa9ce20fa67ecd7a4e49428fdbe016d2afd00e6ddb4, 
//              field1=, field2=, field3=, field4=, field5=, field6=, field7=, field8=, field9=Transaction Completed Successfully, 
//              payment_source=payu, PG_TYPE=CASH-PG, bank_ref_num=684814fc-0e3d-42a1-b0a0-ba7377b63f1b, 
//              bankcode=JIOM, error=E000, error_Message=No Error}


//      CARD

//      {mihpayid=403993715537613125, mode=CC, status=success, unmappedstatus=captured, key=sSaf0k, txnid=TXN1780650530120, amount=199.50, cardCategory=domestic, discount=0.00, net_amount_debit=199.5, addedon=2026-06-05 14:38:51, productinfo=Test Product, firstname=himanshu, lastname=, address1=, address2=, city=, state=, country=, zipcode=, email=hsahoo204@gmail.com, phone=8144263108, udf1=, udf2=, udf3=, udf4=, udf5=, udf6=, udf7=, udf8=, udf9=, udf10=, hash=3386ad345debdba55b4f5e756a057894d59e62b020a05614a2138644aaf7bd9e738efc35a80174a793ee08e95ab2e4a0869647bac74ec31585a79680b9488728, field1=273211214538, field2=588157, field3=199.50, field4=, field5=00, field6=02, field7=AUTHPOSITIVE, field8=AUTHORIZED, field9=Transaction is Successful, payment_source=payu, PG_TYPE=CC-PG, bank_ref_num=494778313478092400, bankcode=CC, error=E000, error_Message=No Error, cardnum=XXXXXXXXXXXX2346}
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