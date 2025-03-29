package logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EventLogger {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN  = "\u001B[32m";
    public static final int NO_ID = -2;

    /**
     * Logs an information log. If the id is -2, it is a scheduler or fire incident subsystem.
     *
     * @param clazz
     * @param id
     * @param message
     * @param definitiveEvent
     */
    public static void info(int id, String message, boolean definitiveEvent) {
        String color = definitiveEvent ? GREEN : RESET;
        log(color, "INFO", getCallingClassName(), id, message);
    }

    public static void warn(int id, String message) {
        log(YELLOW, "WARN", getCallingClassName(), id, message);
    }

    public static void error(int id, String message) {
        log(RED, "ERROR", getCallingClassName(), id, message);
    }

    private static void log(String color, String level, String subsystem, int id, String message) {
        String timestamp = LocalTime.now().format(timeFormat);

        // if it's the Scheduler or FireIncidentSubsystem (no ID needed)
        if (id == NO_ID) {
            System.out.printf("%s[%s] [%s] [%s] %s%s%n", color, timestamp, level, subsystem, message, RESET);
            return;
        }
        // for drone-related logs with ID
        System.out.printf("%s[%s] [%s] [%s %d] %s%s%n",
                color,
                timestamp,
                level,
                "DRONE",
                id, message, RESET);
    }


    private static String getCallingClassName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length >= 4) {
            String fullClassName = stack[3].getClassName();
            return fullClassName.substring(fullClassName.lastIndexOf('.') + 1).toUpperCase();
        }
        return "UNKNOWN";
    }
}
