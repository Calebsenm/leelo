package com.leelo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SideMenuControllerTest {

    private SideMenuController controller;

    @BeforeEach
    void setUp() {
        controller = new SideMenuController();
    }

    @Test
    void testControllerInstantiation() {
        assertNotNull(controller, "SideMenuController should be instantiated");
    }

    @Test
    void testControllerHasBasicFunctionality() {
        // Test that the controller can be created without errors
        assertDoesNotThrow(() -> {
            new SideMenuController();
        }, "SideMenuController should be creatable without exceptions");
    }
}