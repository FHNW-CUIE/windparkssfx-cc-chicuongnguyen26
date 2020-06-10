package cuie.project.template_simplecontrol.demo;

import javafx.geometry.Insets;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import cuie.project.template_simplecontrol.Tachometer;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

public class DemoPane extends BorderPane {

    private final PresentationModel pm;

    // custom control
    private Tachometer tachometer;

    // all controls
    private Slider          slider;
    private ColorPicker     colorPicker;
    private RadioButton     stateSwitch;


    public DemoPane(PresentationModel pm) {
        this.pm = pm;
        initializeControls();
        layoutControls();
        setupEventhandlers();
        setupBindings();
    }

    private void initializeControls() {
        setPadding(new Insets(10));

        tachometer  = new Tachometer();
        slider      = new Slider();
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(10000f);
        slider.setBlockIncrement(0.1f);


        colorPicker = new ColorPicker();
        stateSwitch = new RadioButton("in Betrieb");
    }

    private void layoutControls() {
        VBox controlPane = new VBox(new Label("Gauge Control"),
                                    slider, stateSwitch);
        controlPane.setPadding(new Insets(10, 50, 0, 50));
        controlPane.setSpacing(10);

        setCenter(tachometer);
        setBottom(controlPane);
    }

    private void setupEventhandlers() {

    }

    private void setupBindings() {
        slider.valueProperty().bindBidirectional(pm.pmValueProperty());
        slider.maxProperty().bindBidirectional(pm.maxProperty());
        colorPicker.valueProperty().bindBidirectional(pm.baseColorProperty());
        stateSwitch.selectedProperty().bindBidirectional(pm.runningProperty());

        //bindings Controls to pm
        tachometer.getGauge().valueProperty().bindBidirectional(pm.pmValueProperty());
        tachometer.valueProperty().bindBidirectional(pm.pmValueProperty());
        tachometer.baseColorProperty().bindBidirectional(pm.baseColorProperty());
        tachometer.onProperty().bindBidirectional(pm.runningProperty());
        tachometer.minValueProperty().bindBidirectional(pm.minProperty());
        tachometer.maxValueProperty().bindBidirectional(pm.maxProperty());
        tachometer.animatedProperty().bindBidirectional(pm.animatedProperty());
    }

}
