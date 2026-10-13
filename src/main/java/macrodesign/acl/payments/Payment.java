package macrodesign.acl.payments;

public record Payment(String orderId, Money amount, String cardToken) {
}
