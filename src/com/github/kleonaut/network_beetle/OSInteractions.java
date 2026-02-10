package com.github.kleonaut.network_beetle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// Static utility class that acts as a wrapper around Windows Shell commands and other OS interactions.
// Functions in this class perform actions on the operatinng system, like changing the network or writing to registry
// They also read from the shell, like reading processes or networks, cleaning up the output before returning it
//
// Not thread safe because of Matcher
// Do not call from worker threads
public class OSInteractions
{
    // ignore the idea of hooking into the process; not relevant rn and might not require caching handle reference at all
    // TODO: try command().startInstant() to filter out all tasks that have no start time
    // such tasks can't be possibly useful; maybe good in combination with sys filter, or instead
    // TODO: use stream.findFirst() to compact all of this
    // TODO: initialize matchers inside fetchTasks function to make this thread safe yet optimized
    private static final Matcher exeMatcher = Regex.EXE_FILE.get().matcher("");
    private static final Matcher winMatcher = Regex.WINDOWS_DIR.get().matcher("");

    private OSInteractions() {} // Keep static class from being instantiated



    // ================================== TASK FUNCTIONS ==================================
    // Task is a term for a proccess name, like "firefox.exe", it is a String
    // These functions scan for and filter tasks that are currently running
    // The core of the system here is a ProcessHandle class offered by Java
    // A Handle is an object that comes from ProccessHandle class. I filter it and extract the task from it

