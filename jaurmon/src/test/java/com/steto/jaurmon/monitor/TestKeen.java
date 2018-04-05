package com.steto.jaurmon.monitor;

/**
 * Created by stefano on 03/01/15.
 */


import io.keen.client.java.JavaKeenClientBuilder;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenLogging;
import io.keen.client.java.KeenProject;
import org.apache.commons.cli.*;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main class for the sample application.
 *
 * @author Kevin Litwack (kevin@kevinlitwack.com)
 */
class Main {

    ///// PUBLIC STATIC METHODS /////

    /**
     * Executes the application with the given arguments.
     *
     * @param args The arguments.
     */
    public static void main(String[] args) throws Exception {
        // Parse the command line arguments.
        Options options = buildCommandLineOptions();
        CommandLine cmd = parseCommandLine(args, options);

        // If help was requested, just display it and exit.
        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("keen-sample [OPTION]...", options);
            System.exit(0);
        }

        // Build a Java Keen client.
        KeenClient client = new JavaKeenClientBuilder().build();

        // If logging was specified, enable it.
        if (cmd.hasOption("logging")) {
            KeenLogging.enableLogging();
        }

        // If debug mode was specified, enable it.
        if (cmd.hasOption("debug")) {
            client.setDebugMode(true);
        }

        // Get the number of events to send from the arguments, or use the default.
        int numEvents = DEFAULT_NUM_EVENTS;
        if (cmd.hasOption("num-events")) {
            numEvents = Integer.parseInt(cmd.getOptionValue("num-events"));
        }

        // Get the number of threads to use to send events.
        int numThreads = DEFAULT_NUM_THREADS;
        if (cmd.hasOption("num-threads")) {
            numThreads = Integer.parseInt(cmd.getOptionValue("num-threads"));
        }

        // Get properties defining how to send the events.
        boolean synchronous = !(cmd.hasOption("async"));
        boolean batch = cmd.hasOption("batch");

        // Construct an instance of the Main class.
        Main main = new Main(client, synchronous, batch, numEvents, numThreads);

        // Set the default project based on system properties.
        main.setDefaultProject(System.getProperties());

