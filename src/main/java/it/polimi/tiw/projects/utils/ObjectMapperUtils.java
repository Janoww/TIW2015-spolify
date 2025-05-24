package it.polimi.tiw.projects.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ObjectMapperUtils {

    private static final ObjectMapper MAPPER = createObjectMapper();

    private ObjectMapperUtils() {
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        // This will make them serialize as a string (e.g., ISO 8601)
        // instead of a numeric timestamp (milliseconds since epoch).
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
