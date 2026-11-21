package org.campusfix.desktop.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class CampusFixView {
    private final JFrame frame = new JFrame("CampusFix");
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton signIn = new JButton("Sign in");
    private final JButton check = new JButton("Check database connection");
    private final JLabel status = new JLabel();
    private final JLabel accountTitle = new JLabel();
    private final JLabel accountName = new JLabel();
    private final JButton signOut = new JButton("Sign out");

    public CampusFixView() {
        cards.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        cards.add(createSignInPanel(), "sign-in");
        cards.add(createAccountPanel(), "account");
        frame.setContentPane(cards);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(570, 215);
        frame.setLocationRelativeTo(null);
    }

    private JPanel createSignInPanel() {
        var fields = new JPanel(new GridLayout(0, 2, 8, 8));
        var usernameLabel = new JLabel("Username");
        usernameLabel.setLabelFor(username);
        fields.add(usernameLabel);
        fields.add(username);
        var passwordLabel = new JLabel("Password");
        passwordLabel.setLabelFor(password);
        fields.add(passwordLabel);
        fields.add(password);

        var actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        actions.add(signIn);
        actions.add(check);

        var panel = new JPanel(new BorderLayout(0, 12));
        panel.add(fields, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAccountPanel() {
        var panel = new JPanel(new BorderLayout(0, 12));
        panel.add(accountTitle, BorderLayout.NORTH);
        panel.add(accountName, BorderLayout.CENTER);
        panel.add(signOut, BorderLayout.SOUTH);
        return panel;
    }

    public void onSignIn(Runnable action) {
        signIn.addActionListener(event -> action.run());
    }

    public void onCheck(Runnable action) {
        check.addActionListener(event -> action.run());
    }

    public void onSignOut(Runnable action) {
        signOut.addActionListener(event -> action.run());
    }

    public void onClose(Runnable action) {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                action.run();
            }
        });
    }

    public String username() {
        return username.getText();
    }

    public char[] takePassword() {
        char[] value = password.getPassword();
        password.setText("");
        return value;
    }

    public void showStatus(String message, boolean busy) {
        status.setText(message);
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        signIn.setEnabled(!busy);
        check.setEnabled(!busy);
    }

    public void showSignIn() {
        username.setText("");
        password.setText("");
        showStatus("Sign in to CampusFix.", false);
        layout.show(cards, "sign-in");
        frame.getRootPane().setDefaultButton(signIn);
    }

    public void showAccount(String title, String name) {
        accountTitle.setText(title);
        accountName.setText(String.format("Signed in as %s", name));
        layout.show(cards, "account");
        frame.getRootPane().setDefaultButton(signOut);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
    }
}
