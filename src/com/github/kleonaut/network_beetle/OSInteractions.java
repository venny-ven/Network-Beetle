package com.github.kleonaut.network_beetle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;


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
        List<String> tasks = new ArrayList<>();
        ProcessHandle[] handles = getProcessHandles();
        for (ProcessHandle handle : handles)
            tasks.add(taskOfHandle(handle));
        return tasks;
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

    private static ProcessHandle[] getProcessHandles()
    {
        return ProcessHandle.allProcesses()
                // processes that have a 'command' property (executable path)
                .filter(handle -> handle.info().command().isPresent())
                // processes that are not in 'C:\Windows\' directory
                .filter(handle -> !winMatcher.reset(handle.info().command().get()).find())
                // convert stream to array
                .toArray(ProcessHandle[]::new);
    }

    private static String taskOfHandle(ProcessHandle handle)
    {
        // supply full executable path to matcher to parse out just the executable name
        exeMatcher.reset(handle.info().command().get());
        if (exeMatcher.find())
            // matcher.group() is the matching string
            return exeMatcher.group().toLowerCase(Locale.ENGLISH);
        throw new MatchException("Match not found", null);
    }

    private static ProcessHandle handleOf(String task) throws Exception
    {
        ProcessHandle[] handles = getProcessHandles();
        for (ProcessHandle handle : handles)
            if (taskOfHandle(handle).equals(task))
                return handle;
        throw new Exception("Handle with executable name "+task+" not found");
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
        if (profile == NetProfile.STAY)
        {
            MainWindow.addToLog("Remaining on the same network");
            return;
        }
        try {
            if (profile == NetProfile.DISCONNECT)
            {
                Runtime.getRuntime().exec("netsh wlan disconnect");
                MainWindow.addToLog("Disconnecting from the Internet");
            }
            else
            {
                Runtime.getRuntime().exec("netsh wlan connect name=\"" + profile.name() + "\"");
                MainWindow.addToLog("Connecting to "+profile.name());
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    // TODO: make this work with hidden networks too
    public static List<String> fetchNearbyNetworks()
    {
        return runAndParse("netsh wlan show networks", Regex.SSID);
    }

    public static List<NetProfile> fetchAllNetworkProfiles()
    {
        List<String> results = runAndParse("netsh wlan show profiles", Regex.ALL_PROFILES);
        List<NetProfile> profiles = new ArrayList<>();
        for (String item : results)
            profiles.add(NetProfile.get(item));
        return profiles;
    }

    public static NetProfile fetchNowNetworkProfile()
    {
        List<String> results = runAndParse("netsh wlan show interfaces", Regex.PROFILE);
        if (results.isEmpty()) return NetProfile.DISCONNECT;
        else return NetProfile.get(results.getFirst());
    }

    public static void scanNearbyNetworks()
    {
        try {
            Runtime.getRuntime().exec("netsh wlan show interfaces");
        } catch (IOException e) { throw new RuntimeException(e); }
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

    private static List<String> runAndParse(String command, Regex rgx)
    {
        List<String> results = new ArrayList<>();
        try {
            Process netshProcess = Runtime.getRuntime().exec(command);
            try (
                    InputStream byteIn = netshProcess.getInputStream();
                    Scanner scanner = new Scanner(byteIn).useDelimiter(rgx.NEWLINE.get());
            ) {
                // search for something like "Profile : " pattern with no limit (horizon=0)
                while (scanner.findWithinHorizon(rgx.get(), 0) != null)
                    results.add(scanner.next().trim());
            }
        } catch (IOException e) { throw new RuntimeException(e); }
        return List.copyOf(results);
    }



    // ================================== REGISTRY FUNCTIONS ==================================
    // Writing to registry and reading from it

    public static void setIsLaunchedOnStartup(boolean flag)
    {
        if (flag)
        {
            try {
                File jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                String path = jar.getAbsolutePath();
                Runtime.getRuntime().exec("reg add HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v NetworkBeetle /d \"\\\"" + path + "\\\"\" /f");
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            try {
                Runtime.getRuntime().exec("reg delete HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run /v NetworkBeetle /f");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
