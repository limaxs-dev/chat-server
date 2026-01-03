package tech.limaxs.chat.api.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorResponse {
    private String error;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
