package cuie.project.template_simplecontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;
import org.w3c.dom.css.Rect;

/**
 *
 * The tachometer displays the performance of the windmill
 * through the propeller and the value in the middle
 * in addition you can see and change the state of the windmill with the radionbutton in the middle
 *
 * @author Cristine Paduga / Chi Cuong Nguyen
 */

public class Tachometer extends Region {
    // wird gebraucht fuer StyleableProperties
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

    private static final double ARTBOARD_WIDTH  = 500;  // ToDo: Breite der "Zeichnung" aus dem Grafik-Tool übernehmen
    private static final double ARTBOARD_HEIGHT = 500;  // ToDo: Anpassen an die Breite der Zeichnung

    private static final double ASPECT_RATIO = ARTBOARD_WIDTH / ARTBOARD_HEIGHT;

    private static final double MINIMUM_WIDTH  = 25;    // ToDo: Anpassen
    private static final double MINIMUM_HEIGHT = MINIMUM_WIDTH / ASPECT_RATIO;

    private static final double MAXIMUM_WIDTH = 800;    // ToDo: Anpassen

    // ToDo: diese Parts durch alle notwendigen Parts der gewünschten CustomControl ersetzen
    private Circle      backgroundCircle;
    private Text        display;
    private Circle      thumb;
    private Rectangle   frame;
    private Group       ticks;
    private List<Text>  tickLabels;
    private Polygon     propeller;

    // ToDo: ersetzen durch alle notwendigen Properties der CustomControl
    private final DoubleProperty value = new SimpleDoubleProperty();
    private final BooleanProperty on    = new SimpleBooleanProperty();
    private final DoubleProperty minPerformance = new SimpleDoubleProperty();
    private final DoubleProperty maxPerformance = new SimpleDoubleProperty();


    // ToDo: ergänzen mit allen CSS stylable properties
    private static final CssMetaData<Tachometer, Color> BASE_COLOR_META_DATA = FACTORY.createColorCssMetaData("-base-color", s -> s.baseColor);

    private final StyleableObjectProperty<Color> baseColor = new SimpleStyleableObjectProperty<Color>(BASE_COLOR_META_DATA) {
        @Override
        protected void invalidated() {
            setStyle(String.format("%s: %s;", getCssMetaData().getProperty(), colorToCss(get())));
            applyCss();
        }
    };

    // ToDo: Loeschen falls keine getaktete Animation benoetigt wird
    private final BooleanProperty          blinking = new SimpleBooleanProperty(false);
    private final ObjectProperty<Duration> pulse    = new SimpleObjectProperty<>(Duration.seconds(1.0));


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

    // ToDo: alle Animationen und Timelines deklarieren
    private Transition onTransition;
    private Transition offTransition;

    //private final Timeline timeline = new Timeline();


    // fuer Resizing benoetigt
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
        //ToDo: alle deklarierten Parts initialisieren
        double center = ARTBOARD_WIDTH * (0.5);

        backgroundCircle = new Circle(center, center, center * 0.3);
        backgroundCircle.getStyleClass().add("background-circle");

        display = createCenteredText("display");

