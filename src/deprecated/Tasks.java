package deprecated;

import com.github.kleonaut.network_beetle.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

// not thread safe because of Matcher
// do not call from worker threads
public class Tasks
{
    // ignore the idea of hooking into the process; not relevant rn and might not require caching handle reference at all
    // TODO: try command().startInstant() to filter out all tasks that have no start time
        // such tasks can't be possibly useful; maybe good in combination with sys filter, or instead
    // TODO: use stream.findFirst() to compact all of this
    // TODO: initialize matchers inside fetchTasks function to make this thread safe yet optimized
    private static final Matcher exeMatcher = Regex.EXE_FILE.get().matcher("");
    private static final Matcher winMatcher = Regex.WINDOWS_DIR.get().matcher("");

    private Tasks() {}

    public static List<String> fetch()
    {
        List<String> taskNames = new ArrayList<>();
        ProcessHandle[] handles = getHandles();
        for (ProcessHandle handle : handles)
            taskNames.add(taskNameOf(handle));
        return taskNames;
    }

    public static List<String> fetchNoRepeats()
    {
        List<String> tasks = fetch();
        for (int i = 0; i < tasks.size(); i++)
            for (int k = i+1; k < tasks.size(); k++)
                while (k < tasks.size() && tasks.get(i).equals(tasks.get(k)))
                    tasks.remove(k);
        return tasks;
    }

    private static ProcessHandle[] getHandles()
    {
        return ProcessHandle.allProcesses()
                // processes that have a 'command' property (executable path)
                .filter(handle -> handle.info().command().isPresent())
                // processes that are not in 'C:\Windows\' directory
                .filter(handle -> !winMatcher.reset(handle.info().command().get()).find())
                // convert stream to array
                .toArray(ProcessHandle[]::new);
    }

    private static String taskNameOf(ProcessHandle handle)
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
        ProcessHandle[] handles = getHandles();
        for (ProcessHandle handle : handles)
            if (taskNameOf(handle).equals(task))
                return handle;
        throw new Exception("Handle with executable name "+task+" not found");
    }
}
