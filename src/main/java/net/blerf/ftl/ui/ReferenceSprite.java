package net.blerf.ftl.ui;

/**
 * A sprite which represents a SpriteReference's nested object.
 */
public interface ReferenceSprite<T> {

    SpriteReference<T> getReference();

    void referenceChanged();
}