        thumb = new Circle(12,13,10);
        thumb.getStyleClass().add("thumb");
        thumb.setStrokeWidth(0);
        thumb.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0,0,0,0.3),4,
                0,0,1));

        frame = new Rectangle(5.0, 5.0, ARTBOARD_WIDTH - (93.5 * 5), ARTBOARD_HEIGHT - (97.0 * 5));
        frame.getStyleClass().add("frame");
        frame.setMouseTransparent(true);

        propeller = new Polygon();
        propeller.getStyleClass().add("propeller");
        propeller.getPoints().setAll(
                ARTBOARD_HEIGHT/2, 30.0, //Spitze oben
                190.0, 200.0, // Spitze links
                310.0, 200.0 // Spitze rechts
        );

        ticks = createTicks(center, center, 225, 90, 0, 360, 6, "tick");

        tickLabels = new ArrayList<>();

        int labelCount = 8;
        for (int i = 0; i < labelCount; i++){
            double r = 95;
            double angle = i * 360 / labelCount;

            Point2D p   = pointOnCircle(center, center, 235, angle);
            Text tickLabel = createCenteredText(p.getX(), p.getY(), "tick-label");
            tickLabels.add(tickLabel);
        }
        updateTickLabels();
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

        onTransition = new ParallelTransition(onTranslation, onFill, onFillThumb);

        TranslateTransition offTranslation = new TranslateTransition(Duration.millis(500), thumb);
        offTranslation.setFromX(16);
        offTranslation.setToX(0);

        FillTransition offFill = new FillTransition(Duration.millis(500), frame);
        offFill.setFromValue(FRAME_ON);
        offFill.setToValue(FRAME_OFF);

        FillTransition offFillThumb = new FillTransition(Duration.millis(500), thumb);
        offFillThumb.setFromValue(THUMB_ON);
        offFillThumb.setToValue(THUMB_OFF);

        offTransition = new ParallelTransition(offTranslation, offFill, offFillThumb);
    }

    private void layoutParts() {
        //ToDo: alle Parts zur drawingPane hinzufügen
        drawingPane.getChildren().addAll(propeller, backgroundCircle, display, frame, thumb, ticks);
        drawingPane.getChildren().addAll(tickLabels);

        getChildren().add(drawingPane);
    }

    private void setupEventHandlers() {
        //ToDo: bei Bedarf ergänzen
        drawingPane.setOnMouseClicked(event -> setOn(!isOn()));
    }

    private void setupValueChangeListeners() {
        //ToDo: durch die Listener auf die Properties des Custom Controls ersetzen
        valueProperty().addListener((observable, oldValue, newValue) -> updateUI());

        // fuer die getaktete Animation
        blinking.addListener((observable, oldValue, newValue) -> startClockedAnimation(newValue));

        onProperty().addListener((observable, oldValue, newValue) -> updateUI());

    }

    private void setupBindings() {
        //ToDo: dieses Binding ersetzen
        display.textProperty().bind(valueProperty().asString(CH, "%.2f"));
    }

    private void updateUI(){
        //ToDo : ergaenzen mit dem was bei einer Wertaenderung einer Status-Property im UI upgedated werden muss
        /*if (isOn()) {
            thumb.setLayoutX(16);
            thumb.setFill(Paint.valueOf("#32CD32"));
            frame.setFill(Paint.valueOf("#BCEE68"));
        }
        else {
            thumb.setLayoutX(0);
            thumb.setFill(Paint.valueOf("#3E82F7"));
            frame.setFill(Paint.valueOf("#A2C5FF"));
        }*/
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

    private void updateTickLabels() {
        int labelCount = tickLabels.size();
        double step    = (getMaxPerformance() - getMinPerformance()) / labelCount;
        for (int i = 0; i < labelCount; i++) {
            Text tickLabel = tickLabels.get(i);
            tickLabel.setText(String.format("%.0f", getMinPerformance() + (i * step)));
        }

    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        resize();
    }

    //ToDo: ueberpruefen ob dieser Resizing-Ansatz anwendbar ist.
    private void resize() {
        Insets padding         = getPadding();
        double availableWidth  = getWidth() - padding.getLeft() - padding.getRight();
        double availableHeight = getHeight() - padding.getTop() - padding.getBottom();

        double width = Math.max(Math.min(Math.min(availableWidth, availableHeight * ASPECT_RATIO), MAXIMUM_WIDTH), MINIMUM_WIDTH);

        double scalingFactor = width / ARTBOARD_WIDTH;

        if (availableWidth > 0 && availableHeight > 0) {
            //ToDo: ueberpruefen ob die drawingPane immer zentriert werden soll (eventuell ist zum Beispiel linksbuendig angemessener)
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

    //ToDo: diese Funktionen anschauen und für die Umsetzung des CustomControls benutzen

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
        return createCenteredText(ARTBOARD_WIDTH * 0.5, ARTBOARD_HEIGHT * 0.5, styleClass);
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

    // alle getter und setter  (generiert via "Code -> Generate... -> Getter and Setter)

    // ToDo: ersetzen durch die Getter und Setter Ihres CustomControls
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

    public boolean isBlinking() {
        return blinking.get();
    }

    public BooleanProperty blinkingProperty() {
        return blinking;
    }

    public void setBlinking(boolean blinking) {
        this.blinking.set(blinking);
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

    public double getMaxPerformance() {
        return maxPerformance.get();
    }

    public DoubleProperty maxPerformanceProperty() {
        return maxPerformance;
    }

    public void setMaxPerformance(double maxPerformance) {
        this.maxPerformance.set(maxPerformance);
    }

    public double getMinPerformance() {
        return minPerformance.get();
    }

    public DoubleProperty minPerformanceProperty() {
        return minPerformance;
    }

    public void setMinPerformance(double minPerformance) {
        this.minPerformance.set(minPerformance);
    }
}