    public static List<String> fetchTasks()
    {
        // The pattern below uses stream manipulations to make an efficient filter
        // Previously I used successive filter() stream operators
        // But after each successive filter some processes die while my code still thinks they exist
        // This method is more reliable

        // Access a stream, the stream contains a snapshot list of current processes
        return ProcessHandle.allProcesses()
                // flatMap() is stream operation
                // It maps each element to a stream (transforms it into a stream). The stream is defined in { }
                // Then flattens back to a single stream
                // In my case each handle is mapped to a stream of that handle or to an empty stream
                // Depending on whether the handle passed the required checks or not
                .flatMap(handle ->
                {
                    // Extract the path into an Optional. In Java Processes, the path of exe file is called "command"
                    // If path doesn't exist (process started after boot), return empty stream
                    Optional<String> command = handle.info().command();
                    if (command.isEmpty()) return Stream.empty();

                    // Convert path from optional into a string
                    // If path is a C:\Windows\ path, return empty stream
                    String exePath = command.get();
                    if (winMatcher.reset(exePath).find()) return Stream.empty();

                    // Return just the end portion of the path, the .exe file name
                    exeMatcher.reset(exePath);
                    if (!exeMatcher.find()) return Stream.empty(); // Just in case. This condition should never occur
                    return Stream.of(exeMatcher.group().toLowerCase(Locale.ENGLISH));
                })
                // Turn stream into an array
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<String> fetchTasksNoRepeats()
    {
        List<String> tasks = fetchTasks();
        for (int i = 0; i < tasks.size(); i++)
            for (int k = i+1; k < tasks.size(); k++)
                while (k < tasks.size() && tasks.get(i).equals(tasks.get(k)))
                    tasks.remove(k);
        return tasks;
    }



    // ================================== NETWORK FUNCTIONS ==================================
    // Network Profile is a profile saved in the OS containing a network SSID, password, and preferences.
    // These profiles can be revealed with a shell command: netsh wlan show profiles.
    // These functions offer a way to list profiles, connectto them, and disconnect from them.
    // We can also scan for nearby networks, but not connect to them easily.
    // I use nearby networks to filter out profiles, showing only relevant ones.
    // Now-Profile is a term for a profile currently in use.

    public static void setNetworkProfile(NetProfile profile)
    {
        if (profile == NetProfile.STAY) {
            MainWindow.addToLog("Remaining on the current network");
            return;
        }
        if (profile == NetProfile.DISCONNECT) {
            MainWindow.addToLog("Disconnecting from the Internet...");
            MainWindow.addToLog(runAndCollect("netsh","wlan","disconnect"));

        }
        else {
            MainWindow.addToLog("Connecting to "+profile.name()+"...");
            MainWindow.addToLog(runAndCollect("netsh","wlan","connect","name="+profile.name()));
        }
    }

    // TODO: make this work with hidden networks too
    public static List<String> fetchNearbyNetworks()
    {
        return runAndFilter(Regex.SSID, "netsh","wlan","show","networks");
    }

    public static List<NetProfile> fetchAllNetworkProfiles()
    {
        List<String> results = runAndFilter(Regex.ALL_PROFILES, "netsh","wlan","show","profiles");
        List<NetProfile> profiles = new ArrayList<>();
        for (String item : results)
            profiles.add(NetProfile.get(item));
        return profiles;
    }

    public static NetProfile fetchNowNetworkProfile()
    {
        List<String> results = runAndFilter(Regex.PROFILE, "netsh","wlan","show","interfaces");
        if (results.isEmpty()) return NetProfile.DISCONNECT;
        else return NetProfile.get(results.getFirst());
    }

    public static void scanNearbyNetworks()
    {
        MainWindow.addToLog("Scanned nearby networks");
        runAndCollect("netsh","wlan","show","networks");
    }

    public static List<NetProfile> fetchNearbyNetworkProfiles()
    {
        List<NetProfile> nearbyProfiles = new ArrayList<>();

        List<String> networks = fetchNearbyNetworks();
        List<NetProfile> profiles = fetchAllNetworkProfiles();
        for (String network : networks)
            for (NetProfile profile : profiles)
                if (profile.name().equals(network))
                    nearbyProfiles.add(profile);

        return nearbyProfiles;
    }

    private static List<String> runAndFilter(Regex rgx, String... command)
    {
        List<String> results = new ArrayList<>();

        runShell((output) -> // Run lambda that generates a shell process and insert stream operations over its output
        {
            // Read the stream line by line with a Scanner
            try (Scanner scanner = new Scanner(output, StandardCharsets.UTF_8).useDelimiter(Regex.NEWLINE.get()))
            {
                // Search for something like "Profile : " pattern with no limit (horizon=0)
                while (scanner.findWithinHorizon(rgx.get(), 0) != null) {
                    if (scanner.hasNext()) {
                        results.add(scanner.next().trim());
                    }
                }
            }
        }, command); // Command that starts the shell process

        // Turn to immutable list
        return List.copyOf(results);
    }

    private static String runAndCollect(String... command)
    {
        final String[] result = new String[1]; // Lambda requires effectively final variable, I wrap String with an array

        runShell((stream) -> { // Run lambda that generates a shell process and insert stream operations over its output
            String message = new String(stream.readAllBytes(), StandardCharsets.UTF_8); // Turn stream into a string
            result[0] = message.isBlank() ? "Shell reply empty" : "Shell reply: "+message;
        }, command); // Command that starts the shell process

        return result[0];
    }

    private static void runShell(StreamConsumer consumer, String... command)
    {
        try {
            ProcessBuilder builder = new ProcessBuilder(command); // Create a process factory with my command
            builder.redirectErrorStream(true); // Merge stderr into stdout to create one stream, prevents reading deadlocks
            Process shellProcess = builder.start(); // Execute shell command - start the process

            // Receive custom stream operations here
            try (InputStream stream = shellProcess.getInputStream()) {
                consumer.accept(stream);
            }

            int exitCode = shellProcess.waitFor(); // Wait for process termination. This line suspends main thread
            // Exit code is logged only when process terminated wrong
            if (exitCode != 0) {
                MainWindow.addToLog("!!! Shell exit code: " + exitCode + " !!!");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Occurs if waitFor() fails, therefore neccessary
            throw new RuntimeException(e);
        }
    }

    // This is a Consumer pattern but custom and capable of throwing an exception
    @FunctionalInterface
    private interface StreamConsumer {
        void accept(InputStream inputStream) throws IOException;
    }



    // ================================== REGISTRY FUNCTIONS ==================================
    // Writing to and deleting from registry

    public static void addToStartupApps()
    {
        try {
            String pathToExeFile = String.valueOf(Path.of(System.getProperty("java.home")).resolveSibling(App.NAME + ".exe"));
            if (pathToExeFile.endsWith(".exe")) // Only add to registry if the app is in .exe form
                Runtime.getRuntime().exec("reg add HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v "+App.NAME+" /d \"\\\"" + pathToExeFile + "\\\" --minimized\" /f");
            MainWindow.addToLog("Added to startup apps");
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void removeFromStartupApps()
    {
        try {
            Runtime.getRuntime().exec("reg delete HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v "+App.NAME+" /f");
            MainWindow.addToLog("Removed from startup apps");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
