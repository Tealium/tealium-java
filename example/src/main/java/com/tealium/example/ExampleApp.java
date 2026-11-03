package com.tealium.example;

import com.tealium.DataManager;
import com.tealium.DataManager.EventType;
import com.tealium.Tealium;
import com.tealium.Tealium.DispatchCallback;
import com.tealium.Udo;
import com.tealium.UdoSerializationException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small command-line example that exercises the Tealium Java SDK against
 * real Tealium Collect. See example/README.md for usage.
 */
public final class ExampleApp {

    private ExampleApp() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        GlobalOptions options;
        try {
            options = GlobalOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            return 2;
        }

        String[] rest = options.remainingArgs;
        if (rest.length == 0) {
            printUsage(System.out);
            return 2;
        }

        String subcommand = rest[0];
        String[] subArgs = Arrays.copyOfRange(rest, 1, rest.length);

        if ("help".equals(subcommand)) {
            printUsage(System.out);
            return 0;
        }

        Tealium tealium = options.buildTealium();

        try {
            switch (subcommand) {
                case "track":
                    return trackCommand(tealium, EventType.ACTIVITY, subArgs);
                case "view":
                    return trackCommand(tealium, EventType.VIEW, subArgs);
                case "persist":
                    return persistCommand(tealium, subArgs);
                case "session":
                    return sessionCommand(tealium, subArgs);
                default:
                    System.err.println("Unknown command: " + subcommand);
                    printUsage(System.err);
                    return 2;
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            return 2;
        } catch (UdoSerializationException e) {
            System.err.println("Failed to serialize data: " + e.getMessage());
            return 1;
        }
    }

    // =========================================================================
    // COMMANDS
    // =========================================================================

    private static int trackCommand(Tealium tealium, String eventType, String[] args)
            throws UdoSerializationException {

        if (args.length == 0) {
            System.err.println("Expected a title for the event.");
            printUsage(System.err);
            return 2;
        }

        String title = args[0];
        Udo eventData = parseKeyValueArgs(Arrays.copyOfRange(args, 1, args.length));

        final boolean[] successHolder = new boolean[1];
        final Map<String, Object>[] infoHolder = new Map[1];
        final String[] errorHolder = new String[1];

        tealium.track(eventType, title, eventData, new DispatchCallback() {
            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {
                successHolder[0] = success;
                infoHolder[0] = info;
                errorHolder[0] = error;
            }
        });

        System.out.println("success=" + successHolder[0]);
        if (errorHolder[0] != null) {
            System.out.println("error=" + errorHolder[0]);
        }
        printInfo(infoHolder[0]);

        return successHolder[0] ? 0 : 1;
    }

