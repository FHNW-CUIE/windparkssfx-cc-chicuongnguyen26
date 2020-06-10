package cuie.project.template_simplecontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import eu.hansolo.medusa.GaugeBuilder;


import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;
import eu.hansolo.medusa.Gauge;


/**
 *
 * The tachometer displays the performance of the windmill
 * through the pointer and the value in the middle
 * in addition you can see and change the state of the windmill with the radionbutton in the middle
 *
 * @author Cristine Paduga / Chi Cuong Nguyen
 */

public class Tachometer extends Region {

    // wird gebraucht fuer StyleableProperties
    private static final Color THUMB_ON  = Color.rgb( 62, 130, 247);
    private static final Color THUMB_OFF = Color.rgb(250, 250, 250);
    private static final Color FRAME_ON  = Color.rgb(162, 197, 255);
    private static final Color FRAME_OFF = Color.rgb(153, 153, 153);
    private static final Color PROPELLER_ON = Color.rgb(60, 152, 172);
    private static final Color PROPELLER_OFF = Color.rgb(183, 193, 197);

    private static final StyleablePropertyFactory<Tachometer> FACTORY = new StyleablePropertyFactory<>(Region.getClassCssMetaData());
    //Animation switch
    private static final Color THUMB_ON  = Color.rgb( 62, 130, 247);
    private static final Color THUMB_OFF = Color.rgb(250, 250, 250);
    private static final Color FRAME_ON  = Color.rgb(162, 197, 255);
    private static final Color FRAME_OFF = Color.rgb(153, 153, 153);

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return FACTORY.getCssMetaData();
    }

    private static final Locale CH = new Locale("de", "CH");

    private static final double ARTBOARD_WIDTH  = 300;  // ToDo: Breite der "Zeichnung" aus dem Grafik-Tool übernehmen
    private static final double ARTBOARD_HEIGHT = 200;  // ToDo: Anpassen an die Breite der Zeichnung

    private static final double ASPECT_RATIO = ARTBOARD_WIDTH / ARTBOARD_HEIGHT;

    private static final double MINIMUM_WIDTH  = 25;    //
    private static final double MINIMUM_HEIGHT = MINIMUM_WIDTH / ASPECT_RATIO;

    private static final double MAXIMUM_WIDTH = 800;    //

    //dial tachometer
    private Gauge   gauge;
    private Button  button;

    //head windmill
    private Circle      backgroundCircle;
    private Text        display;
    private Polygon     propeller;
    private Rectangle   pillar;

    //Switch
    private Circle      thumb;
    private Rectangle   frame;

    // all properties
    private final DoubleProperty    value           = new SimpleDoubleProperty();
    private final BooleanProperty   on              = new SimpleBooleanProperty();
    private final DoubleProperty    minValue        = new SimpleDoubleProperty(0);
    private final DoubleProperty    maxValue        = new SimpleDoubleProperty(1000);
    private final BooleanProperty   animated        = new SimpleBooleanProperty(true);


    private static final CssMetaData<Tachometer, Color> BASE_COLOR_META_DATA = FACTORY.createColorCssMetaData("-base-color", s -> s.baseColor);

    private final StyleableObjectProperty<Color> baseColor = new SimpleStyleableObjectProperty<Color>(BASE_COLOR_META_DATA) {
        @Override
        protected void invalidated() {
            setStyle(String.format("%s: %s;", getCssMetaData().getProperty(), colorToCss(get())));
            applyCss();
        }
    };

    private final ObjectProperty<Duration> pulse    = new SimpleObjectProperty<>(Duration.seconds(1.0));

    // all animations
    private Transition onTransition;
    private Transition offTransition;

    private final AnimationTimer timer = new AnimationTimer() {
        private long lastTimerCall;

        @Override
        public void handle(long now) {
            if (now > lastTimerCall + (getPulse().toMillis() * 1_000_000L)) {
                performPeriodicTask();
                lastTimerCall = now;
            }
        }
    };

    // animation & timelines
    private Transition onTransition;
    private Transition offTransition;

    // resizing
    private Pane drawingPane;

    public Tachometer() {
        initializeSelf();
        initializeParts();
        initializeDrawingPane();
        initializeAnimations();
        layoutParts();
        setupEventHandlers();
        setupValueChangeListeners();
        setupBindings();
    }

    private void initializeSelf() {
        loadFonts("/fonts/Lato/Lato-Lig.ttf", "/fonts/Lato/Lato-Reg.ttf");
        addStylesheetFiles("style.css");
        getStyleClass().add("tachometer");

    }

    private void initializeParts() {
        gauge = GaugeBuilder.create()
                            .skinType(Gauge.SkinType.GAUGE)
                            .borderWidth(3.0)
                            .startAngle(290)
                            .angleRange(220)
                            .customTickLabels("0", "10000", "20000", "30000", "40000")
                            .customTickLabelFontSize(5)
                            .minorTickMarksVisible(true)
                            .needleColor(Color.rgb(70, 130,180))
                            .needleSize(Gauge.NeedleSize.THIN)
                            .prefSize(300, 300)
                            .minValue(0)
                            .maxValue(40000)
                            .valueVisible(false)
                            .build();

        button = new Button();
        button.setOnMousePressed(event -> gauge.setValue(value.doubleValue() * gauge.getRange() + gauge.getMinValue()));

        backgroundCircle = new Circle(150, 140, 35);
        backgroundCircle.getStyleClass().add("background-circle");

        display = createCenteredText("display");

        propeller = new Polygon();
        propeller.getStyleClass().add("propeller");
        propeller.getPoints().setAll(
                150.0, 20.0, //Spitze oben
                130.0, 129.9, // Ecke links oben
                40.1, 205.0, // Spitze links
                150.0, 149.9, // Mitte
                259.9, 205.0, // Spitze rechts
                170.0, 129.9 // Ecke rechts oben
        );

        thumb = new Circle(backgroundCircle.getCenterX()* 0.94, backgroundCircle.getCenterY() * 0.867,7);
        thumb.getStyleClass().add("thumb");
        thumb.setStrokeWidth(0);
        thumb.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.3),4,
                0,0,1));

        frame = new Rectangle(backgroundCircle.getCenterX() * 0.92 , backgroundCircle.getCenterY() * 0.83, 25, 10);
        frame.getStyleClass().add("frame");
        frame.setMouseTransparent(true);

        pillar = new Rectangle();
        pillar.getStyleClass().add("pillar");
        pillar.setX(130);
        pillar.setY(150);
        pillar.setWidth(40);
        pillar.setHeight(57);
    }

    private void initializeDrawingPane() {
        drawingPane = new Pane();
        drawingPane.getStyleClass().add("drawing-pane");
        drawingPane.setMaxSize(ARTBOARD_WIDTH,  ARTBOARD_HEIGHT);
        drawingPane.setMinSize(ARTBOARD_WIDTH,  ARTBOARD_HEIGHT);
        drawingPane.setPrefSize(ARTBOARD_WIDTH, ARTBOARD_HEIGHT);
    }

    private void initializeAnimations(){
        //ToDo: alle deklarierten Animationen initialisieren
        TranslateTransition onTranslation = new TranslateTransition(Duration.millis(500), thumb);
        onTranslation.setFromX(0);
        onTranslation.setToX(16);

        FillTransition onFill = new FillTransition(Duration.millis(500), frame);
        onFill.setFromValue(FRAME_OFF);
        onFill.setToValue(FRAME_ON);

        FillTransition onFillThumb = new FillTransition(Duration.millis(500), thumb);
        onFillThumb.setFromValue(THUMB_OFF);
        onFillThumb.setToValue(THUMB_ON);


        FillTransition onFillPropeller = new FillTransition(Duration.millis(500), propeller);
        onFillPropeller.setFromValue(PROPELLER_OFF);
        onFillPropeller.setToValue(PROPELLER_ON);


        onTransition = new ParallelTransition(onTranslation, onFill, onFillThumb, onFillPropeller);


        TranslateTransition offTranslation = new TranslateTransition(Duration.millis(500), thumb);
        offTranslation.setFromX(16);
        offTranslation.setToX(0);

        FillTransition offFill = new FillTransition(Duration.millis(500), frame);
        offFill.setFromValue(FRAME_ON);
        offFill.setToValue(FRAME_OFF);

        FillTransition offFillThumb = new FillTransition(Duration.millis(500), thumb);
        offFillThumb.setFromValue(THUMB_ON);
        offFillThumb.setToValue(THUMB_OFF);


        FillTransition offFillPropeller = new FillTransition(Duration.millis(500), propeller);
        offFillPropeller.setFromValue(PROPELLER_ON);
        offFillPropeller.setToValue(PROPELLER_OFF);

        offTransition = new ParallelTransition(offTranslation, offFill, offFillThumb, offFillPropeller);


    }

    private void layoutParts() {
        drawingPane.getChildren().addAll(pillar, propeller, gauge, backgroundCircle, display, frame, thumb);
        getChildren().add(drawingPane);
    }

    private void setupEventHandlers() {
        thumb.setOnMouseClicked(event -> setOn(!isOn()));
    }

    private void setupValueChangeListeners() {

        valueProperty().addListener((observable, oldValue, newValue) -> rotateProperty());
        onProperty().addListener((observable, oldValue, newValue) -> updateUI());

    }

    private void setupBindings() {
        display.textProperty().bind(valueProperty().asString(CH, "%.2f" + " kW"));
    }

    private void updateUI(){
        onTransition.stop();
        offTransition.stop();
        if(isOn()){
            onTransition.play();
        }else {
            offTransition.play();
        }
    }

    private void performPeriodicTask(){
        //ToDo: ergaenzen mit dem was bei der getakteten Animation gemacht werden muss
        //normalerweise: den Wert einer der Status-Properties aendern
    }

    private void startClockedAnimation(boolean start) {
        if (start) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        resize();
    }

    private void resize() {
        Insets padding         = getPadding();
        double availableWidth  = getWidth() - padding.getLeft() - padding.getRight();
        double availableHeight = getHeight() - padding.getTop() - padding.getBottom();

        double width = Math.max(Math.min(Math.min(availableWidth, availableHeight * ASPECT_RATIO), MAXIMUM_WIDTH), MINIMUM_WIDTH);

        double scalingFactor = width / ARTBOARD_WIDTH;

        if (availableWidth > 0 && availableHeight > 0) {
            relocateDrawingPaneCentered();
            drawingPane.setScaleX(scalingFactor);
            drawingPane.setScaleY(scalingFactor);
        }
    }

    private void relocateDrawingPaneCentered() {
        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, (getHeight() - ARTBOARD_HEIGHT) * 0.5);
    }

    private void relocateDrawingPaneCenterBottom(double scaleY, double paddingBottom) {
        double visualHeight = ARTBOARD_HEIGHT * scaleY;
        double visualSpace  = getHeight() - visualHeight;
        double y            = visualSpace + (visualHeight - ARTBOARD_HEIGHT) * 0.5 - paddingBottom;

        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, y);
    }

    private void relocateDrawingPaneCenterTop(double scaleY, double paddingTop) {
        double visualHeight = ARTBOARD_HEIGHT * scaleY;
        double y            = (visualHeight - ARTBOARD_HEIGHT) * 0.5 + paddingTop;

        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, y);
    }

    // Sammlung nuetzlicher Funktionen

    private void loadFonts(String... font){
        for(String f : font){
            Font.loadFont(getClass().getResourceAsStream(f), 0);
        }
    }

    private void addStylesheetFiles(String... stylesheetFile){
        for(String file : stylesheetFile){
            String stylesheet = getClass().getResource(file).toExternalForm();
            getStylesheets().add(stylesheet);
        }
    }

    /**
     * Umrechnen einer Prozentangabe, zwischen 0 und 100, in den tatsaechlichen Wert innerhalb des angegebenen Wertebereichs.
     *
     * @param percentage Wert in Prozent
     * @param minValue untere Grenze des Wertebereichs
     * @param maxValue obere Grenze des Wertebereichs
     * @return value der akuelle Wert
     */
    private double percentageToValue(double percentage, double minValue, double maxValue){
        return ((maxValue - minValue) * percentage) + minValue;
    }

    /**
     * Umrechnen des angegebenen Werts in eine Prozentangabe zwischen 0 und 100.
     *
     * @param value der aktuelle Wert
     * @param minValue untere Grenze des Wertebereichs
     * @param maxValue obere Grenze des Wertebereichs
     * @return Prozentangabe des aktuellen Werts
     */
    private double valueToPercentage(double value, double minValue, double maxValue) {
        return (value - minValue) / (maxValue - minValue);
    }

    /**
     * Berechnet den Winkel zwischen 0 und 360 Grad, 0 Grad entspricht "Nord", der dem value
     * innerhalb des Wertebereichs zwischen minValue und maxValue entspricht.
     *
     * @param value der aktuelle Wert
     * @param minValue untere Grenze des Wertebereichs
     * @param maxValue obere Grenze des Wertebereichs
     * @return angle Winkel zwischen 0 und 360 Grad
     */
    private double valueToAngle(double value, double minValue, double maxValue) {
        return percentageToAngle(valueToPercentage(value, minValue, maxValue));
    }

    /**
     * Umrechnung der Maus-Position auf den aktuellen Wert.
     *
     * Diese Funktion ist sinnvoll nur fuer radiale Controls einsetzbar.
     *
     * Lineare Controls wie Slider müssen auf andere Art die Mausposition auf den value umrechnen.
     *
     * @param mouseX x-Position der Maus
     * @param mouseY y-Position der Maus
     * @param cx x-Position des Zentrums des radialen Controls
     * @param cy y-Position des Zentrums des radialen Controls
     * @param minValue untere Grenze des Wertebereichs
     * @param maxValue obere Grenze des Wertebereichs
     * @return value der dem Winkel entspricht, in dem die Maus zum Mittelpunkt des radialen Controls steht
     */
    private double radialMousePositionToValue(double mouseX, double mouseY, double cx, double cy, double minValue, double maxValue){
        double percentage = angleToPercentage(angle(cx, cy, mouseX, mouseY));

        return percentageToValue(percentage, minValue, maxValue);
    }

    /**
     * Umrechnung eines Winkels, zwischen 0 und 360 Grad, in eine Prozentangabe.
     *
     * Diese Funktion ist sinnvoll nur fuer radiale Controls einsetzbar.
     *
     * @param angle der Winkel
     * @return die entsprechende Prozentangabe
     */
    private double angleToPercentage(double angle){
        return angle / 360.0;
    }

    /**
     * Umrechnung einer Prozentangabe, zwischen 0 und 100, in den entsprechenden Winkel.
     *
     * Diese Funktion ist sinnvoll nur fuer radiale Controls einsetzbar.
     *
     * @param percentage die Prozentangabe
     * @return der entsprechende Winkel
     */
    private double percentageToAngle(double percentage){
        return 360.0 * percentage;
    }

    /**
     * Berechnet den Winkel zwischen einem Zentrums-Punkt und einem Referenz-Punkt.
     *
     * @param cx x-Position des Zentrums
     * @param cy y-Position des Zentrums
     * @param x x-Position des Referenzpunkts
     * @param y y-Position des Referenzpunkts
     * @return winkel zwischen 0 und 360 Grad
     */
    private double angle(double cx, double cy, double x, double y) {
        double deltaX = x - cx;
        double deltaY = y - cy;
        double radius = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        double nx     = deltaX / radius;
        double ny     = deltaY / radius;
        double theta  = Math.toRadians(90) + Math.atan2(ny, nx);

        return Double.compare(theta, 0.0) >= 0 ? Math.toDegrees(theta) : Math.toDegrees((theta)) + 360.0;
    }

    /**
     * Berechnet den Punkt auf einem Kreis mit gegebenen Radius im angegebenen Winkel
     *
     * @param cX x-Position des Zentrums
     * @param cY y-Position des Zentrums
     * @param radius Kreisradius
     * @param angle Winkel zwischen 0 und 360 Grad
     * @return Punkt auf dem Kreis
     */
    private Point2D pointOnCircle(double cX, double cY, double radius, double angle) {
        return new Point2D(cX - (radius * Math.sin(Math.toRadians(angle - 180))),
                           cY + (radius * Math.cos(Math.toRadians(angle - 180))));
    }

    /**
     * Erzeugt eine Text-Instanz in der Mitte des CustomControls.
     * Der Text bleibt zentriert auch wenn der angezeigte Text sich aendert.
     *
     * @param styleClass mit dieser StyleClass kann der erzeugte Text via css gestyled werden
     * @return Text
     */
    private Text createCenteredText(String styleClass) {
        return createCenteredText(backgroundCircle.getCenterX() , backgroundCircle.getCenterY(), styleClass);
    }

    /**
     * Erzeugt eine Text-Instanz mit dem angegebenen Zentrum.
     * Der Text bleibt zentriert auch wenn der angezeigte Text sich aendert.
     *
     * @param cx x-Position des Zentrumspunkt des Textes
     * @param cy y-Position des Zentrumspunkt des Textes
     * @param styleClass mit dieser StyleClass kann der erzeugte Text via css gestyled werden
     * @return Text
     */
    private Text createCenteredText(double cx, double cy, String styleClass) {
        Text text = new Text();
        text.getStyleClass().add(styleClass);
        text.setTextOrigin(VPos.CENTER);
        text.setTextAlignment(TextAlignment.CENTER);
        double width = cx > ARTBOARD_WIDTH * 0.5 ? ((ARTBOARD_WIDTH - cx) * 2.0) : cx * 2.0;
        text.setWrappingWidth(width);
        text.setBoundsType(TextBoundsType.VISUAL);
        text.setY(cy);
        text.setX(cx - (width / 2.0));

        return text;
    }

    /**
     * Erzeugt eine Group von Lines, die zum Beispiel fuer Skalen oder Zifferblaetter verwendet werden koennen.
     *
     * Diese Funktion ist sinnvoll nur fuer radiale Controls einsetzbar.
     * @param cx x-Position des Zentrumspunkts
     * @param cy y-Position des Zentrumspunkts
     * @param radius radius auf dem die Anfangspunkte der Ticks liegen
     * @param numberOfTicks gewuenschte Anzahl von Ticks
     * @param startingAngle Wickel in dem der erste Tick liegt, zwischen 0 und 360 Grad
     * @param overallAngle gewuenschter Winkel zwischen den erzeugten Ticks, zwischen 0 und 360 Grad
     * @param tickLength Laenge eines Ticks
     * @param styleClass Name der StyleClass mit der ein einzelner Tick via css gestyled werden kann
     * @return Group mit allen Ticks
     */
    private Group createTicks(double cx, double cy, double radius, int numberOfTicks, double startingAngle, double overallAngle,  double tickLength, String styleClass) {
        Group group = new Group();

        double degreesBetweenTicks = overallAngle == 360 ?
                                     overallAngle / numberOfTicks :
                                     overallAngle /(numberOfTicks - 1);
        double innerRadius         = radius - tickLength;

        for (int i = 0; i < numberOfTicks; i++) {
            double angle = startingAngle + i * degreesBetweenTicks;

            Point2D startPoint = pointOnCircle(cx, cy, radius,      angle);
            Point2D endPoint   = pointOnCircle(cx, cy, innerRadius, angle);

            Line tick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            tick.getStyleClass().add(styleClass);
            group.getChildren().add(tick);
        }

        return group;
    }

    private String colorToCss(final Color color) {
  		return color.toString().replace("0x", "#");
  	}


    // compute sizes

    @Override
    protected double computeMinWidth(double height) {
        Insets padding           = getPadding();
        double horizontalPadding = padding.getLeft() + padding.getRight();

        return MINIMUM_WIDTH + horizontalPadding;
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets padding         = getPadding();
        double verticalPadding = padding.getTop() + padding.getBottom();

        return MINIMUM_HEIGHT + verticalPadding;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets padding           = getPadding();
        double horizontalPadding = padding.getLeft() + padding.getRight();

        return ARTBOARD_WIDTH + horizontalPadding;
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets padding         = getPadding();
        double verticalPadding = padding.getTop() + padding.getBottom();

        return ARTBOARD_HEIGHT + verticalPadding;
    }

    // getter & setters
    public double getValue() {
        return value.get();
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public Color getBaseColor() {
        return baseColor.get();
    }

    public StyleableObjectProperty<Color> baseColorProperty() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor.set(baseColor);
    }

    public Duration getPulse() {
        return pulse.get();
    }

    public ObjectProperty<Duration> pulseProperty() {
        return pulse;
    }

    public void setPulse(Duration pulse) {
        this.pulse.set(pulse);
    }

    public boolean isOn() {
        return on.get();
    }

    public BooleanProperty onProperty() {
        return on;
    }

    public void setOn(boolean on) {
        this.on.set(on);
    }

    public double getMinValue() {
        return minValue.get();
    }

    public DoubleProperty minValueProperty() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue.set(minValue);
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue.set(maxValue);
    }

    public boolean isAnimated() {
        return animated.get();
    }

    public BooleanProperty animatedProperty() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated.set(animated);
    }

    public Polygon getPropeller() {
        return propeller;
    }

    public void setPropeller(Polygon propeller) {
        this.propeller = propeller;
    }

    public Gauge getGauge() {
        return gauge;
    }

    public void setGauge(Gauge gauge) {
        this.gauge = gauge;
    }
}