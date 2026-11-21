package org.campusfix.desktop.controller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.campusfix.desktop.view.CampusFixView;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.DatabaseConnection;

public final class CampusFixController {
    private static final Logger LOGGER = Logger.getLogger(CampusFixController.class.getName());

    private final DatabaseConnection database;
    private final AuthenticationService authentication;
    private final CampusFixView view;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private User signedInUser;
    private boolean closing;

    public CampusFixController(DatabaseConnection database, AuthenticationService authentication) {
        this.database = database;
        this.authentication = authentication;
        view = new CampusFixView();
        view.onSignIn(this::signIn);
        view.onCheck(this::checkConnection);
        view.onSignOut(this::signOut);
        view.onClose(this::close);
    }

    public void start() {
        view.showSignIn();
        view.show();
    }

    private void signIn() {
        String username = view.username();
        char[] password = view.takePassword();
        view.showStatus("Signing in...", true);

        worker.execute(() -> {
            try {
                User user = authentication.signIn(username, password).orElse(null);
                updateWindow(() -> completeSignIn(user));
            } catch (SQLException | IllegalStateException exception) {
                LOGGER.log(Level.WARNING, "Sign-in failed.", exception);
                updateWindow(() -> view.showStatus("Could not sign in. Check the database setup.", false));
            } finally {
                Arrays.fill(password, '\0');
            }
        });
    }

    private void completeSignIn(User user) {
        if (user == null) {
            view.showStatus("Username or password is incorrect.", false);
            return;
        }
        signedInUser = user;
        showAccount();
    }

    private void showAccount() {
        String title = signedInUser.role() == User.Role.ADMIN ? "Administrator account" : "Regular account";
        view.showAccount(title, signedInUser.username());
    }

    private void signOut() {
        signedInUser = null;
        view.showSignIn();
    }

    private void checkConnection() {
        view.showStatus("Checking PostgreSQL...", true);
        worker.execute(() -> {
            String message;
            try {
                message = database.checkConnection()
                        ? "Connected to PostgreSQL."
                        : "PostgreSQL returned an unexpected result.";
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Database connection check failed.", exception);
                message = "Connection failed. Check your settings and PostgreSQL.";
            }
            String result = message;
            updateWindow(() -> view.showStatus(result, false));
        });
    }

    private void updateWindow(Runnable update) {
        SwingUtilities.invokeLater(() -> {
            if (!closing) {
                update.run();
            }
        });
    }

    private void close() {
        if (closing) {
            return;
        }
        closing = true;
        view.close();
        worker.execute(() -> {
            try {
                database.close();
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Database connection could not be closed.", exception);
            }
        });
        worker.shutdown();
    }
}
