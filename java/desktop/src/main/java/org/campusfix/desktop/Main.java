package org.campusfix.desktop;

import javax.swing.SwingUtilities;
import org.campusfix.desktop.controller.CampusFixController;
import org.campusfix.model.CampusFix;

public final class Main {
    public static void main(String[] args) {
        var database = DesktopDatabase.getInstance().connection();
        var model = new CampusFix(database);
        SwingUtilities.invokeLater(() -> new CampusFixController(model).start());
    }
}
