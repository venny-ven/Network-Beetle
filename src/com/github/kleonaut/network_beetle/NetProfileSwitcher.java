package com.github.kleonaut.network_beetle;

import javax.swing.Timer;

public class NetProfileSwitcher implements ModeObserver
{
    private final Timer verificationTimer;
    private NetProfile expectedProfile;
    private final PowerPublisher powerPublisher;

    public NetProfileSwitcher(PowerPublisher powerPublisher)
    {
        verificationTimer = new Timer(App.VERIFICATION_DELAY, e -> verify());
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
            MainWindow.addToLog("Successfully connected to " + expectedProfile.name());
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
            expectedProfile = mode.netProfile();
            if (expectedProfile != NetProfile.STAY && expectedProfile != NetProfile.DISCONNECT) {
                verificationTimer.restart();
                MainWindow.addToLog("Verifying connection");
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
