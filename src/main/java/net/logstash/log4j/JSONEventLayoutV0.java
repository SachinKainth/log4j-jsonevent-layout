package net.logstash.log4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.logstash.log4j.data.HostData;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class JSONEventLayoutV0 extends Layout {

    private boolean locationInfo;

    private final String hostname = new HostData().getHostName();

    private HashMap<String, Object> fieldData;

    public static final DateTimeFormatter ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static String dateFormat(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDateTime localDate = instant.atZone(zoneId).toLocalDateTime();
        return localDate.format(ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayoutV0() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayoutV0(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String format(LoggingEvent loggingEvent) {
        String threadName = loggingEvent.getThreadName();
        long timestamp = loggingEvent.getTimeStamp();
        fieldData = new HashMap<>();
        HashMap<String, Object> exceptionInformation = new HashMap<>();
        Map mdc = loggingEvent.getProperties();
        String ndc = loggingEvent.getNDC();

        JSONObject logstashEvent = new JSONObject();

        logstashEvent.put("@source_host", hostname);
        logstashEvent.put("@message", loggingEvent.getRenderedMessage());
        logstashEvent.put("@timestamp", dateFormat(timestamp));

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                exceptionInformation.put("exception_class", throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put("exception_message", throwableInformation.getThrowable().getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = String.join("\n", throwableInformation.getThrowableStrRep());
                exceptionInformation.put("stacktrace", stackTrace);
            }
            addFieldData("exception", exceptionInformation);
        }

        if (locationInfo) {
            LocationInfo info = loggingEvent.getLocationInformation();
            addFieldData("file", info.getFileName());
            addFieldData("line_number", info.getLineNumber());
            addFieldData("class", info.getClassName());
            addFieldData("method", info.getMethodName());
        }

        addFieldData("loggerName", loggingEvent.getLoggerName());
        addFieldData("mdc", mdc);
        addFieldData("ndc", ndc);
        addFieldData("level", loggingEvent.getLevel().toString());
        addFieldData("threadName", threadName);

        logstashEvent.put("@fields", fieldData);
        return logstashEvent.toString() + "\n";
    }

    public boolean ignoresThrowable() {
        return false;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void activateOptions() {
    }

    private void addFieldData(String keyname, Object keyval) {
        if (null != keyval) {
            fieldData.put(keyname, keyval);
        }
    }
}
