package it.polimi.tiw.projects.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    private static final Logger logger = LoggerFactory.getLogger(ResponseUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends a JSON error response.
     *
     * @param resp         The HttpServletResponse object.
     * @param statusCode   The HTTP status code.
     * @param errorMessage The error message.
     */
    public static void sendError(HttpServletResponse resp, int statusCode, String errorMessage) {
        try {
            resp.setStatus(statusCode);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
        } catch (IOException e) {
            logger.error(
                    "IOException while sending error response. Status: {}, Message: {}. Client may not have received the error.",
                    statusCode, errorMessage, e);
        }
    }

    /**
     * Sends a JSON success response.
     *
     * @param resp       The HttpServletResponse object.
     * @param statusCode The HTTP status code.
     * @param data       The data to be sent as JSON.
     */
    public static void sendJson(HttpServletResponse resp, int statusCode, Object data) {
        try {
            resp.setStatus(statusCode);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(mapper.writeValueAsString(data));
        } catch (IOException e) {
            logger.error(
                    "IOException while sending JSON success response. Status: {}. Client may not have received the data.",
                    statusCode, e);
        }
    }
}
