package com.example.sb4memoryjwt.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;

public final class Jwks {
    private Jwks() {}

    public static RSAKey loadRsaKey(JwtProperties properties) {
        try {
            RSAPublicKey publicKey = readPublicKey(properties.publicKey());
            RSAPrivateKey privateKey = readPrivateKey(properties.privateKey());
            verifyKeyPair(publicKey, privateKey);
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.keyId())
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load configured RSA JWT keys", exception);
        }
    }

    public static JWKSource<SecurityContext> source(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    public static Map<String, Object> publicSet(List<RSAKey> rsaKeys) {
        return Map.of(
                "keys",
                rsaKeys.stream().map(key -> key.toPublicJWK().toJSONObject()).toList());
    }

    private static RSAPublicKey readPublicKey(Resource resource) throws Exception {
        byte[] bytes = decodePem(resource, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static RSAPrivateKey readPrivateKey(Resource resource) throws Exception {
        byte[] bytes = decodePem(resource, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static void verifyKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) throws Exception {
        byte[] challenge = "jwt-key-pair-check".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(challenge);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(challenge);
        if (!verifier.verify(signer.sign())) {
            throw new IllegalArgumentException("Configured RSA public and private keys do not match");
        }
    }

    private static byte[] decodePem(Resource resource, String header, String footer) throws IOException {
        String pem;
        try (var input = resource.getInputStream()) {
            pem = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String encoded = pem.replace(header, "").replace(footer, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
