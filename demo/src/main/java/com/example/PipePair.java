package com.example;
import javafx.scene.shape.Rectangle;
class PipePair
{
    Rectangle top;
    Rectangle bottom;

    double baseY;
    double offset = 0;
    boolean moving;

    PipePair(Rectangle top, Rectangle bottom, double baseY, boolean moving)
    {
        this.top = top;
        this.bottom = bottom;
        this.baseY = baseY;
        this.moving = moving;
    }
}