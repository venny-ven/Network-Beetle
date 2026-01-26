package com.github.kleonaut.network_beetle;

import javax.swing.Timer;

public class NetProfileSwitcher implements ModeObserver
{
    private final Timer verificationTimer;
    private NetProfile expectedProfile;
    private final PowerPublisher powerPublisher;

    public NetProfileSwitcher(PowerPublisher powerPublisher)
    {
        verificationTimer = new Timer(5000, e -> verify());
        verificationTimer.setRepeats(false);
        this.powerPublisher = powerPublisher;
    }

    private void verify()
    {
        if (OSInteractions.fetchNowNetworkProfile() != expectedProfile)
        {
            MainWindow.addToLog("Failure to connect to "+ expectedProfile.name());
            powerPublisher.turnOff();
        }
        else
            MainWindow.addToLog("Successfully connected");
    }

    @Override
    public void setMode(Mode mode)
    {
        verificationTimer.stop();
        if (mode.netProfile() != OSInteractions.fetchNowNetworkProfile())
        {
            // Forces a network scan, a Wi-Fi network needs to be scanned before it can be connected to
            OSInteractions.scanNearbyNetworks();
            MainWindow.addToLog("Scanned nearby networks");

            OSInteractions.setNetworkProfile(mode.netProfile());
            expectedProfile = mode.netProfile();
            if (expectedProfile != NetProfile.STAY && expectedProfile != NetProfile.DISCONNECT) {
                verificationTimer.restart();
                MainWindow.addToLog("Verifying connection");
            }
        }
    }

    @Override
    public void setModeless()
    {
        verificationTimer.stop();
    }
}
