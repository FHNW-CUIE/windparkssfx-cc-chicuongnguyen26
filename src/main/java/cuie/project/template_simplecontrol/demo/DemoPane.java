package cuie.project.template_simplecontrol.demo;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import cuie.project.template_simplecontrol.Tachometer;
import javafx.util.converter.NumberStringConverter;

public class DemoPane extends BorderPane {

    private final PresentationModel pm;

    // declare the custom control
    private Tachometer tachometer;


    // all controls
    private Slider      slider;
    private ColorPicker colorPicker;
    private RadioButton stateSwitch;
    private TextField   maxField;

    public DemoPane(PresentationModel pm) {
        this.pm = pm;
        initializeControls();
        layoutControls();
        setupBindings();
    }

    private void initializeControls() {
        setPadding(new Insets(10));

        tachometer  = new Tachometer();
        slider      = new Slider();
        slider.setShowTickLabels(true);

        colorPicker = new ColorPicker();
        stateSwitch = new RadioButton("running");

        maxField    = new TextField();

    }

    private void layoutControls() {
        VBox controlPane = new VBox(new Label("SimpleControl Properties"),
                                    slider, new Label("Max Performance"), maxField, stateSwitch, colorPicker);
        controlPane.setPadding(new Insets(0, 50, 0, 50));
        controlPane.setSpacing(10);

        setCenter(tachometer);
        setRight(controlPane);
    }

    private void setupBindings() {
        slider.valueProperty().bindBidirectional(pm.pmValueProperty());
        colorPicker.valueProperty().bindBidirectional(pm.baseColorProperty());
        stateSwitch.selectedProperty().bindBidirectional(pm.runningProperty());
        maxField.textProperty().bindBidirectional(pm.maxProperty(), new NumberStringConverter());

        //bindings Controls to pm
        tachometer.valueProperty().bindBidirectional(pm.pmValueProperty());
        tachometer.baseColorProperty().bindBidirectional(pm.baseColorProperty());
        tachometer.onProperty().bindBidirectional(pm.runningProperty());
        tachometer.maxPerformanceProperty().bindBidirectional(pm.maxProperty());
        tachometer.minPerformanceProperty().bind(pm.minProperty());
    }

}
