package com.github.kleonaut.network_beetle;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Record
{
    private final int VERSION = 1;
    private final Preferences registry = Preferences.userNodeForPackage(Record.class);

    private List<Mode> modes;
    private boolean isLaunchedOnStartup;

    public Record()
    {
        registry.putInt(versionKey(), VERSION);
        int modeCount = registry.getInt(modeCountKey(), 2);
        List<Mode> list = new ArrayList<>();
        for (int i = 0; i < modeCount; i++)
            list.add(retrieveMode(i));
        modes = List.copyOf(list);
        isLaunchedOnStartup = retrieveIsLaunchedOnStartup();
    }

    // list is ordered with first element as highest priority
    public List<Mode> modes() { return modes; }
    public Mode mode(int index) { return modes.get(index); }
    public Mode defaultMode() { return modes.getLast(); }
    public boolean isLaunchedOnStartup() { return isLaunchedOnStartup; }
    public void toggleLaunchedOnStartup() { setLaunchedOnStartup(!isLaunchedOnStartup); }
    public void setLaunchedOnStartup(boolean flag)
    {
        // Update temporal record variable
        isLaunchedOnStartup = flag;

        // Update user config in registry
        overwriteIsLaunchedOnStartup(flag);

        // Update Startup Apps list.
        // Don't overdo this - only write when the user does it manually
        if (flag)
            OSInteractions.addToStartupApps();
        else
            OSInteractions.removeFromStartupApps();
    }


    public void overwriteModeAt(int index, Mode mode)
    {
        int m = index;

        // overwrite in memory
        List<Mode> list = new ArrayList<>(modes);
        list.set(m, mode);
        modes = List.copyOf(list);

        // save to registry
        registry.put(modeNameKey(m), mode.name());
        registry.put(modeProfileIdKey(m), mode.netProfile().id());
        registry.putInt(modeConditionCountKey(m), mode.conditions().size());
        for (int c = 0; c < mode.conditions().size(); c++)
            registry.put(modeConditionKey(m, c), mode.conditions().get(c));
    }

    private Mode retrieveMode(int index)
    {
        int m = index;

        String name = registry.get(modeNameKey(m), modeNameDefault(m));
        NetProfile profile = NetProfile.get(registry.get(modeProfileIdKey(m), modeProfileIdDefault()));

        List<String> conditions = new ArrayList<>();
        int conditionCount = registry.getInt(modeConditionCountKey(m), 0);
        for (int c = 0; c < conditionCount; c++)
            conditions.add(registry.get(modeConditionKey(m, c), modeConditionDefault()));

        return new Mode(name, profile, conditions);
    }

    private boolean retrieveIsLaunchedOnStartup()
    {
        return Boolean.parseBoolean(registry.get(launchedKey(), "false"));
    }

    private void overwriteIsLaunchedOnStartup(boolean flag)
    {
        registry.put(launchedKey(), Boolean.toString(flag));
    }


    private String versionKey() { return "version"; }
    private String startupKey() { return App.NAME; }
    private String launchedKey() { return "is_launched_on_startup"; }
    private String enabledKey() { return "is_enabled_on_launch"; }
    private String modeCountKey() { return "mode_count"; }
    private String modeNameKey(int modeIndex) { return "mode"+modeIndex+"_name"; }
    private String modeProfileIdKey(int modeIndex) { return "mode"+modeIndex+"_network_profile"; }
    private String modeConditionCountKey(int modeIndex) { return "mode"+modeIndex+"_condition_count"; }
    private String modeConditionKey(int modeIndex, int conditionIndex) { return "mode"+modeIndex+"_condition"+conditionIndex; }
    private String modeNameDefault(int modeIndex) { return (modeIndex == 1) ? "Default Mode" : "Preferrential Mode"; }
    private String modeProfileIdDefault() { return NetProfile.STAY.id(); }
    private String modeConditionDefault() { return "[Error retrieving condition]"; }
}
