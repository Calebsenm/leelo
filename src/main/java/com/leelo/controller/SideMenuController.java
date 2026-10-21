package com.leelo.controller;

import com.leelo.App;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.shape.SVGPath;

/**
 * The navigation bar shown on the left of every screen.
 */
public class SideMenuController {

    /** The screens the menu can navigate to. */
    public enum Section {
        HOME("home", "/com/leelo/icons/home.png",
                "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
        TEXTS("texts", "/com/leelo/icons/notes.png",
                "M21 5c-1.11-.35-2.33-.5-3.5-.5-1.95 0-4.05.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 .25.25.5.5.5.1 0 .15-.05.25-.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05.4 5.5 1.5 1.35-.85 3.8-1.5 5.5-1.5 1.65 0 3.35.3 4.75 1.05.1.05.15.05.25.05.25 0 .5-.25.5-.5V6c-.6-.45-1.25-.75-2-1zm0 13.5c-1.1-.35-2.3-.5-3.5-.5-1.7 0-4.15.65-5.5 1.5V8c1.35-.85 3.8-1.5 5.5-1.5 1.2 0 2.4.15 3.5.5v11.5z"),
        WORDS("words", "/com/leelo/icons/word.png",
                "M12.87 15.07l-2.54-2.51.03-.03A17.52 17.52 0 0014.07 6H17V4h-7V2H8v2H1v2h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 9h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-13zm-2.62 7l1.62-4.33L19.12 16h-3.24z"),
        LANGUAGES("languages", null,
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"),
        TAGS("term_tags", null,
                "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"),
        STATS("statistics", null,
                "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z"),
        SETTINGS("settings", null,
                "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z");

        private final String destination;
        private final String iconPath;
        private final String svgPath;

        Section(String destination, String iconPath, String svgPath) {
            this.destination = destination;
            this.iconPath = iconPath;
            this.svgPath = svgPath;
        }

        public String getDestination() {
            return destination;
        }
    }

    @FXML
    private Button homeButton;
    @FXML
    private Button textButton;
    @FXML
    private Button wordsButton;
    @FXML
    private Button languagesButton;
    @FXML
    private Button tagsButton;
    @FXML
    private Button statsButton;
    @FXML
    private Button settingsButton;

    private Button activeButton;
    private static final double ICON_SIZE = 18.0;
    private static String currentSection = "home";

    /** Remembers the section that is about to be shown. */
    public static void setCurrentSection(String section) {
        currentSection = normalizeSection(section);
    }

    /**
     * Maps the FXML files that belong to a section to the destination of that
     * section, so the menu keeps the right entry highlighted on sub-screens.
     */
    private static String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return "home";
        }
        return switch (section) {
            case "add_text", "add_text_url" -> "texts";
            case "language_form" -> "languages";
            case "tags" -> "term_tags";
            case "stats" -> "statistics";
            default -> section;
        };
    }

    @FXML
    public void initialize() {
        setupButton(homeButton, Section.HOME);
        setupButton(textButton, Section.TEXTS);
        setupButton(wordsButton, Section.WORDS);
        setupButton(languagesButton, Section.LANGUAGES);
        setupButton(tagsButton, Section.TAGS);
        setupButton(statsButton, Section.STATS);
        setupButton(settingsButton, Section.SETTINGS);
        setActiveSection(currentSection);
    }

    /** Attaches the icon and the navigation action to a button. */
    private void setupButton(Button button, Section section) {
        button.setGraphic(createIcon(section));
        button.setGraphicTextGap(10);
        button.setOnAction(event -> {
            setActiveSection(section.getDestination());
            navigate(section);
        });
    }

    /** Builds the icon of a menu entry. */
    private SVGPath createIcon(Section section) {
        SVGPath icon = new SVGPath();
        icon.setContent(section.svgPath);
        icon.setScaleX(ICON_SIZE / 24.0);
        icon.setScaleY(ICON_SIZE / 24.0);
        icon.getStyleClass().add("nav-icon");
        return icon;
    }

    /** Marks a button as the one of the current screen. */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-btn-active");
        }
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("nav-btn-active");
        }
    }

    /**
     * Marks the entry of the given screen as the current one.
     */
    public void setActiveSection(String section) {
        currentSection = normalizeSection(section);
        Button button = null;
        for (Section candidate : Section.values()) {
            if (candidate.getDestination().equalsIgnoreCase(currentSection)) {
                button = buttonFor(candidate);
                break;
            }
        }
        if (button != null) {
            setActiveButton(button);
        }
    }

    /** Shows or hides the menu. */
    public void setVisible(boolean visible) {
        if (homeButton.getParent() != null) {
            homeButton.getParent().setVisible(visible);
            homeButton.getParent().setManaged(visible);
        }
    }

    /** Returns the button of a menu entry. */
    private Button buttonFor(Section section) {
        return switch (section) {
            case HOME -> homeButton;
            case TEXTS -> textButton;
            case WORDS -> wordsButton;
            case LANGUAGES -> languagesButton;
            case TAGS -> tagsButton;
            case STATS -> statsButton;
            case SETTINGS -> settingsButton;
        };
    }

    /** Opens a screen of the application. */
    private void navigate(Section section) {
        try {
            if (section == Section.HOME) {
                App.showHome();
            } else {
                App.setRoot(section.getDestination());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
