package com.github.kleonaut.network_beetle;

public class App
{
    public static final String NAME = "NetworkBeetle";

    public App(boolean isMinimized)
    {
        Record record = new Record();

        ModePublisher modePublisher = new ModePublisher(record);
        PowerPublisher powerPublisher = new PowerPublisher();
        DisposePublisher disposePublisher = new DisposePublisher();

        MainWindow window = new MainWindow(powerPublisher, record);
        Tray tray = new Tray(window, powerPublisher, disposePublisher);
        NetProfileSwitcher netSwitcher = new NetProfileSwitcher(powerPublisher);

        // objects will switch mode in this order
        modePublisher.add(window);
        modePublisher.add(netSwitcher);
        modePublisher.add(tray);

        // objects will be disposed of in this order
        disposePublisher.add(powerPublisher);
        disposePublisher.add(tray);
        disposePublisher.add(window);

        // objects will be powered in this order
        powerPublisher.add(modePublisher);
        powerPublisher.add(tray);
        powerPublisher.add(window);

        powerPublisher.turnOn();
        if (!isMinimized) window.setVisible(true);
    }
}
