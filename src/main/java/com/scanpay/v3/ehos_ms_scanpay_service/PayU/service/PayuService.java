package com.scanpay.v3.ehos_ms_scanpay_service.PayU.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PayuService {
	@Value("${payu.key}")
	private String KEY;

	@Value("${payu.salt}")
	private String SALT;
	@Autowired
	private RestTemplate restTemplate;

	@Value("${payu.uat_verify-url}")
	private String VERIFY_URL;

	public boolean verifyPayment(String txnId) {
		try {

			String command = "verify_payment";

			String hashString = KEY + "|" + command + "|" + txnId + "|" + SALT;

			String hash = generateHash(hashString);

			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

			body.add("key", KEY);
			body.add("command", command);
			body.add("hash", hash);
			body.add("var1", txnId);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(VERIFY_URL, request, String.class);

			if (response.getBody() == null) {
				return false;
			}

			ObjectMapper mapper = new ObjectMapper();

			JsonNode root = mapper.readTree(response.getBody());

			// ❗ basic API success check
			if (root == null || root.path("status").isMissingNode()) {
				return false;
			}

			int status = root.path("status").asInt();

			if (status != 1) {
				return false;
			}

			// 🔥 transaction-level validation
			JsonNode txnNode = root.path("transaction_details").path(txnId);

			if (txnNode.isMissingNode()) {
				return false;
			}

			String txnStatus = txnNode.path("status").asText();

			String unmappedStatus = txnNode.path("unmappedstatus").asText();

			// ✅ FINAL SAFE CHECK
			return "success".equalsIgnoreCase(txnStatus) || "captured".equalsIgnoreCase(unmappedStatus);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public String generateHash(String data) {
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

	public boolean validateResponseHash(Map<String, String> params) {

		String receivedHash = params.get("hash");
		   String hashString = buildResponseHashString(params);

	        String generatedHash = generateHash(hashString);

	        return generatedHash.equalsIgnoreCase(receivedHash);
	}
	
	 // -------------------------------
    // BUILD RESPONSE HASH STRING
    // -------------------------------
    private String buildResponseHashString(Map<String, String> params) {

        String[] udf = new String[10];

        for (int i = 1; i <= 10; i++) {
            udf[i - 1] = params.getOrDefault("udf" + i, "");
        }

        return SALT + "|"
                + params.getOrDefault("status", "") + "|"
                + udf[9] + "|"
                + udf[8] + "|"
                + udf[7] + "|"
                + udf[6] + "|"
                + udf[5] + "|"
                + udf[4] + "|"
                + udf[3] + "|"
                + udf[2] + "|"
                + udf[1] + "|"
                + udf[0] + "|"
                + params.getOrDefault("email", "") + "|"
                + params.getOrDefault("firstname", "") + "|"
                + params.getOrDefault("productinfo", "") + "|"
                + params.getOrDefault("amount", "") + "|"
                + params.getOrDefault("txnid", "") + "|"
                + KEY;
    }
}
