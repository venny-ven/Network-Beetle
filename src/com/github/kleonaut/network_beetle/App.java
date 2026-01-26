package com.github.kleonaut.network_beetle;

public class App
{
    public static final String NAME = "NetworkBeetle";
    public static final int MAX_LOG_LENGTH = 200;
    public static final int VERIFICATION_DELAY = 5000;
    public static final int SEARCH_DELAY = 2000;

    public App(boolean isMinimized)
    {
        Record record = new Record();

        ModePublisher modePublisher = new ModePublisher(record);
        PowerPublisher powerPublisher = new PowerPublisher();
        DisposePublisher disposePublisher = new DisposePublisher();

        MainWindow mainWindow = new MainWindow(powerPublisher, record);
        Tray tray = new Tray(mainWindow, powerPublisher, disposePublisher);
        NetProfileSwitcher netSwitcher = new NetProfileSwitcher(powerPublisher);

        // objects will switch mode in this order
        modePublisher.add(mainWindow);
        modePublisher.add(netSwitcher);
        modePublisher.add(tray);

        // objects will be disposed of in this order
        disposePublisher.add(powerPublisher);
        disposePublisher.add(tray);
        disposePublisher.add(mainWindow);

        // objects will be powered in this order
        powerPublisher.add(modePublisher);
        powerPublisher.add(tray);
        powerPublisher.add(mainWindow);

        if (isMinimized)
        {
            // Auto-start if running minimized
            powerPublisher.turnOn();
        } else
        {
            // Splash screen brings up the main window after a few seconds
            SplashScreen splashScreen = new SplashScreen(mainWindow);
            splashScreen.beginSplash();
        }
    }
}
