package net.logstash.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.MDC;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jvincent
 * Date: 12/5/12
 * Time: 12:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONEventLayoutV0Test {
    static Logger logger;
    static MockAppenderV0 appender;
    static final String[] logstashFields = new String[]{
            "@message",
            "@source_host",
            "@fields",
            "@timestamp"
    };

    @BeforeAll
    public static void setupTestAppender() {
        appender = new MockAppenderV0(new JSONEventLayoutV0());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappender");
        logger.addAppender(appender);
    }

    @AfterEach
    public void clearTestAppender() {
        NDC.clear();
        appender.clear();
        appender.close();
    }

    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String message = MockAppenderV0.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);

        for (String fieldName : logstashFields) {
            assertTrue(jsonObject.has(fieldName), "Event does not contain field: " + fieldName);
        }
    }

    @Test
    public void testJSONEventLayoutHasFieldLevel() {
        logger.fatal("this is a new test message");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        assertEquals("FATAL", atFields.get("level"), "Log level is wrong");
    }

    @Test
    public void testJSONEventLayoutHasNDC() {
        String ndcData = "json-layout-test";
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        assertEquals(ndcData, atFields.get("ndc"), "NDC is wrong");
    }

    @Test
    public void testJSONEventLayoutHasMDC() {
        MDC.put("foo","bar");
        logger.warn("I should have MDC data in my log");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject mdcData = (JSONObject) atFields.get("mdc");

        assertEquals("bar", mdcData.get("foo"), "MDC is wrong");
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = "shits on fire, yo";
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject exceptionInformation = (JSONObject) atFields.get("exception");

        assertEquals("java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"), "Exception class missing");
        assertEquals(exceptionMessage, exceptionInformation.get("exception_message"), "Exception exception message");
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        assertEquals(this.getClass().getCanonicalName(), atFields.get("class"), "Logged class does not match");
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        assertNotNull(atFields.get("file"), "File value is missing");
    }

    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        assertNotNull(atFields.get("loggerName"), "LoggerName value is missing");
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        assertNotNull(atFields.get("threadName"), "ThreadName value is missing");
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayoutV0 layout = (JSONEventLayoutV0) appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = MockAppenderV0.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        assertFalse(atFields.has("line_number"), "atFields contains line_number value");
        assertFalse(atFields.has("class"), "atFields contains class value");
        assertFalse(atFields.has("file"), "atFields contains file value");
        assertFalse(atFields.has("method"), "atFields contains method value");

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    @Test
    @Disabled
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayoutV0 layout = (JSONEventLayoutV0) appender.getLayout();
        boolean locationInfo = layout.getLocationInfo();
        int iterations = 100000;
        long start, stop;

        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long firstMeasurement = stop - start;

        layout.setLocationInfo(!locationInfo);
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long secondMeasurement = stop - start;

        System.out.println("First Measurement (locationInfo: " + locationInfo + "): " + firstMeasurement);
        System.out.println("Second Measurement (locationInfo: " + !locationInfo + "): " + secondMeasurement);

        // Clean up
        layout.setLocationInfo(!locationInfo);
    }

    @Test
    public void testDateFormat() {
        long timestamp = 1364844991207L;
        assertEquals("2013-04-01T19:36:31.207Z", JSONEventLayoutV0.dateFormat(timestamp), "format does not produce expected output");
    }

    public boolean isValidJsonStrict(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

}