        // Execute the program.
        main.execute();
    }

    ///// PUBLIC METHODS //////

    public void execute() throws InterruptedException {
        // Print out the parameters of this execution.
        System.out.printf(
                Locale.US, "Sending %d events from %d threads using %s requests and %sbatching\n",
                numEvents, numThreads, (synchronous ? "synchronous" : "asynchronous"),
                (batch ? "" : "no "));

        // Seed the random number generator.
        long seed = System.currentTimeMillis();
        rng.setSeed(seed);
        System.out.println("Random seed: " + seed);

        // Build a set of tasks to run.
        List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < numThreads; i++) {
            Callable<Integer> eventAdderTask = new EventAdder();
            tasks.add(eventAdderTask);
        }

        System.out.println("Starting workers in thread pool...");

        // Create a thread pool to run the tasks, run them, and then shutdown the pool.
        ExecutorService workers = Executors.newFixedThreadPool(numThreads);
        List<Future<Integer>> futures = workers.invokeAll(tasks);

        System.out.println("Workers starters; awaiting completion");

        for (Future<Integer> future : futures) {
            try {
                Integer eventCount = future.get();
                System.out.printf("Worker finished after adding %d events\n", eventCount);
            } catch (Exception e) {
                System.err.println("Worker exited with exception: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        // If this execution used batching, perform one final synchronous upload to catch any
        // events that may have been added after the last upload due to thread scheduling.
        if (batch) {
            System.out.println("Performing final clean-up sendQueuedEvents");
            client.sendQueuedEvents();
        }

        System.out.println("All workers have exited; shutting down worker ExecutorService");

        workers.shutdown();
        workers.awaitTermination(60, TimeUnit.SECONDS);

        System.out.println("Worker ExecutorService shut down; shutting down publish " +
                "ExecutorService");

        // Shutdown the publish executor service for the Keen client.
        ExecutorService service = (ExecutorService) client.getPublishExecutor();
        service.shutdown();
        service.awaitTermination(30, TimeUnit.SECONDS);

        // Print the expected sum of the "telemCounter" column, which can be used to validate that the
        // Keen server received all of the events.
        System.out.println("Expected sum of counters: " + getExpectedSum());
        System.out.println("You can verify this sum via the web workbench, or " +
                "with the following curl command:");
        KeenProject defaultProject = client.getDefaultProject();
        System.out.printf("  curl \"https://api.keen.io/3.0/projects/%s/queries/sum?" +
                        "api_key=<read key>&event_collection=sample-app&target_property=telemCounter\"",
                defaultProject.getProjectId()
        );
    }

    public void setDefaultProject(Properties properties) {
        String projectId = properties.getProperty("io.keen.project.id");
        String writeKey = properties.getProperty("io.keen.project.write_key");
        String readKey = properties.getProperty("io.keen.project.read_key");
        KeenProject project = new KeenProject(projectId, writeKey, readKey);
        client.setDefaultProject(project);
    }

    ///// PRIVATE TYPES /////

    private class EventAdder implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            int numEventsAdded = 0;
            int nextEventCounter;
            while ((nextEventCounter = counter.getAndIncrement()) <= numEvents) {
                addEvent(nextEventCounter);
                numEventsAdded++;
                Thread.sleep((long) rng.nextInt(MAX_SLEEP_INTERVAL_MILLIS));
            }
            return numEventsAdded;
        }

    }

    ///// PRIVATE CONSTANTS /////

    private static final String COLLECTION_NAME = "sample-app";
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_NUM_EVENTS = 5;
    private static final int DEFAULT_NUM_THREADS = 1;
    private static final int MAX_SLEEP_INTERVAL_MILLIS = 50;

    ///// PRIVATE FIELDS /////

    private final KeenClient client;
    private final boolean synchronous;
    private final boolean batch;
    private final int numEvents;
    private final int numThreads;
    private final AtomicInteger counter;
    // TODO: Make seed configurable.
    private final Random rng;

    ///// PRIVATE CONSTRUCTORS /////

    private Main(KeenClient client, boolean synchronous, boolean batch, int numEvents, int numThreads) {
        this.client = client;
        this.synchronous = synchronous;
        this.batch = batch;
        this.numEvents = numEvents;
        this.numThreads = numThreads;
        counter = new AtomicInteger(1);
        rng = new Random();
    }

    ///// PRIVATE STATIC METHODS /////

    private static Options buildCommandLineOptions() {
        Option help = new Option("h", "help", false, "Print this message");
        Option async = new Option("a", "async", false, "Use asynchronous methods");
        Option batch = new Option("b", "batch", false, "Use queueing and batch posting");
        Option numEvents = OptionBuilder.withLongOpt("num-events")
                .withDescription("Number of events to post")
                .hasArg()
                .withArgName("COUNT")
                .create();
        Option numThreads = OptionBuilder.withLongOpt("num-threads")
                .withDescription("Number of threads from which to post events")
                .hasArg()
                .withArgName("COUNT")
                .create();
        Option logging = new Option("l", "logging", false, "Enable Keen logging");
        Option debug = new Option("d", "debug", false, "Enable debug mode");

        // Add all of the options to an Objects object and return it.
        Options options = new Options();
        options.addOption(help);
        options.addOption(async);
        options.addOption(batch);
        options.addOption(numEvents);
        options.addOption(numThreads);
        options.addOption(logging);
        options.addOption(debug);
        return options;
    }

    private static CommandLine parseCommandLine(String[] args,
                                                Options options) throws ParseException {
        CommandLineParser parser = new BasicParser();
        return parser.parse(options, args, true);
    }

    private static int sumOfFirstNIntegers(int n) {
        return (n * (n + 1)) / 2;
    }

    private static String generateString(Random rng, int minLength, int maxLength) {
        int length = rng.nextInt(maxLength - minLength) + minLength;
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            // Generate a random ASCII character between space (0x20) and ~ (0x7E).
            text[i] = (char) (' ' + rng.nextInt('~' - ' '));
        }
        return new String(text);
    }

    ///// PRIVATE METHODS /////

    private void addEvent(int n) {
        Map<String, Object> event = buildEvent(n);

        if (batch) {
            // Queue the event. This is always done synchronously.
            client.queueEvent(COLLECTION_NAME, event);

            // Periodically issue a batch post.
            if (n % DEFAULT_BATCH_SIZE == 0) {
                if (synchronous) {
                    client.sendQueuedEvents();
                } else {
                    client.sendQueuedEventsAsync();
                }
            }
        } else {
            if (synchronous) {
                client.addEvent(COLLECTION_NAME, event);
            } else {
                client.addEventAsync(COLLECTION_NAME, event);
            }
        }
    }

    private Map<String, Object> buildEvent(int n) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("telemCounter", n);
        event.put("string", generateString(rng, 3, 20));
        return event;
    }

    private int getExpectedSum() {
        return sumOfFirstNIntegers(numEvents);
    }

}

public class TestKeen {

    String projectId = "54a7cd0246f9a77abeac017f";
    String writeKey = "935444c3f088bbf5bc0ea2d2a3a47a395cc7f803752e7c1443af120e30f7a1ebe22587af54b43601ff99a5eb550e4d17889bb28514a0d6700db89aa38e065d3fd4bb05c48bb6454b90a1e9c0b034ea6824d86c0a677a9d90ca48df98a52a426343076e9b42323eb50f4f0cb211ed1601";
    String readKey = "14979a910a0acf5361b9fce43cd908074fd9de4df4de8d6afcc11001dbc381b251a4a36aa42d94e4f276e55a74fd6c0bf75ae18c496bac226a6803ff31321e805906fa7fe230e2b5679f450fe0b92fd28fab2db5ac1d726eda935fd95b8384831c51de4a42e56e8a31cdf87b46dc8070";
    KeenProject project = new KeenProject(projectId, writeKey, readKey);
    KeenClient client = new JavaKeenClientBuilder().build();

    @Test
    public void should() {
        String collectionName="ImpiantoViaTalDeiTali";
        client.setDefaultProject(project);
        Map<String, Object> event = new HashMap<>();
        event.put("start", System.currentTimeMillis());
        client.addEvent(collectionName, event);


    }


}
