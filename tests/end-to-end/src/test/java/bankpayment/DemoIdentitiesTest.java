package bankpayment;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static bankpayment.DemoIdentities.Identity.CLIENT;
import static bankpayment.DemoIdentities.Identity.OPERATIONS;
import static bankpayment.DemoIdentities.Identity.PAYMENT_SERVICE;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoIdentitiesTest {
    @Test
    void signsSeparateClientOperationsAndServiceIdentities() throws Exception {
        var identities = new DemoIdentities();
        var now = Instant.parse("2026-11-20T10:00:00Z");
        var expiry = now.plusSeconds(300);
        var mapper = JsonMapper.builder().build();
        var decoder = Base64.getUrlDecoder();

        var client = identities.token(CLIENT, "payment-service", now, expiry).split("\\.");
        var operations = identities.token(OPERATIONS, "payment-service", now, expiry).split("\\.");
        var service = identities.token(PAYMENT_SERVICE, "account-service", now, expiry).split("\\.");

        var clientClaims = mapper.readTree(decoder.decode(client[1]));
        var operationsClaims = mapper.readTree(decoder.decode(operations[1]));
        var serviceClaims = mapper.readTree(decoder.decode(service[1]));
        assertEquals("client-demo", clientClaims.get("sub").stringValue());
        assertEquals("CLIENT", clientClaims.get("groups").get(0).stringValue());
        assertEquals("OPERATIONS", operationsClaims.get("groups").get(0).stringValue());
        assertEquals("payment-service", serviceClaims.get("sub").stringValue());
        assertEquals("account-service", serviceClaims.get("aud").stringValue());
        assertEquals("SERVICE", serviceClaims.get("groups").get(0).stringValue());
        assertEquals(DemoIdentities.ISSUER, serviceClaims.get("iss").stringValue());
        assertEquals(now.getEpochSecond(), serviceClaims.get("iat").longValue());
        assertEquals(expiry.getEpochSecond(), serviceClaims.get("exp").longValue());

        var verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(identities.publicKey());
        verifier.update((service[0] + "." + service[1]).getBytes(US_ASCII));
        assertTrue(verifier.verify(decoder.decode(service[2])));
        verifier.update((service[0] + "." + client[1]).getBytes(US_ASCII));
        assertFalse(verifier.verify(decoder.decode(service[2])));
    }
}
