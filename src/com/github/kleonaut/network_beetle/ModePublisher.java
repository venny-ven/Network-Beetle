package com.github.kleonaut.network_beetle;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class ModePublisher implements PowerObserver
{
    private final List<ModeObserver> observers = new ArrayList<>();
    private final Record record;
    private final Timer timer;
    private Mode nowMode = null;

    ModePublisher(Record record)
    {
        this.record = record;
        timer = new Timer(App.SEARCH_PROCESSES_DELAY, e -> update());
        timer.setInitialDelay(0);
        timer.setRepeats(false);
    }

    public void add(ModeObserver observer) { observers.add(observer); }

    public void update()
    {
        List<String> tasks = OSInteractions.fetchTasks();
        search:
        {
            for (Mode mode : record.modes())
                for (String condition : mode.conditions())
                    for (String task : tasks)
                        if (condition.equals(task))
                        {
                            if (nowMode != mode)
                            {
                                MainWindow.addToLog(condition + " detected");
                                publish(mode);
                            }
                            break search; // If mode is already correct, just break search and not publish changes
                        }
            if (nowMode != record.defaultMode())
            {
                MainWindow.addToLog("No condition detected");
                publish(record.defaultMode());
            }
        }
        timer.setInitialDelay(App.SEARCH_PROCESSES_DELAY);
        timer.start();
    }

    private void publish(Mode mode)
    {
        nowMode = mode;
        if (mode == null)
            for (ModeObserver observer : observers) observer.setModeless();
        else
        {
            MainWindow.addToLog("Switched to " + mode.name());
            for (ModeObserver observer : observers) observer.setMode(mode);

        }
    }

    @Override
    public void setPowered(boolean flag)
    {
        if (flag) {
            timer.start();
            MainWindow.addToLog("Started search");
        }
        else
        {
            timer.stop();
            publish(null);
            MainWindow.addToLog("Stopped search");
        }
    }

    @Override
    public void setPowerBlocked(boolean flag) { }
}