    private static int persistCommand(Tealium tealium, String[] args) throws UdoSerializationException {
        if (args.length == 0) {
            System.err.println("Expected a persist subcommand: show, add, or delete.");
            printUsage(System.err);
            return 2;
        }

        String action = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "show": {
                Udo persistentData = tealium.getDataManager().getPersistentData();
                System.out.println(persistentData.toJson());
                return 0;
            }
            case "add": {
                if (rest.length == 0) {
                    System.err.println("Expected one or more key=value pairs.");
                    printUsage(System.err);
                    return 2;
                }
                Udo additions = parseKeyValueArgs(rest);
                tealium.getDataManager().addPersistentData(additions);
                System.out.println("Added: " + additions.keySet());
                return 0;
            }
            case "delete": {
                if (rest.length == 0) {
                    System.err.println("Expected one or more keys to delete.");
                    printUsage(System.err);
                    return 2;
                }
                List<String> keys = Arrays.asList(rest);
                tealium.getDataManager().deletePersistentData(keys);
                System.out.println("Deleted: " + keys);
                return 0;
            }
            default:
                System.err.println("Unknown persist subcommand: " + action);
                printUsage(System.err);
                return 2;
        }
    }

    private static int sessionCommand(Tealium tealium, String[] args) {
        if (args.length == 0) {
            System.err.println("Expected a session subcommand: show or reset.");
            printUsage(System.err);
            return 2;
        }

        switch (args[0]) {
            case "show":
                System.out.println("session_id=" + tealium.getDataManager().getSessionId());
                return 0;
            case "reset":
                System.out.println("session_id=" + tealium.getDataManager().resetSessionId());
                return 0;
            default:
                System.err.println("Unknown session subcommand: " + args[0]);
                printUsage(System.err);
                return 2;
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Parse "key=value" arguments into a Udo. A key repeated across multiple
     * arguments (e.g. "a=1 a=2") becomes a single List&lt;String&gt; value.
     */
    private static Udo parseKeyValueArgs(String[] pairs) {
        Map<String, List<String>> collectedValues = new LinkedHashMap<String, List<String>>();

        for (String pair : pairs) {
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex <= 0) {
                throw new IllegalArgumentException("Expected key=value but got: " + pair);
            }

            String key = pair.substring(0, separatorIndex);
            String value = pair.substring(separatorIndex + 1);

            List<String> values = collectedValues.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                collectedValues.put(key, values);
            }
            values.add(value);
        }

        Udo udo = new Udo();
        for (Map.Entry<String, List<String>> entry : collectedValues.entrySet()) {
            List<String> values = entry.getValue();
            if (values.size() == 1) {
                udo.put(entry.getKey(), values.get(0));
            } else {
                udo.put(entry.getKey(), values);
            }
        }
        return udo;
    }

    private static void printInfo(Map<String, Object> info) throws UdoSerializationException {
        if (info == null) {
            return;
        }

        Object dispatchService = info.get(DataManager.InfoKey.DISPATCH_SERVICE);
        if (dispatchService != null) {
            System.out.println("dispatch_service=" + dispatchService);
        }

        Object encodedUrl = info.get(DataManager.InfoKey.ENCODED_URL);
        if (encodedUrl != null) {
            System.out.println("encoded_url=" + encodedUrl);
        }

        Object payload = info.get(DataManager.InfoKey.PAYLOAD);
        if (payload instanceof Udo) {
            System.out.println("payload=" + ((Udo) payload).toJson());
        } else if (payload != null) {
            System.out.println("payload=" + payload);
        }

        Object responseHeaders = info.get(DataManager.InfoKey.RESPONSE_HEADERS);
        if (responseHeaders != null) {
            System.out.println("response_headers=" + responseHeaders);
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: run.sh [global options] <command> [args]");
        out.println();
        out.println("Global options:");
        out.println("  --account <name>      Tealium account (default: tealiummobile)");
        out.println("  --profile <name>      Tealium profile (default: demo)");
        out.println("  --env <name>          Tealium environment (default: dev)");
        out.println("  --datasource <id>     Tealium datasource (optional)");
        out.println("  --timeout <ms>        Dispatch timeout in milliseconds (optional)");
        out.println("  --log-level <level>   VERBOSE, WARNINGS, ERRORS, or NONE (default: VERBOSE)");
        out.println();
        out.println("Commands:");
        out.println("  track <title> [key=value ...]   Send an activity event");
        out.println("  view <title> [key=value ...]    Send a view event");
        out.println("  persist show                    Print persistent data as JSON");
        out.println("  persist add key=value ...       Add persistent data");
        out.println("  persist delete key ...          Remove persistent data keys");
        out.println("  session show                    Print the current session id");
        out.println("  session reset                   Reset the session id");
        out.println("  help                             Print this message");
    }

    // =========================================================================
    // GLOBAL OPTIONS
    // =========================================================================

    private static final class GlobalOptions {
        String account = "tealiummobile";
        String profile = "demo";
        String environment = "dev";
        String datasource = null;
        Integer timeoutMillis = null;
        com.tealium.LogLevel logLevel = com.tealium.LogLevel.VERBOSE;
        String[] remainingArgs = new String[0];

        static GlobalOptions parse(String[] args) {
            GlobalOptions options = new GlobalOptions();

            int index = 0;
            while (index < args.length && args[index].startsWith("--")) {
                String flag = args[index];
                switch (flag) {
                    case "--account":
                        options.account = requiredValue(args, flag, ++index);
                        break;
                    case "--profile":
                        options.profile = requiredValue(args, flag, ++index);
                        break;
                    case "--env":
                        options.environment = requiredValue(args, flag, ++index);
                        break;
                    case "--datasource":
                        options.datasource = requiredValue(args, flag, ++index);
                        break;
                    case "--timeout":
                        options.timeoutMillis = parseTimeout(requiredValue(args, flag, ++index));
                        break;
                    case "--log-level":
                        options.logLevel = parseLogLevel(requiredValue(args, flag, ++index));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown global option: " + flag);
                }
                index++;
            }

            options.remainingArgs = Arrays.copyOfRange(args, index, args.length);
            return options;
        }

        Tealium buildTealium() {
            Tealium.Builder builder = new Tealium.Builder(account, profile)
                    .setEnvironment(environment)
                    .setLogLevel(logLevel);
            if (datasource != null) {
                builder.setDatasource(datasource);
            }
            if (timeoutMillis != null) {
                builder.setTimeout(timeoutMillis);
            }
            return builder.build();
        }

        private static String requiredValue(String[] args, String flag, int index) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return args[index];
        }

        private static int parseTimeout(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid --timeout value: " + value);
            }
        }

        private static com.tealium.LogLevel parseLogLevel(String value) {
            try {
                return com.tealium.LogLevel.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid --log-level value: " + value + " (expected VERBOSE, WARNINGS, ERRORS, or NONE)");
            }
        }
    }
}
