package org.campusfix.desktop.view;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class ConnectionView {
    private final JFrame frame = new JFrame("CampusFix");
    private final JLabel status = new JLabel("Ready to check the database.");
    private final JButton checkButton = new JButton("Check database connection");

    public ConnectionView() {
        var content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(status, BorderLayout.CENTER);
        content.add(checkButton, BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(600, 180);
        frame.setLocationRelativeTo(null);
    }

    public void onCheck(Runnable action) {
        checkButton.addActionListener(event -> action.run());
    }

    public void onClose(Runnable action) {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                action.run();
            }
        });
    }

    public void showStatus(String message, boolean busy) {
        status.setText(message);
        checkButton.setEnabled(!busy);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
    }
}

