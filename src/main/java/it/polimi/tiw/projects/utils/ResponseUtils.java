package it.polimi.tiw.projects.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends a JSON error response.
     *
     * @param resp         The HttpServletResponse object.
     * @param statusCode   The HTTP status code.
     * @param errorMessage The error message.
     * @throws IOException If an input or output exception occurs
     */
    public static void sendError(HttpServletResponse resp, int statusCode, String errorMessage) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        resp.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

    /**
     * Sends a JSON success response.
     *
     * @param resp       The HttpServletResponse object.
     * @param statusCode The HTTP status code.
     * @param data       The data to be sent as JSON.
     * @throws IOException If an input or output exception occurs
     */
    public static void sendJson(HttpServletResponse resp, int statusCode, Object data) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(mapper.writeValueAsString(data));
    }
}
