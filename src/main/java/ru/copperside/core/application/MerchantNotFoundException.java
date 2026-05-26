package ru.copperside.core.application;

public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(Long merchantId) {
        super("Merchant not found: " + merchantId);
    }
}
