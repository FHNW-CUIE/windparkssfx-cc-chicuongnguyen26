package cuie.project.template_simplecontrol.demo;

import javafx.geometry.Insets;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import cuie.project.template_simplecontrol.Tachometer;

public class DemoPane extends BorderPane {

    private final PresentationModel pm;

    // declare the custom control
    private Tachometer tachometer;


    // all controls
    private Slider      slider;
    private ColorPicker colorPicker;
    private RadioButton stateSwitch;

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

    }

    private void layoutControls() {
        VBox controlPane = new VBox(new Label("SimpleControl Properties"),
                                    slider, stateSwitch, colorPicker);
        controlPane.setPadding(new Insets(0, 50, 0, 50));
        controlPane.setSpacing(10);

        setCenter(tachometer);
        setRight(controlPane);
    }

    private void setupBindings() {
        slider.valueProperty().bindBidirectional(pm.pmValueProperty());
        colorPicker.valueProperty().bindBidirectional(pm.baseColorProperty());
        stateSwitch.selectedProperty().bindBidirectional(pm.runningProperty());

        //bindings Controls to pm
        tachometer.valueProperty().bindBidirectional(pm.pmValueProperty());
        tachometer.baseColorProperty().bindBidirectional(pm.baseColorProperty());
        tachometer.onProperty().bindBidirectional(pm.runningProperty());
    }

}
