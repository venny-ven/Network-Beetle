package com.github.kleonaut.network_beetle;

import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

public class SplashScreen {

    private final JWindow splashWindow = new JWindow();
    private final Timer timer;
    private final MainWindow mainWindow;

    public SplashScreen(MainWindow mainWindow) {

        this.mainWindow = mainWindow;
        JLabel label = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("resources/splashLogo.png")));
        splashWindow.getContentPane().add(label);
        splashWindow.pack();
        splashWindow.setLocationRelativeTo(null);

        timer = new Timer(3000, e -> endSplash());
        timer.setInitialDelay(0);
        timer.setRepeats(false);
    }

    public void beginSplash()
    {
        splashWindow.setVisible(true);
        timer.setInitialDelay(3000);
        timer.start();
    }

    private void endSplash()
    {
        timer.stop();
        mainWindow.setVisible(true);
        splashWindow.dispose();
    }
}
