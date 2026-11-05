package com.github.anyuoyuna.lifeassistant.exception;

public class GeminiUnavailableException extends RuntimeException {
    public GeminiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GeminiUnavailableException from(Throwable cause) {
        return isTemporaryFailure(cause)
                ? new GeminiUnavailableException("Gemini is temporarily unavailable", cause)
                : null;
    }

    private static boolean isTemporaryFailure(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            String details = (current.getClass().getName() + " " + current.getMessage()).toLowerCase();
            if (details.contains("429")
                    || details.contains("503")
                    || details.contains("resource_exhausted")
                    || details.contains("unavailable")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
