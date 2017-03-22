package hudson.plugins.spotinst.api.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMapper.class);

    private static ObjectMapper jsonMapper = new ObjectMapper();

    public static <T> T fromJson(String content, Class<T> contentClass) {
        T retVal = null;

        try {
            retVal = jsonMapper.readValue(content, contentClass);
        } catch (IOException e) {

            LOGGER.error("Error in parsing json to object", e);
        }

        return retVal;
    }

    public static <T> String toJson(T objectToWrite) {
        String retVal = null;


        try {
            retVal = jsonMapper.writeValueAsString(objectToWrite);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error in writing object to json", e);
        }

        return retVal;
    }
}
