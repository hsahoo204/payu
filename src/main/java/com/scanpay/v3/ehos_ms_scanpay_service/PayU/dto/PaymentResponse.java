package com.scanpay.v3.ehos_ms_scanpay_service.PayU.dto;

public class PaymentResponse {
	   private String status;
	    private String txnId;
	    private String amount;
	    private String email;
	    private String message;
	    private String bankRefId;   // bank_ref_num
	    private String payuId;      // mihpayid
	    private String paymentMode;  // mode
	    
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getTxnId() {
			return txnId;
		}
		public void setTxnId(String txnId) {
			this.txnId = txnId;
		}
		public String getAmount() {
			return amount;
		}
		public void setAmount(String amount) {
			this.amount = amount;
		}
		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public String getBankRefId() {
			return bankRefId;
		}
		public void setBankRefId(String bankRefId) {
			this.bankRefId = bankRefId;
		}
		public String getPayuId() {
			return payuId;
		}
		public void setPayuId(String payuId) {
			this.payuId = payuId;
		}
		public String getPaymentMode() {
			return paymentMode;
		}
		public void setPaymentMode(String paymentMode) {
			this.paymentMode = paymentMode;
		}
	    
	    
}
