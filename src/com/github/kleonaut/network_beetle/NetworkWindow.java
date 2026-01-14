package com.github.kleonaut.network_beetle;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class NetworkWindow
{
    private final JDialog dialog;
    private final JTextArea nominatedNetwork;
    private final JList<String> networkList;
    private final ModeWindow parent;
    private static final Timer timer = new Timer(2000, null);

    NetworkWindow(ModeWindow parent, NetProfile assignedProfile)
    {
        dialog = new JDialog(parent.dialog(), "Assign Network Profile", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.parent = parent;

        nominatedNetwork = new JTextArea(" "+assignedProfile.name());
        nominatedNetwork.setEditable(false);
        nominatedNetwork.setFocusable(false);

        networkList = new JList<>(new DefaultListModel<>());
        networkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        networkList.addListSelectionListener(e -> nominateSelection());
        networkList.setEnabled(false);
        ((DefaultListModel<String>)networkList.getModel()).addElement(" Fetching...");
        JScrollPane networkListPane = new JScrollPane(networkList);
        networkListPane.getVerticalScrollBar().setUnitIncrement(5);
        networkListPane.getHorizontalScrollBar().setUnitIncrement(5);

        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> confirmSelection());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        panel.add(nominatedNetwork,
                  new Constraints(0, 0).stretch().insets(5, 10, 5, 5).get());
        panel.add(networkListPane,
                  new Constraints(0, 1).stretch().grow().get());
        panel.add(confirmButton,
                  new Constraints(0, 2).anchor(6).get());

        dialog.setContentPane(panel);
        dialog.setSize(300, 250);
        dialog.setLocationRelativeTo(parent.dialog());

        timer.addActionListener(e -> updateNetworkList());
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
        timer.setInitialDelay(400);
        timer.start();

        dialog.setVisible(true);
    }

    public void updateNetworkList()
    {
        networkList.setEnabled(true);

        String selectedNetwork = networkList.getSelectedValue();
        int selectedIndex = -1;

        DefaultListModel<String>listModel = (DefaultListModel<String>)networkList.getModel();
        listModel.clear();
        listModel.addElement(" " + NetProfile.STAY.name());
        listModel.addElement(" " + NetProfile.DISCONNECT.name());

        for (NetProfile profile : OSInteractions.fetchAllNetworkProfiles())
            listModel.addElement(" " + profile.name());

        for (int i = 0; i < listModel.size(); i++)
            if (listModel.get(i).equals(selectedNetwork))
                selectedIndex = i;

        networkList.setSelectedIndex(selectedIndex);
    }

    public void nominateSelection() { nominatedNetwork.setText(networkList.getSelectedValue()); }
    public void confirmSelection()
    {
        parent.setNetwork(NetProfile.get(nominatedNetwork.getText().trim()));
        dialog.dispose();
    }
}
