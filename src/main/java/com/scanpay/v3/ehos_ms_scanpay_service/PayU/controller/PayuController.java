package com.scanpay.v3.ehos_ms_scanpay_service.PayU.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto.PaymentRequest;

@RestController
@RequestMapping("/payu")
public class PayuController {

    private static final String KEY = "sSaf0k";
    private static final String SALT = "W22A1CzrHMitv0hsIjkBrApr9p4aIBHR";
    private static final String PAYU_URL = "https://test.payu.in/_payment";

    @PostMapping("/create-payment")
    public Map<String, String> createPayment(@RequestBody PaymentRequest req) {

        String txnId = "TXN" + System.currentTimeMillis();

        String hashString = KEY + "|" + txnId + "|" + req.getAmount() + "|"
                + req.getProductInfo() + "|" + req.getFirstName() + "|" + req.getEmail()
                + "|||||||||||" + SALT;

        String hash = generateHash(hashString);

        Map<String, String> response = new HashMap<>();
        response.put("key", KEY);
        response.put("txnid", txnId);
        response.put("amount", req.getAmount());
        response.put("productinfo", req.getProductInfo());
        response.put("firstname", req.getFirstName());
        response.put("email", req.getEmail());
        response.put("phone", req.getPhone());
        response.put("surl", "http://localhost:8081/payu/success");
        response.put("furl", "http://localhost:8081/payu/failure");
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

    @GetMapping("/success")
    public String success() {
        return "Payment Success";
    }

    @GetMapping("/failure")
    public String failure() {
        return "Payment Failed";
    }
}