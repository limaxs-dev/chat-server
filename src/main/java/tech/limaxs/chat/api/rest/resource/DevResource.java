package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.limaxs.chat.api.rest.dto.TokenResponse;
import tech.limaxs.chat.api.rest.dto.ErrorResponse;
import tech.limaxs.chat.util.JWTGenerator;

import java.util.Date;

/**
 * DEV ONLY: Endpoint to generate test JWT tokens.
 * This should be disabled in production.
 */
@Path("/api/dev")
@RunOnVirtualThread
public class DevResource {

    @GET
    @Path("/token/{userId}")
    @Produces("application/json")
    public Response generateToken(
            @PathParam("userId") String userId,
            @QueryParam("name") @DefaultValue("Test User") String name) {
        try {
            String token = JWTGenerator.generateToken(userId, name);

            return Response.ok(new TokenResponse(
                    token,
                    userId,
                    name,
                    new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)
            )).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }
}
