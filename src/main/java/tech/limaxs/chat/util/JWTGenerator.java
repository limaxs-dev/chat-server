package tech.limaxs.chat.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for generating test JWT tokens.
 * This simulates what your Client Service would do when users log in.
 */
public class JWTGenerator {

    private static final String PRIVATE_KEY_PATH = "src/main/resources/keys/private-key.pem";
    private static final long TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    public static void main(String[] args) {
        try {
            // Generate tokens for test users
            generateAndPrintToken("550e8400-e29b-41d4-a716-446655440000", "Alice", "test-tenant");
            generateAndPrintToken("550e8400-e29b-41d4-a716-446655440001", "Bob", "test-tenant");
            generateAndPrintToken("550e8400-e29b-41d4-a716-446655440002", "Charlie", "test-tenant");
            generateAndPrintToken("550e8400-e29b-41d4-a716-446655440003", "Diana", "test-tenant");
        } catch (Exception e) {
            System.err.println("Error generating JWT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String generateToken(String userId, String name, String tenantId) throws Exception {
        PrivateKey privateKey = loadPrivateKey();

        Date now = new Date();
        Date exp = new Date(now.getTime() + TOKEN_VALIDITY_MS);

        return Jwts.builder()
                .subject(userId)
                .claim("name", name)
                .claim("tenantId", tenantId)  // Store tenantId as a claim instead of issuer
                .issuer("nexus-chat-engine")   // Use fixed issuer matching mp.jwt.verify.issuer
                .issuedAt(now)
                .expiration(exp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public static void generateAndPrintToken(String userId, String name, String tenantId) throws Exception {
        String token = generateToken(userId, name, tenantId);

        System.out.println("========================================");
        System.out.println("User: " + name);
        System.out.println("User ID: " + userId);
        System.out.println("Tenant: " + tenantId);
        System.out.println("========================================");
        System.out.println("JWT Token:");
        System.out.println(token);
        System.out.println();
        System.out.println("Export command:");
        System.out.println("export TOKEN=\"" + token + "\"");
        System.out.println("========================================");
        System.out.println();
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        String keyContent = new String(Files.readAllBytes(
                Paths.get(PRIVATE_KEY_PATH)));

        // Remove PEM headers and decode
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}
