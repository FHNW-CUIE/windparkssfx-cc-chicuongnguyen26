package cuie.project.template_simplecontrol.demo;

import javafx.beans.property.*;
import javafx.scene.paint.Color;

public class PresentationModel {
    private final DoubleProperty        pmValue         = new SimpleDoubleProperty();
    private final ObjectProperty<Color> baseColor       = new SimpleObjectProperty<>();
    private final BooleanProperty       running         = new SimpleBooleanProperty(true);
    private final DoubleProperty        min             = new SimpleDoubleProperty(200.0);
    private final DoubleProperty        max             = new SimpleDoubleProperty(5000.0);

    public double getPmValue() {
        return pmValue.get();
    }

    public DoubleProperty pmValueProperty() {
        return pmValue;
    }

    public void setPmValue(double pmValue) {
        this.pmValue.set(pmValue);
    }

    public Color getBaseColor() {
        return baseColor.get();
    }

    public ObjectProperty<Color> baseColorProperty() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor.set(baseColor);
    }

    public boolean getRunning() {
        return running.get();
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public double getMax() {
        return max.get();
    }

    public DoubleProperty maxProperty() {
        return max;
    }

    public void setMax(double max) {
        this.max.set(max);
    }

    public double getMin() {
        return min.get();
    }

    public DoubleProperty minProperty() {
        return min;
    }

    public void setMin(double min) {
        this.min.set(min);
    }
}
