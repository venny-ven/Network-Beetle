package com.github.kleonaut.network_beetle;

import javax.swing.Timer;

public class NetProfileSwitcher implements ModeObserver
{
    private final Timer verificationTimer;
    private Mode expectedMode;
    private final PowerPublisher powerPublisher;
    private int attemptCount = 0;

    public NetProfileSwitcher(PowerPublisher powerPublisher)
    {
        verificationTimer = new Timer(App.VERIFY_CONNECTION_DELAY, e -> verify());
        verificationTimer.setRepeats(false);
        this.powerPublisher = powerPublisher;
    }

    private void verify()
    {
        if (OSInteractions.fetchNowNetworkProfile() == expectedMode.netProfile())
        {
            MainWindow.addToLog("Successfully connected to " + expectedMode.netProfile().name());
            return;
        }

        MainWindow.addToLog("Failure to connect to " + expectedMode.netProfile().name());

        if (attemptCount >= App.MAX_RECONNECT_ATTEMPTS) {
            MainWindow.addToLog("Maximum reconnect attempts reached");
            attemptCount = 0;
            powerPublisher.turnOff();
        }
        else {
            attemptCount++;
            MainWindow.addToLog("Trying again...");
            setMode(expectedMode);
        }
    }

    @Override
    public void setMode(Mode mode)
    {
        verificationTimer.stop();
        if (mode.netProfile() != OSInteractions.fetchNowNetworkProfile())
        {
            // Forces a network scan, a Wi-Fi network needs to be scanned before it can be connected to
            OSInteractions.scanNearbyNetworks();

            OSInteractions.setNetworkProfile(mode.netProfile());
            expectedMode = mode;
            if (mode.netProfile() != NetProfile.STAY && mode.netProfile() != NetProfile.DISCONNECT) {
                verificationTimer.restart();
                MainWindow.addToLog("Verifying connection (Attempt #" + (attemptCount + 1) + ")");
            }
        } else {
            if (mode.netProfile() == NetProfile.DISCONNECT)
                MainWindow.addToLog("Already disconnected");
            else if (mode.netProfile() == NetProfile.STAY)
                MainWindow.addToLog("Remaining on the same network");
            else
                MainWindow.addToLog("Already connected to " + mode.netProfile().name());
        }
    }

    @Override
    public void setModeless()
    {
        verificationTimer.stop();
    }
}
