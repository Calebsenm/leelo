package com.leelo;

import com.leelo.controller.SideMenuController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that every screen can be loaded.
 *
 * <p>A broken screen is only noticed when the user opens it, so the screens
 * are loaded here to make sure that the files and the controllers agree.
 */
class FxmlSmokeTest {

    private static final List<String> SCREENS = List.of(
            "side_menu",
            "texts",
            "add_text",
            "add_text_url",
            "words",
            "add_word",
            "languages",
            "language_form",
            "term_tags",
            "statistics",
            "settings",
            "reading");

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void everyScreenLoads() throws Exception {
        for (String screen : SCREENS) {
            FXMLLoader loader = new FXMLLoader(
                    FxmlSmokeTest.class.getResource("/com/leelo/" + screen + ".fxml"));
            Parent root = loader.load();
            assertNotNull(root, "The screen " + screen + " is empty");
            assertNotNull(loader.getController(), "The screen " + screen + " has no controller");
        }
    }

    /** The menu must never point at a screen that does not exist. */
    @Test
    void everyMenuDestinationHasAScreen() {
        for (SideMenuController.Section section : SideMenuController.Section.values()) {
            if (section == SideMenuController.Section.HOME) {
                continue;
            }
            String screen = "/com/leelo/" + section.getDestination() + ".fxml";
            assertNotNull(FxmlSmokeTest.class.getResource(screen),
                    "Missing screen for menu section " + section + ": " + screen);
        }
    }
}
