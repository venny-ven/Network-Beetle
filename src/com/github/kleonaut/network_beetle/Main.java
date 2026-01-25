package com.github.kleonaut.network_beetle;

import java.util.Arrays;

public class Main
{
    // TODO: fix changing pane sizes issue in ModeDialog
    // TODO: erase overviewframe log to avoid memory hog
    // TODO: create a better, transparent icon
    // TODO: when opening tray icon popup the app pauses; occurs because AWT; use a Swing hack

    public static void main(String[] args) {
        boolean isMinimized = Arrays.asList(args).contains("--minimized");
        new App(isMinimized);
    }
}
