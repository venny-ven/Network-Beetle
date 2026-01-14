package com.github.kleonaut.network_beetle;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class ConditionWindow
{
    // TODO: list width changes when contents change because of button text update, list width should be fixed

    private final JDialog dialog;
    private final JList<String> nominatedCondtions;
    private final JList<String> possibleConditions;
    private final ModeWindow parent;

    private final JButton transferButton;
    private static final Timer timer = new Timer(2000, null);

    private boolean isLocked;
    private boolean isAdding;

    ConditionWindow(ModeWindow parent, List<String> assignedConditions)
    {
        dialog = new JDialog(parent.dialog(), "Assign Conditions", true);
        this.parent = parent;

        nominatedCondtions = new JList<>(new DefaultListModel<>());
        nominatedCondtions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nominatedCondtions.setPrototypeCellValue("a");
        nominatedCondtions.addListSelectionListener(selectAction);
        DefaultListModel<String>listModel = (DefaultListModel<String>)nominatedCondtions.getModel();
        for (String condition : assignedConditions)
            listModel.addElement(" " + condition);
        JScrollPane nominatedPane = new JScrollPane(nominatedCondtions);
        nominatedPane.getVerticalScrollBar().setUnitIncrement(5);
        nominatedPane.getHorizontalScrollBar().setUnitIncrement(5);

        transferButton = new JButton("=");
        transferButton.setEnabled(false);
        transferButton.addActionListener(e -> performTransfer());

        possibleConditions = new JList<>(new DefaultListModel<>());
        possibleConditions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        possibleConditions.setPrototypeCellValue("a");
        possibleConditions.addListSelectionListener(selectAction);
        possibleConditions.setEnabled(false);
        ((DefaultListModel<String>)possibleConditions.getModel()).addElement(" Fetching...");
        JScrollPane possiblePane = new JScrollPane(possibleConditions);
        possiblePane.getVerticalScrollBar().setUnitIncrement(5);
        possiblePane.getHorizontalScrollBar().setUnitIncrement(5);

        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> confirmChanges());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        panel.add(nominatedPane,
                  new Constraints(0, 0).stretch().grow().get());
        panel.add(transferButton,
                  new Constraints(1, 0).get());
        panel.add(possiblePane,
                  new Constraints(2, 0).stretch().grow().get());
        panel.add(confirmButton,
                  new Constraints(0, 1).width(3).anchor(6).get());

        dialog.setContentPane(panel);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(parent.dialog());

        timer.addActionListener(e -> updateConditions());
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                timer.stop();
                for (ActionListener listener : timer.getActionListeners())
                    timer.removeActionListener(listener);
            }
        });
        timer.setInitialDelay(700);
        timer.start();

        dialog.setVisible(true);
    }

    public void updateConditions()
    {
        isLocked = true;
        possibleConditions.setEnabled(true);

        String selectedCondition = possibleConditions.getSelectedValue();
        int selectedIndex = -1;

        DefaultListModel<String>listModel = (DefaultListModel<String>)possibleConditions.getModel();
        listModel.clear();

        for (String task : OSInteractions.fetchTasksNoRepeats())
            listModel.addElement(" " + task);

        for (int i = 0; i < listModel.size(); i++)
            if (listModel.get(i).equals(selectedCondition))
                selectedIndex = i;

        possibleConditions.setSelectedIndex(selectedIndex);
        isLocked = false;
    }

    public void performTransfer()
    {
        isLocked = true;
        if (isAdding)
            // NOTE: when nominatedCondtions has no items, this line invokes its ListSelectionListener
            ((DefaultListModel<String>)nominatedCondtions.getModel()).addElement(possibleConditions.getSelectedValue());
        else
            ((DefaultListModel<String>)nominatedCondtions.getModel()).removeElementAt(nominatedCondtions.getSelectedIndex());
        transferButton.setText("=");
        transferButton.setEnabled(false);
        isLocked = false;
    }

    ListSelectionListener selectAction = new ListSelectionListener()
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting()) return;
            if (isLocked) return;
            isLocked = true;
            if (e.getSource() == nominatedCondtions)
            {
                possibleConditions.clearSelection();
                isAdding = false;
                transferButton.setText(">");
            }
            else
            {
                nominatedCondtions.clearSelection();

                // disable transfer button if item is already in nominated conditions
                DefaultListModel<String> list = (DefaultListModel<String>)nominatedCondtions.getModel();
                for (int i = 0; i < list.size(); i++)
                    if(list.get(i).equals(possibleConditions.getSelectedValue()))
                    {
                        transferButton.setEnabled(false);
                        transferButton.setText("=");
                        isLocked = false;
                        return;
                    }

                isAdding = true;
                transferButton.setText("<");
            }
            transferButton.setEnabled(true);
            isLocked = false;
        }
    };

    public void confirmChanges()
    {
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < nominatedCondtions.getModel().getSize(); i++)
            conditions.add(nominatedCondtions.getModel().getElementAt(i).trim());
        parent.setConditions(conditions);
        dialog.dispose();
    }
}
