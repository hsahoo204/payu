package com.scanpay.v3.ehos_ms_scanpay_service.PayU.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scanpay.v3.ehos_ms_scanpay_service.PayU.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

	Optional<PaymentTransaction> findByTxnId(String txnId);

}
