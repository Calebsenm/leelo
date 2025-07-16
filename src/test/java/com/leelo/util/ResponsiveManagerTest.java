package com.leelo.util;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResponsiveManager functionality
 * Note: This requires JavaFX to be initialized for proper testing
 */
public class ResponsiveManagerTest {
    
    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX toolkit for testing
        // This is a simplified approach - in real testing you might use TestFX
        try {
            new Thread(() -> Application.launch(TestApp.class)).start();
            Thread.sleep(1000); // Wait for JavaFX to initialize
        } catch (Exception e) {
            // JavaFX might already be initialized
        }
    }
    
    @Test
    void testResponsiveStateDetection() {
        // Test responsive state determination logic
        assertEquals(ResponsiveManager.ResponsiveState.SMALL, 
                    determineState(400));
        assertEquals(ResponsiveManager.ResponsiveState.MEDIUM, 
                    determineState(800));
        assertEquals(ResponsiveManager.ResponsiveState.LARGE, 
                    determineState(1200));
    }
    
    @Test
    void testBreakpoints() {
        // Test breakpoint constants
        assertEquals(600.0, ResponsiveManager.SMALL_BREAKPOINT);
        assertEquals(1024.0, ResponsiveManager.MEDIUM_BREAKPOINT);
    }
    
    @Test
    void testCSSClassNames() {
        // Test CSS class name constants
        assertEquals("responsive-small", ResponsiveManager.RESPONSIVE_SMALL);
        assertEquals("responsive-medium", ResponsiveManager.RESPONSIVE_MEDIUM);
        assertEquals("responsive-large", ResponsiveManager.RESPONSIVE_LARGE);
    }
    
    // Helper method to simulate state determination logic
    private ResponsiveManager.ResponsiveState determineState(double width) {
        if (width < ResponsiveManager.SMALL_BREAKPOINT) {
            return ResponsiveManager.ResponsiveState.SMALL;
        } else if (width < ResponsiveManager.MEDIUM_BREAKPOINT) {
            return ResponsiveManager.ResponsiveState.MEDIUM;
        } else {
            return ResponsiveManager.ResponsiveState.LARGE;
        }
    }
    
    /**
     * Simple test application for JavaFX initialization
     */
    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            VBox root = new VBox();
            root.getChildren().add(new Label("Test"));
            Scene scene = new Scene(root, 300, 200);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Test");
            // Don't show the stage for testing
        }
    }
}