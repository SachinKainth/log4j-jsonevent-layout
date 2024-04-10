package net.logstash.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.*;

import java.util.HashMap;
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
public class JSONEventLayoutV1Test {
    static Logger logger;
    static MockAppenderV1 appender;
    static final String userFieldsSingle = "field1:value1";
    static final String userFieldsMulti = "field2:value2,field3:value3";
    static final String userFieldsSingleProperty = "field1:propval1";

    static final String[] logstashFields = new String[]{
            "message",
            "source_host",
            "@timestamp",
            "@version"
    };

    @BeforeAll
    public static void setupTestAppender() {
        appender = new MockAppenderV1(new JSONEventLayoutV1());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappenderv1");
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
        String message = MockAppenderV1.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
    }

    @Test
    public void testJSONEventLayoutHasUserFieldsFromProps() {
        System.setProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY, userFieldsSingleProperty);
        logger.info("this is an info message with user fields");
        String message = MockAppenderV1.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
        JSONObject jsonObject = new JSONObject(message);
        assertTrue(jsonObject.has("field1"), "Event does not contain field 'field1'");
        assertEquals("propval1", jsonObject.get("field1"), "Event does not contain value 'value1'");
        System.clearProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY);
    }

    @Test
    public void testJSONEventLayoutHasUserFieldsFromConfig() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = MockAppenderV1.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
        JSONObject jsonObject = new JSONObject(message);
        assertTrue(jsonObject.has("field1"), "Event does not contain field 'field1'");
        assertEquals("value1", jsonObject.get("field1"), "Event does not contain value 'value1'");

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsMulti() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsMulti);

        logger.info("this is an info message with user fields");
        String message = MockAppenderV1.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
        JSONObject jsonObject = new JSONObject(message);
        assertTrue(jsonObject.has("field2"), "Event does not contain field 'field2'");
        assertEquals("value2", jsonObject.get("field2"), "Event does not contain value 'value2'");
        assertTrue(jsonObject.has("field3"), "Event does not contain field 'field3'");
        assertEquals("value3", jsonObject.get("field3"), "Event does not contain value 'value3'");

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsPropOverride() {
        // set the property first
        System.setProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY, userFieldsSingleProperty);

        // set the config values
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = MockAppenderV1.getMessages()[0];
        assertTrue(isValidJsonStrict(message), "Event is not valid JSON");
        JSONObject jsonObject = new JSONObject(message);
        assertTrue(jsonObject.has("field1"), "Event does not contain field 'field1'");
        assertEquals("propval1", jsonObject.get("field1"), "Event does not contain value 'propval1'");

        layout.setUserFields(prevUserData);
        System.clearProperty(JSONEventLayoutV1.ADDITIONAL_DATA_PROPERTY);

    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        for (String fieldName : logstashFields) {
            assertTrue(jsonObject.has(fieldName), "Event does not contain field: " + fieldName);
        }
    }

    @Test
    public void testJSONEventLayoutHasNDC() {
        String ndcData = "json-layout-test";
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);

        assertEquals(ndcData, jsonObject.get("ndc"), "NDC is wrong");
    }

    @Test
    public void testJSONEventLayoutHasMDC() {
        MDC.put("foo", "bar");
        logger.warn("I should have MDC data in my log");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject mdc = (JSONObject) jsonObject.get("mdc");

        assertEquals("bar", mdc.get("foo"), "MDC is wrong");
    }

    @Test
    public void testJSONEventLayoutHasNestedMDC() {
        HashMap<String, String> nestedMdc = new HashMap<>();
        nestedMdc.put("bar","baz");
        MDC.put("foo",nestedMdc);
        logger.warn("I should have nested MDC data in my log");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject mdc = (JSONObject) jsonObject.get("mdc");
        JSONObject nested = (JSONObject) mdc.get("foo");

        assertTrue(mdc.has("foo"), "Event is missing foo key");
        assertEquals("baz", nested.get("bar"), "Nested MDC data is wrong");
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = "shits on fire, yo";
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        JSONObject exceptionInformation = (JSONObject) jsonObject.get("exception");

        assertEquals("java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"), "Exception class missing");
        assertEquals(exceptionMessage, exceptionInformation.get("exception_message"), "Exception exception message");
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);

        assertEquals(this.getClass().getCanonicalName(), jsonObject.get("class"), "Logged class does not match");
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);

        assertNotNull(jsonObject.get("file"), "File value is missing");
    }

    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        assertNotNull(jsonObject.get("logger_name"), "LoggerName value is missing");
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);
        assertNotNull(jsonObject.get("thread_name"), "ThreadName value is missing");
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = MockAppenderV1.getMessages()[0];
        JSONObject jsonObject = new JSONObject(message);

        assertFalse(jsonObject.has("file"), "atFields contains file value");
        assertFalse(jsonObject.has("line_number"), "atFields contains line_number value");
        assertFalse(jsonObject.has("class"), "atFields contains class value");
        assertFalse(jsonObject.has("method"), "atFields contains method value");

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    @Test
    @Disabled
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
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
        assertEquals("2013-04-01T19:36:31.207Z", JSONEventLayoutV1.dateFormat(timestamp), "format does not produce expected output");
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
