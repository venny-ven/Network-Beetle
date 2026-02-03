package com.github.kleonaut.network_beetle;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainWindow implements PowerObserver, DisposeObserver, ModeObserver
{
    // TODO: figure out how to handle JDK bug 8221452: setMinimumSize() does not take into account display scaling %
        // current solution: have no miminum size
    private final JFrame frame = new JFrame(App.NAME);
    private final JToggleButton powerButton = new JToggleButton("Enable");
    private static final JTextArea logField = new JTextArea(7, 25);
    private static int logNumber = 0;
    private final ButtonGroup modeButtons = new ButtonGroup();
    private final List<JButton> viewButtons = new ArrayList<>();
    private final PowerPublisher powerPublisher;
    private final Record record;
    private final JCheckBox startupCheckbox = new JCheckBox("Run on Windows startup");

    public MainWindow(PowerPublisher powerPublisher, Record record)
    {
        this.powerPublisher = powerPublisher;
        this.record = record;

        logField.setEditable(false);
        logField.setFocusable(false);
        powerButton.addActionListener(e -> this.powerPublisher.toggle());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        List<Mode> modes = record.modes();
        for (int i = 0; i < modes.size(); i++)
        {
            JToggleButton toggleButton = new JToggleButton(modes.get(i).name());
            toggleButton.setEnabled(false);
            modeButtons.add(toggleButton);
            panel.add(toggleButton,
                      new Constraints(0, i).stretch().get());

            JButton viewButton = new JButton("View");
            final int modeIndex = i;
            viewButton.addActionListener(e -> openModeDialog(modeIndex));
            viewButtons.add(viewButton);
            panel.add(viewButton,
                      new Constraints(1, i).get());
        }

        // Icons
        List<Image> iconPack = List.of(
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon16.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon32.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon48.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon64.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon96.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon128.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon192.png")).getImage(),
                new ImageIcon(getClass().getClassLoader().getResource("resources/icon256.png")).getImage()
        );
        frame.setIconImages(iconPack);

        // Startup checkbox
        startupCheckbox.setSelected(record.isLaunchedOnStartup());
        startupCheckbox.addActionListener(e -> record.toggleLaunchedOnStartup());

        // Add all
        panel.add(logField,
                  new Constraints(0, modes.size()).width(2).stretch().grow().get());
        panel.add(startupCheckbox,
                  new Constraints(0, modes.size()+1).width(2).anchor(4).get());
        panel.add(powerButton,
                  new Constraints(0, modes.size()+2).width(2).insets(10).get());

        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void openModeDialog(int modeIndex)
    {
        powerPublisher.block();
        new ModeWindow(frame, record, modeIndex);
        powerPublisher.unblock();
    }

    public static void addToLog(String text) {
        logNumber++;
        Document doc = logField.getDocument();
        try {
            doc.insertString(0, " " + logNumber + ": " + text.trim() + "\n", null);

            // Erase log to avoid memory hog
            if (doc.getLength() > App.MAX_LOG_CHAR_LENGTH)
                doc.remove(App.MAX_LOG_CHAR_LENGTH, doc.getLength() - App.MAX_LOG_CHAR_LENGTH);

        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }


    }

    public void setVisible(boolean flag) { frame.setVisible(flag); }


    /* ----------------------------------------------------- INTERFACES ----------------------------------------------------- */

    @Override
    public void setPowered(boolean flag)
    {
        powerButton.setText(flag ? "Disable" : "Enable");
        powerButton.setSelected(flag);
        for (JButton button : viewButtons)
            button.setEnabled(!flag);
    }

    @Override
    public void setPowerBlocked(boolean flag) { powerButton.setEnabled(!flag); }

    @Override
    public void setMode(Mode mode)
    {
        for(Enumeration<AbstractButton> enumeration = modeButtons.getElements(); enumeration.hasMoreElements();)
        {
            AbstractButton button = enumeration.nextElement();
            if (button.getText().equals(mode.name()))
                button.setSelected(true);
        }
    }

    @Override
    public void setModeless() { modeButtons.clearSelection(); }

    @Override
    public void dispose() { frame.dispose(); }
}
