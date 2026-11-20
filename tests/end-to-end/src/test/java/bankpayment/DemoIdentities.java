package bankpayment;

import tools.jackson.databind.json.JsonMapper;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;

final class DemoIdentities {
    static final String ISSUER = "urn:bankpayment:demo";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();
    private final KeyPair keyPair = generateKeyPair();

    enum Identity {
        CLIENT("client-demo", "CLIENT"),
        OPERATIONS("operations-demo", "OPERATIONS"),
        PAYMENT_SERVICE("payment-service", "SERVICE"),
        ACCOUNT_SERVICE("account-service", "SERVICE"),
        BILLER_SERVICE("biller-service", "SERVICE");

        private final String subject;
        private final String role;

        Identity(String subject, String role) {
            this.subject = subject;
            this.role = role;
        }
    }

    PublicKey publicKey() {
        return keyPair.getPublic();
    }

    String token(Identity identity, String audience, Instant issuedAt, Instant expiresAt) {
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("The token must expire after issuance");
        }
        var header = encodeJson(Map.of("typ", "JWT", "alg", "RS256"));
        var claims = encodeJson(Map.of(
                "iss", ISSUER,
                "sub", identity.subject,
                "aud", audience,
                "iat", issuedAt.getEpochSecond(),
                "exp", expiresAt.getEpochSecond(),
                "groups", List.of(identity.role)
        ));
        var signingInput = header + "." + claims;
        try {
            var signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(signingInput.getBytes(US_ASCII));
            return signingInput + "." + BASE64.encodeToString(signer.sign());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot sign the demo token", exception);
        }
    }

    private static String encodeJson(Map<String, ?> value) {
        return BASE64.encodeToString(JSON.writeValueAsBytes(value));
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot generate demo signing keys", exception);
        }
    }
}
