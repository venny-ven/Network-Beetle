package com.github.kleonaut.network_beetle;

import javax.swing.*;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ModeWindow
{
    private final JDialog dialog;
    private final JTextArea networkField;
    private final JTextArea conditionField;
    private final Record record;
    private final int index;
    private final JButton saveButton;

    ModeWindow(Frame owner, Record record, int modeIndex)
    {
        this.record = record;
        this.index = modeIndex;
        Mode mode = record.mode(index);

        dialog = new JDialog(owner, mode.name()+" Details", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        networkField = new JTextArea(" "+ mode.netProfile().name());
        networkField.setEditable(false);
        networkField.setFocusable(false);

        JButton networkEditButton = new JButton("Edit");
        networkEditButton.addActionListener(e -> openNetworkWindow());

        conditionField = new JTextArea();
        conditionField.setText(stringFromList(mode.conditions()));
        conditionField.setEditable(false);
        conditionField.setFocusable(false);
        JScrollPane conditionPane = new JScrollPane(conditionField);
        conditionPane.getVerticalScrollBar().setUnitIncrement(5);
        conditionPane.getHorizontalScrollBar().setUnitIncrement(5);

        JButton conditionEditButton = new JButton("Edit");
        if (mode == record.modes().getLast())
        {
            conditionEditButton.setEnabled(false);
            conditionField.setText(" Conditions can not be added\n to the default mode");
        }
        else
            conditionEditButton.addActionListener(e -> openConditionWindow());

        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveChanges());
        saveButton.setEnabled(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        panel.add(networkField,
                  new Constraints(0, 0).stretch().get());
        panel.add(networkEditButton,
                  new Constraints(1, 0).get());
        panel.add(conditionPane,
                  new Constraints(0, 1).stretch().grow().get());
        panel.add(conditionEditButton,
                  new Constraints(1, 1).anchor(8).get());
        panel.add(saveButton,
                  new Constraints(0, 2).width(2).anchor(6).get());

        dialog.setContentPane(panel);
        dialog.setSize(270, 280);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public void openNetworkWindow() { new NetworkWindow(this, record.mode(index).netProfile()); }
    public void setNetwork(NetProfile netProfile)
    {
        networkField.setText(" "+netProfile.name());
        saveButton.setEnabled(true);
    }

    public void openConditionWindow() { new ConditionWindow(this, record.mode(index).conditions()); }
    public void setConditions(List<String> conditions)
    {
        conditionField.setText(stringFromList(conditions));
        saveButton.setEnabled(true);
    }

    public void saveChanges()
    {
        Mode mode = new Mode(
                record.mode(index).name(),
                NetProfile.get(networkField.getText().trim()),
                listFromString(conditionField.getText()));
        record.overwriteModeAt(index, mode);
        saveButton.setEnabled(false);
    }

    private String stringFromList(List<String> list)
    {
        StringBuilder builder = new StringBuilder();
        for (String item : list)
            builder.append(" ").append(item).append("\n");
        return builder.toString();
    }

    private List<String> listFromString(String string)
    {
        List<String> list = new ArrayList<>();
        Scanner s = new Scanner(string).useDelimiter("\\n");
        while(s.hasNext())
            list.add(s.next().trim());
        return list;
    }

    public JDialog dialog() { return dialog; }
}
