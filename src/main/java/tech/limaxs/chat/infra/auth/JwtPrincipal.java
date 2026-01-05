package tech.limaxs.chat.infra.auth;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JwtPrincipal {

    private final SecurityIdentity securityIdentity;

    public JwtPrincipal(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    public UUID getUserId() {
        if (securityIdentity == null || securityIdentity.getPrincipal() == null) {
            throw new UnauthorizedException("No authentication provided");
        }
        String userIdStr = securityIdentity.getPrincipal().getName();
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new UnauthorizedException("Invalid authentication: no user ID");
        }
        return UUID.fromString(userIdStr);
    }

    public String getName() {
        if (securityIdentity == null || securityIdentity.getPrincipal() == null) {
            throw new UnauthorizedException("No authentication provided");
        }

        // Try to get from JsonWebToken claims first
        if (securityIdentity.getPrincipal() instanceof JsonWebToken) {
            JsonWebToken jwt = (JsonWebToken) securityIdentity.getPrincipal();
            return Optional.ofNullable(jwt.getClaim("name"))
                    .map(Object::toString)
                    .orElse("");
        }
        // Fallback to security attributes
        return Optional.ofNullable(securityIdentity.getAttributes().get("name"))
                .map(Object::toString)
                .orElse("");
    }
}
