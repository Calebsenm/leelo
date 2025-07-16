package com.leelo.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * ResponsiveManager handles responsive design by applying CSS classes based on window size.
 * Since JavaFX doesn't support CSS media queries natively, this class provides
 * a programmatic approach to responsive design.
 */
public class ResponsiveManager {
    
    // Breakpoint constants
    public static final double SMALL_BREAKPOINT = 600.0;
    public static final double MEDIUM_BREAKPOINT = 1024.0;
    
    // CSS class names for responsive states
    public static final String RESPONSIVE_SMALL = "responsive-small";
    public static final String RESPONSIVE_MEDIUM = "responsive-medium";
    public static final String RESPONSIVE_LARGE = "responsive-large";
    
    private Stage stage;
    private Scene scene;
    private List<Node> managedNodes;
    private ResponsiveState currentState;
    
    // Listeners for width and height changes
    private ChangeListener<Number> widthListener;
    private ChangeListener<Number> heightListener;
    
    public enum ResponsiveState {
        SMALL, MEDIUM, LARGE
    }
    
    /**
     * Constructor for ResponsiveManager
     * @param stage The primary stage of the application
     */
    public ResponsiveManager(Stage stage) {
        this.stage = stage;
        this.scene = stage.getScene();
        this.managedNodes = new ArrayList<>();
        this.currentState = ResponsiveState.LARGE; // Default state
        
        initializeListeners();
        attachListeners();
        updateResponsiveState(); // Initial state calculation
    }
    
    /**
     * Initialize the change listeners for width and height
     */
    private void initializeListeners() {
        widthListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateResponsiveState();
            }
        };
        
        heightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateResponsiveState();
            }
        };
    }
    
    /**
     * Attach listeners to stage width and height properties
     */
    private void attachListeners() {
        stage.widthProperty().addListener(widthListener);
        stage.heightProperty().addListener(heightListener);
    }
    
    /**
     * Remove listeners from stage properties
     */
    public void detachListeners() {
        stage.widthProperty().removeListener(widthListener);
        stage.heightProperty().removeListener(heightListener);
    }
    
    /**
     * Add a node to be managed by the responsive system
     * @param node The node to manage
     */
    public void addManagedNode(Node node) {
        if (!managedNodes.contains(node)) {
            managedNodes.add(node);
            applyResponsiveClass(node, currentState);
        }
    }
    
    /**
     * Remove a node from responsive management
     * @param node The node to remove
     */
    public void removeManagedNode(Node node) {
        managedNodes.remove(node);
        removeAllResponsiveClasses(node);
    }
    
    /**
     * Update the responsive state based on current window dimensions
     */
    private void updateResponsiveState() {
        double width = stage.getWidth();
        ResponsiveState newState = determineResponsiveState(width);
        
        if (newState != currentState) {
            ResponsiveState oldState = currentState;
            currentState = newState;
            onResponsiveStateChanged(oldState, newState);
        }
    }
    
    /**
     * Determine the responsive state based on width
     * @param width Current window width
     * @return The appropriate ResponsiveState
     */
    private ResponsiveState determineResponsiveState(double width) {
        if (width < SMALL_BREAKPOINT) {
            return ResponsiveState.SMALL;
        } else if (width < MEDIUM_BREAKPOINT) {
            return ResponsiveState.MEDIUM;
        } else {
            return ResponsiveState.LARGE;
        }
    }
    
    /**
     * Handle responsive state changes
     * @param oldState Previous responsive state
     * @param newState New responsive state
     */
    private void onResponsiveStateChanged(ResponsiveState oldState, ResponsiveState newState) {
        // Apply new responsive classes to all managed nodes
        for (Node node : managedNodes) {
            removeAllResponsiveClasses(node);
            applyResponsiveClass(node, newState);
        }
    }
    
    /**
     * Apply the appropriate responsive CSS class to a node
     * @param node The node to apply the class to
     * @param state The responsive state
     */
    private void applyResponsiveClass(Node node, ResponsiveState state) {
        String cssClass = getResponsiveClassName(state);
        if (!node.getStyleClass().contains(cssClass)) {
            node.getStyleClass().add(cssClass);
        }
    }
    
    /**
     * Remove all responsive CSS classes from a node
     * @param node The node to clean up
     */
    private void removeAllResponsiveClasses(Node node) {
        node.getStyleClass().removeAll(RESPONSIVE_SMALL, RESPONSIVE_MEDIUM, RESPONSIVE_LARGE);
    }
    
    /**
     * Get the CSS class name for a responsive state
     * @param state The responsive state
     * @return The corresponding CSS class name
     */
    private String getResponsiveClassName(ResponsiveState state) {
        switch (state) {
            case SMALL:
                return RESPONSIVE_SMALL;
            case MEDIUM:
                return RESPONSIVE_MEDIUM;
            case LARGE:
                return RESPONSIVE_LARGE;
            default:
                return RESPONSIVE_LARGE;
        }
    }
    
    /**
     * Get the current responsive state
     * @return Current ResponsiveState
     */
    public ResponsiveState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the current window width
     * @return Current window width in pixels
     */
    public double getCurrentWidth() {
        return stage.getWidth();
    }
    
    /**
     * Get the current window height
     * @return Current window height in pixels
     */
    public double getCurrentHeight() {
        return stage.getHeight();
    }
    
    /**
     * Check if the current state is small screen
     * @return true if current state is SMALL
     */
    public boolean isSmallScreen() {
        return currentState == ResponsiveState.SMALL;
    }
    
    /**
     * Check if the current state is medium screen
     * @return true if current state is MEDIUM
     */
    public boolean isMediumScreen() {
        return currentState == ResponsiveState.MEDIUM;
    }
    
    /**
     * Check if the current state is large screen
     * @return true if current state is LARGE
     */
    public boolean isLargeScreen() {
        return currentState == ResponsiveState.LARGE;
    }
    
    /**
     * Manually trigger a responsive state update
     * Useful when nodes are added dynamically
     */
    public void refresh() {
        updateResponsiveState();
    }
    
    /**
     * Set minimum window constraints based on responsive requirements
     */
    public void setMinimumConstraints() {
        stage.setMinWidth(400); // Minimum usable width
        stage.setMinHeight(300); // Minimum usable height
    }
    
    /**
     * Apply responsive classes to the scene root and common containers
     */
    public void initializeSceneResponsiveness() {
        if (scene != null && scene.getRoot() != null) {
            addManagedNode(scene.getRoot());
        }
        setMinimumConstraints();
    }
    
    /**
     * Utility method to create a responsive manager and initialize it
     * @param stage The primary stage
     * @return Configured ResponsiveManager instance
     */
    public static ResponsiveManager createAndInitialize(Stage stage) {
        ResponsiveManager manager = new ResponsiveManager(stage);
        manager.initializeSceneResponsiveness();
        return manager;
    }
}