package com.leelo.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating simple PNG icons using JavaFX shapes
 */
public class IconFactory {
    
    private static final Map<IconType, Image> iconCache = new HashMap<>();
    private static final double ICON_SIZE = 32.0;
    
    public enum IconType {
        HOME,
        DOCUMENT,
        BOOK,
        BRAIN,
        EXIT
    }
    
    /**
     * Gets an icon image for the specified type
     * @param type The icon type
     * @return Image object for the icon
     */
    public static Image getIcon(IconType type) {
        if (iconCache.containsKey(type)) {
            return iconCache.get(type);
        }
        
        Image icon = createIcon(type);
        iconCache.put(type, icon);
        return icon;
    }
    
    /**
     * Creates an icon using JavaFX shapes
     * @param type The icon type to create
     * @return Image object
     */
    private static Image createIcon(IconType type) {
        Group iconGroup = new Group();
        
        switch (type) {
            case HOME:
                iconGroup = createHomeIcon();
                break;
            case DOCUMENT:
                iconGroup = createDocumentIcon();
                break;
            case BOOK:
                iconGroup = createBookIcon();
                break;
            case BRAIN:
                iconGroup = createBrainIcon();
                break;
            case EXIT:
                iconGroup = createExitIcon();
                break;
        }
        
        // Create snapshot parameters
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        
        // Create writable image
        WritableImage image = new WritableImage((int)ICON_SIZE, (int)ICON_SIZE);
        iconGroup.snapshot(params, image);
        
        return image;
    }
    
    private static Group createHomeIcon() {
        Group group = new Group();
        
        // House roof (triangle)
        Polygon roof = new Polygon();
        roof.getPoints().addAll(new Double[]{
            16.0, 6.0,   // top
            8.0, 14.0,   // left
            24.0, 14.0   // right
        });
        roof.setFill(Color.web("#4a5568"));
        
        // House body (rectangle)
        Rectangle body = new Rectangle(10, 14, 12, 12);
        body.setFill(Color.web("#718096"));
        
        // Door (rectangle)
        Rectangle door = new Rectangle(14, 20, 4, 6);
        door.setFill(Color.web("#4a5568"));
        
        group.getChildren().addAll(roof, body, door);
        return group;
    }
    
    private static Group createDocumentIcon() {
        Group group = new Group();
        
        // Document body
        Rectangle doc = new Rectangle(8, 4, 16, 24);
        doc.setFill(Color.web("#e2e8f0"));
        doc.setStroke(Color.web("#4a5568"));
        doc.setStrokeWidth(1);
        
        // Document corner fold
        Polygon fold = new Polygon();
        fold.getPoints().addAll(new Double[]{
            20.0, 4.0,   // top right
            20.0, 8.0,   // fold bottom
            24.0, 8.0    // fold right
        });
        fold.setFill(Color.web("#cbd5e1"));
        fold.setStroke(Color.web("#4a5568"));
        fold.setStrokeWidth(1);
        
        // Text lines
        Line line1 = new Line(10, 12, 18, 12);
        line1.setStroke(Color.web("#4a5568"));
        line1.setStrokeWidth(1);
        
        Line line2 = new Line(10, 16, 18, 16);
        line2.setStroke(Color.web("#4a5568"));
        line2.setStrokeWidth(1);
        
        Line line3 = new Line(10, 20, 15, 20);
        line3.setStroke(Color.web("#4a5568"));
        line3.setStrokeWidth(1);
        
        group.getChildren().addAll(doc, fold, line1, line2, line3);
        return group;
    }
    
    private static Group createBookIcon() {
        Group group = new Group();
        
        // Book spine
        Rectangle spine = new Rectangle(6, 6, 4, 20);
        spine.setFill(Color.web("#4a5568"));
        
        // Book pages
        Rectangle pages = new Rectangle(10, 8, 16, 18);
        pages.setFill(Color.web("#f7fafc"));
        pages.setStroke(Color.web("#4a5568"));
        pages.setStrokeWidth(1);
        
        // Book cover
        Rectangle cover = new Rectangle(8, 6, 16, 20);
        cover.setFill(Color.TRANSPARENT);
        cover.setStroke(Color.web("#4a5568"));
        cover.setStrokeWidth(2);
        
        group.getChildren().addAll(spine, pages, cover);
        return group;
    }
    
    private static Group createBrainIcon() {
        Group group = new Group();
        
        // Brain outline (simplified)
        Circle brain = new Circle(16, 16, 12);
        brain.setFill(Color.web("#e2e8f0"));
        brain.setStroke(Color.web("#4a5568"));
        brain.setStrokeWidth(2);
        
        // Brain details (curves)
        Arc curve1 = new Arc(12, 12, 6, 6, 0, 180);
        curve1.setFill(Color.TRANSPARENT);
        curve1.setStroke(Color.web("#4a5568"));
        curve1.setStrokeWidth(1);
        
        Arc curve2 = new Arc(20, 12, 6, 6, 0, 180);
        curve2.setFill(Color.TRANSPARENT);
        curve2.setStroke(Color.web("#4a5568"));
        curve2.setStrokeWidth(1);
        
        Line divider = new Line(16, 8, 16, 24);
        divider.setStroke(Color.web("#4a5568"));
        divider.setStrokeWidth(1);
        
        group.getChildren().addAll(brain, curve1, curve2, divider);
        return group;
    }
    
    private static Group createExitIcon() {
        Group group = new Group();
        
        // Door frame
        Rectangle frame = new Rectangle(6, 4, 20, 24);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Color.web("#4a5568"));
        frame.setStrokeWidth(2);
        
        // Door
        Rectangle door = new Rectangle(8, 6, 16, 20);
        door.setFill(Color.web("#e2e8f0"));
        door.setStroke(Color.web("#4a5568"));
        door.setStrokeWidth(1);
        
        // Door handle
        Circle handle = new Circle(20, 16, 2);
        handle.setFill(Color.web("#4a5568"));
        
        // Exit arrow
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[]{
            28.0, 16.0,  // tip
            24.0, 12.0,  // top
            24.0, 14.0,  // top inner
            20.0, 14.0,  // left
            20.0, 18.0,  // left bottom
            24.0, 18.0,  // bottom inner
            24.0, 20.0   // bottom
        });
        arrow.setFill(Color.web("#dc2626"));
        
        group.getChildren().addAll(frame, door, handle, arrow);
        return group;
    }
    
    /**
     * Clears the icon cache
     */
    public static void clearCache() {
        iconCache.clear();
    }
}