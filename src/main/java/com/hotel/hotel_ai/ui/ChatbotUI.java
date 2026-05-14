package com.hotel.hotel_ai.ui;

import com.hotel.hotel_ai.controller.*;
import com.hotel.hotel_ai.model.*;
import com.hotel.hotel_ai.repository.DatabaseManager;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChatbotUI extends Application {

    // ── Theme palette: [bg, surface, surface2, accent, accent2, text, muted, inputBg] ──
    private static final String[][] THEMES = {
        {"#020617", "#0F172A", "#1E293B", "#F59E0B", "#D97706", "#F8FAFC", "#475569", "#0A1628"},
        {"#020B18", "#051829", "#0A2A45", "#0891B2", "#0E7490", "#F0F9FF", "#4A8CAA", "#030F1C"},
        {"#12060A", "#1C0310", "#2C0618", "#F43F5E", "#E11D48", "#FFF1F2", "#9B6070", "#0E0208"},
        {"#01040A", "#060418", "#0D0A28", "#7C3AED", "#6D28D9", "#EEF2FF", "#6B6BA0", "#030210"}
    };
    private static final String[] THEME_IDS   = {"dark", "ocean", "sunset", "void"};
    private static final String[] THEME_NAMES = {"Dark", "Ocean", "Sunset", "Void"};
    private int themeIdx = 0;

    // theme helpers
    private String tb()  { return THEMES[themeIdx][0]; }
    private String ts()  { return THEMES[themeIdx][1]; }
    private String ts2() { return THEMES[themeIdx][2]; }
    private String ta()  { return THEMES[themeIdx][3]; }
    private String tt()  { return THEMES[themeIdx][5]; }
    private String tm()  { return THEMES[themeIdx][6]; }

    private Stage primaryStage;
    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private RoomController roomController;
    private ReservationController reservationController;
    private CustomerController customerController;
    private AIEngine aiEngine;
    private ApiServer apiServer;
    private String activeNav = "dashboard";
    private Button[] navButtons;
    private static final String BOTPRESS_URL =
        "https://cdn.botpress.cloud/webchat/v3.6/shareable.html?configUrl=https://files.bpcontent.cloud/2026/05/02/04/20260502045413-AUI6NCFO.json";

    public String sendMessageToAI(String msg) { return aiEngine.sendMessageToAI(msg); }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        DatabaseManager.getInstance().initializeDatabase();
        initControllers();
        stage.setMaximized(true);
        showLoginScreen();
    }

    private void initControllers() {
        roomController        = new RoomController();
        reservationController = new ReservationController();
        customerController    = new CustomerController();
        aiEngine              = new AIEngine();
        apiServer             = new ApiServer();
        apiServer.start();
    }

    // ════════════════════════════════════════════════════════════
    //  LOGIN
    // ════════════════════════════════════════════════════════════
    private void showLoginScreen() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + THEMES[themeIdx][0] + ";");
        root.getStyleClass().add("theme-" + THEME_IDS[themeIdx]);

        // animated ambient blobs
        double[][] blobData = {{300, -420, -200}, {220, 450, 310}, {150, -180, 330}, {100, 350, -250}};
        String[] blobColors = {THEMES[themeIdx][3], "#4F46E5", THEMES[themeIdx][3], THEMES[themeIdx][4]};
        double[] blobAlphas = {0.07, 0.07, 0.04, 0.04};
        for (int i = 0; i < 4; i++) {
            Circle blob = new Circle(blobData[i][0]);
            blob.setFill(Color.web(blobColors[i], blobAlphas[i]));
            blob.setTranslateX(blobData[i][1]); blob.setTranslateY(blobData[i][2]);
            root.getChildren().add(blob);
            ScaleTransition pulse = new ScaleTransition(Duration.seconds(3.5 + i * 0.8), blob);
            pulse.setFromX(1.0); pulse.setToX(1.15);
            pulse.setFromY(1.0); pulse.setToY(1.15);
            pulse.setAutoReverse(true); pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setDelay(Duration.seconds(i * 0.6));
            pulse.play();
        }

        // Splash: full-screen logo at reduced opacity — shown alone for 5s before login card appears
        ImageView splashLogoRef = null;
        try {
            ImageView sImg = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png")));
            sImg.setPreserveRatio(true);
            sImg.setOpacity(0);
            sImg.setMouseTransparent(true);
            root.getChildren().add(sImg);
            splashLogoRef = sImg;
        } catch (Exception ignored) {}
        final ImageView splashLogo = splashLogoRef;

        // dark glass card
        VBox card = new VBox(20);
        card.setStyle(
            "-fx-background-color: rgba(10,18,35,0.97);" +
            "-fx-background-radius: 24; -fx-border-radius: 24;" +
            "-fx-border-color: rgba(245,158,11,0.18); -fx-border-width: 1;" +
            "-fx-padding: 48 52;" +
            "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.05), 80, 0, 0, 0);"
        );
        card.setMaxWidth(440); card.setMinWidth(440); card.setAlignment(Pos.CENTER);

        // subtle border glow pulse on the card
        Timeline borderPulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(2.5),
                new KeyValue(card.opacityProperty(), 0.96)),
            new KeyFrame(Duration.seconds(5),
                new KeyValue(card.opacityProperty(), 1.0))
        );
        borderPulse.setCycleCount(Timeline.INDEFINITE);
        // NOTE: borderPulse.play() is deferred — starting it here would immediately set
        // card.opacity = 1.0 (its Duration.ZERO keyframe), which would break the splash.
        // It is started inside the cardIn.setOnFinished callback below.

        HBox brandRow = new HBox(14); brandRow.setAlignment(Pos.CENTER);
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png")));
            logo.setFitWidth(54); logo.setFitHeight(54); logo.setPreserveRatio(true);
            brandRow.getChildren().add(logo);
        } catch (Exception ignored) {}
        VBox brandText = new VBox(3); brandText.setAlignment(Pos.CENTER_LEFT);
        Label brandName = new Label("INNsight");
        brandName.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
        Label tagline = new Label("Your Digital Concierge");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        brandText.getChildren().addAll(brandName, tagline);
        brandRow.getChildren().add(brandText);

        Separator sep = new Separator();
        VBox welcomeBox = new VBox(5);
        Label wLabel = new Label("Welcome back");
        wLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
        Label wSub = new Label("Sign in to access your hotel dashboard");
        wSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        welcomeBox.getChildren().addAll(wLabel, wSub);

        VBox userBox = new VBox(8);
        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        TextField userField = new TextField();
        userField.setPromptText("Enter your username");
        userField.getStyleClass().add("login-field");
        userBox.getChildren().addAll(userLbl, userField);

        VBox passBox = new VBox(8);
        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.getStyleClass().add("login-field");
        passBox.getChildren().addAll(passLbl, passField);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        errLabel.setVisible(false); errLabel.setManaged(false);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE); loginBtn.setPrefHeight(50);

        Label hint = new Label("Default: admin / admin123");
        hint.setStyle("-fx-text-fill: #253047; -fx-font-size: 12px;");

        loginBtn.setOnAction(e -> {
            String u = userField.getText().trim(), p = passField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) { showLoginError(errLabel, "Please enter both fields"); return; }
            ScaleTransition pulse2 = new ScaleTransition(Duration.millis(100), loginBtn);
            pulse2.setToX(0.97); pulse2.setToY(0.97); pulse2.setAutoReverse(true); pulse2.setCycleCount(2); pulse2.play();
            if (DatabaseManager.getInstance().validateAdmin(u, p)) {
                FadeTransition ft = new FadeTransition(Duration.millis(350), root);
                ft.setFromValue(1); ft.setToValue(0); ft.setOnFinished(ev -> showMainApplication()); ft.play();
            } else { showLoginError(errLabel, "Invalid username or password"); shakeNode(card); }
        });
        passField.setOnAction(e -> loginBtn.fire());

        card.getChildren().addAll(brandRow, sep, welcomeBox, userBox, passBox, errLabel, loginBtn, hint);
        root.getChildren().add(card);

        card.setTranslateY(40); card.setOpacity(0);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("INNsight — AI-Driven Hotel Management");
        primaryStage.setScene(scene); primaryStage.setMaximized(true); primaryStage.show();

        // Logo occupies 40% of screen width, centred — 30% blank on each side
        if (splashLogo != null) {
            splashLogo.fitWidthProperty().bind(scene.widthProperty().multiply(0.40));
        }

        // Splash sequence: fade logo in → hold 3s → fade out → login card slides up
        Runnable showCard = () -> {
            Timeline cardIn = new Timeline(new KeyFrame(Duration.millis(700),
                new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT)));
            cardIn.setDelay(Duration.millis(200));
            cardIn.setOnFinished(e -> borderPulse.play()); // safe to start now — card is fully visible
            cardIn.play();
        };

        if (splashLogo != null) {
            // 1) Fade logo in over 1 second to 28% opacity
            FadeTransition splashIn = new FadeTransition(Duration.millis(1000), splashLogo);
            splashIn.setFromValue(0); splashIn.setToValue(0.28);
            splashIn.setOnFinished(ev1 -> {
                // 2) Hold for 3 seconds
                PauseTransition hold = new PauseTransition(Duration.seconds(3));
                hold.setOnFinished(ev2 -> {
                    // 3) Fade logo out + show login card simultaneously
                    FadeTransition splashOut = new FadeTransition(Duration.millis(700), splashLogo);
                    splashOut.setToValue(0);
                    splashOut.play();
                    showCard.run();
                });
                hold.play();
            });
            splashIn.play();
        } else {
            showCard.run(); // logo load failed — just show card immediately
        }
    }

    private void showLoginError(Label l, String msg) {
        l.setText("  " + msg); l.setVisible(true); l.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), l);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ════════════════════════════════════════════════════════════
    //  MAIN APPLICATION SHELL
    // ════════════════════════════════════════════════════════════
    private void showMainApplication() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("theme-" + THEME_IDS[themeIdx]);
        sidebar = buildSidebar(); mainLayout.setLeft(sidebar);
        contentArea = new StackPane(); contentArea.getStyleClass().add("content-area");
        contentArea.setStyle("-fx-background-color: " + tb() + "; -fx-padding: 30;");
        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        scroll.getStyleClass().add("main-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        mainLayout.setCenter(scroll);
        navigateTo("dashboard");
        Scene scene = new Scene(mainLayout, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("INNsight — Hotel Management");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(false); primaryStage.setMaximized(true);
        FadeTransition fade = new FadeTransition(Duration.millis(400), mainLayout);
        fade.setFromValue(0); fade.setToValue(1); fade.play();
    }

    // ════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ════════════════════════════════════════════════════════════
    private VBox buildSidebar() {
        VBox sb = new VBox();
        sb.setStyle("-fx-background-color: " + ts() + "; -fx-border-color: transparent " + ts2() + " transparent transparent; -fx-border-width: 0 1 0 0;");
        sb.setPadding(new Insets(24, 14, 24, 14)); sb.setSpacing(3);
        sb.setMinWidth(248); sb.setMaxWidth(248);

        HBox brand = new HBox(12); brand.setAlignment(Pos.CENTER_LEFT); brand.setPadding(new Insets(0,0,20,8));
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png")));
            logo.setFitWidth(38); logo.setFitHeight(38); logo.setPreserveRatio(true);
            brand.getChildren().add(logo);
        } catch (Exception ignored) {}
        VBox brandTxt = new VBox(2);
        Label brandN = new Label("INNsight"); brandN.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label brandS = new Label("Property Management"); brandS.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm() + ";");
        brandTxt.getChildren().addAll(brandN, brandS); brand.getChildren().add(brandTxt);

        Button dashBtn = navBtn("  Dashboard",    "dashboard");
        Button roomBtn = navBtn("  Rooms",         "rooms");
        Button resBtn  = navBtn("  Reservations",  "reservations");
        Button custBtn = navBtn("  Customers",     "customers");
        Button chatBtn = navBtn("  AI Chat",       "chat");
        navButtons = new Button[]{dashBtn, roomBtn, resBtn, custBtn, chatBtn};

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Label themeLabel = sectionLabel("THEMES");
        HBox themeDots = new HBox(10); themeDots.setAlignment(Pos.CENTER_LEFT); themeDots.setPadding(new Insets(0,0,16,8));
        for (int i = 0; i < THEMES.length; i++) {
            themeDots.getChildren().add(makeThemeDot(i));
        }

        Button logoutBtn = makeLogoutBtn();
        sb.getChildren().addAll(brand, sectionLabel("MAIN MENU"), dashBtn, roomBtn, resBtn, custBtn, chatBtn, spacer, themeLabel, themeDots, logoutBtn);
        return sb;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: " + ts2() + "; -fx-font-weight: bold; -fx-padding: 8 0 4 8;");
        return l;
    }

    private Button navBtn(String text, String navId) {
        Button btn = new Button(text); btn.setMaxWidth(Double.MAX_VALUE); btn.setAlignment(Pos.CENTER_LEFT);
        styleNav(btn, false);
        btn.setOnMouseEntered(e -> { if (!navId.equals(activeNav)) btn.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: #CBD5E1; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 11 16; -fx-background-radius: 10; -fx-cursor: hand;"); });
        btn.setOnMouseExited(e -> { if (!navId.equals(activeNav)) styleNav(btn, false); });
        btn.setOnAction(e -> navigateTo(navId));
        return btn;
    }

    private void styleNav(Button btn, boolean active) {
        if (active)
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: " + ta() + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 11 16 11 13; -fx-background-radius: 10; -fx-border-color: transparent transparent transparent " + ta() + "; -fx-border-width: 0 0 0 3; -fx-cursor: hand;");
        else
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 11 16; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    private Button makeThemeDot(int idx) {
        boolean active = themeIdx == idx;
        String dotColor = THEMES[idx][3];
        Button dot = new Button();
        dot.setStyle("-fx-background-color: " + dotColor + "; -fx-background-radius: 50%; -fx-min-width: 22; -fx-max-width: 22; -fx-min-height: 22; -fx-max-height: 22; -fx-cursor: hand;" +
            (active ? "-fx-border-color: white; -fx-border-width: 2.5; -fx-border-radius: 50%;" : ""));
        Tooltip.install(dot, new Tooltip(THEME_NAMES[idx]));
        dot.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(120), dot); st.setToX(1.25); st.setToY(1.25); st.play(); });
        dot.setOnMouseExited(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(120), dot); st.setToX(1.0); st.setToY(1.0); st.play(); });
        dot.setOnAction(e -> applyTheme(idx));
        return dot;
    }

    private Button makeLogoutBtn() {
        Button btn = new Button("Logout"); btn.setMaxWidth(Double.MAX_VALUE);
        String base  = "-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 11 16; -fx-background-radius: 10; -fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT; -fx-padding: 11 16; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover)); btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> showLoginScreen());
        return btn;
    }

    private void applyTheme(int idx) {
        themeIdx = idx;
        if (mainLayout == null) return;
        mainLayout.getStyleClass().removeIf(c -> c.startsWith("theme-"));
        mainLayout.getStyleClass().add("theme-" + THEME_IDS[idx]);
        contentArea.setStyle("-fx-background-color: " + tb() + "; -fx-padding: 30;");
        sidebar.getStyleClass().removeIf(c -> c.startsWith("theme-"));
        sidebar.setStyle("-fx-background-color: " + ts() + "; -fx-border-color: transparent " + ts2() + " transparent transparent; -fx-border-width: 0 1 0 0;");
        sidebar.getChildren().clear();
        VBox fresh = buildSidebar();
        sidebar.getChildren().addAll(fresh.getChildren());
        navigateTo(activeNav);
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════
    private void navigateTo(String navId) {
        activeNav = navId;
        String[] ids = {"dashboard", "rooms", "reservations", "customers", "chat"};
        for (int i = 0; i < navButtons.length; i++) styleNav(navButtons[i], ids[i].equals(navId));
        contentArea.getChildren().clear();
        VBox view;
        switch (navId) {
            case "rooms":        view = buildRoomsView();        break;
            case "reservations": view = buildReservationsView(); break;
            case "customers":    view = buildCustomersView();    break;
            case "chat":         view = buildChatView();         break;
            default:             view = buildDashboardView();    break;
        }
        view.setOpacity(0); view.setTranslateX(20);
        contentArea.getChildren().add(view); StackPane.setAlignment(view, Pos.TOP_LEFT);
        Timeline slideIn = new Timeline(new KeyFrame(Duration.millis(300),
            new KeyValue(view.opacityProperty(), 1, Interpolator.EASE_OUT),
            new KeyValue(view.translateXProperty(), 0, Interpolator.EASE_OUT)));
        slideIn.play();
    }

    // ════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ════════════════════════════════════════════════════════════
    private VBox buildDashboardView() {
        VBox view = new VBox(24); view.setPadding(new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(); topBar.setAlignment(Pos.CENTER);
        VBox hdr = new VBox(4);
        Label title = new Label("Dashboard Overview"); title.getStyleClass().add("page-title");
        Label sub = new Label("Welcome back! Here's what's happening at your property today.");
        sub.getStyleClass().add("page-subtitle");
        hdr.getChildren().addAll(title, sub); HBox.setHgrow(hdr, Priority.ALWAYS);
        Button newResBtn = new Button("+ New Reservation"); newResBtn.getStyleClass().add("btn-primary");
        newResBtn.setOnAction(e -> navigateTo("reservations"));
        topBar.getChildren().addAll(hdr, newResBtn);

        // Stat cards with staggered entrance
        HBox statsRow = new HBox(16); statsRow.setAlignment(Pos.CENTER_LEFT);
        try {
            int total  = roomController.getTotalRoomCount();
            int avail  = roomController.getAvailableRoomCount();
            int active = reservationController.getActiveReservationCount();
            int guests = customerController.getCustomerCount();
            double rev = reservationController.getTotalRevenue();
            int occ    = total > 0 ? ((total - avail) * 100 / total) : 0;

            VBox[] cards = {
                statCard("Occupancy",       occ + "%",                    "#F59E0B", "of rooms filled"),
                statCard("Active Bookings", String.valueOf(active),        "#818CF8", "confirmed stays"),
                statCard("Available Rooms", String.valueOf(avail),         "#22C55E", "ready to book"),
                statCard("Total Guests",    String.valueOf(guests),        "#06B6D4", "registered guests"),
                statCard("Revenue",         "$" + String.format("%.0f", rev), "#A78BFA", "all-time earnings")
            };
            for (int i = 0; i < cards.length; i++) {
                HBox.setHgrow(cards[i], Priority.ALWAYS); cards[i].setMaxWidth(Double.MAX_VALUE);
                statsRow.getChildren().add(cards[i]);
                animateIn(cards[i], i * 70);
            }
        } catch (Exception e) { statsRow.getChildren().add(new Label("Error loading stats")); }

        HBox banner = buildAIBanner();

        VBox recentCard = new VBox(16); recentCard.getStyleClass().add("card");
        HBox recentHdr = new HBox(); recentHdr.setAlignment(Pos.CENTER_LEFT);
        HBox titleRow = new HBox(10); titleRow.setAlignment(Pos.CENTER_LEFT);
        try {
            ImageView li = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png")));
            li.setFitWidth(22); li.setFitHeight(22); li.setPreserveRatio(true); titleRow.getChildren().add(li);
        } catch (Exception ignored) {}
        Label recentTitle = new Label("Recent Reservations");
        recentTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        titleRow.getChildren().add(recentTitle);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button viewAll = new Button("View All →");
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ta() + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        viewAll.setOnAction(e -> navigateTo("reservations"));
        recentHdr.getChildren().addAll(titleRow, sp, viewAll);
        try {
            TableView<Reservation> tbl = buildReservationTable();
            List<Reservation> all = reservationController.getAllReservations();
            tbl.setItems(FXCollections.observableArrayList(all.subList(0, Math.min(5, all.size()))));
            tbl.setPrefHeight(270);
            recentCard.getChildren().addAll(recentHdr, tbl);
        } catch (Exception e) { recentCard.getChildren().addAll(recentHdr, new Label("No reservations yet.")); }

        view.getChildren().addAll(topBar, statsRow, banner, recentCard);
        return view;
    }

    private VBox statCard(String label, String value, String accent, String sub) {
        VBox card = new VBox(8);
        String base  = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-padding: 20 22; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 18, 0, 0, 4);";
        String hover = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + accent + "; -fx-border-width: 1.5; -fx-padding: 20 22; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 26, 0, 0, 8);";
        card.setStyle(base); card.setPrefHeight(136);

        HBox top = new HBox(6); top.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●"); dot.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 10px;");
        Label lbl = new Label(label.toUpperCase()); lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm() + "; -fx-font-weight: bold;");
        top.getChildren().addAll(dot, lbl);

        Label val = new Label(value); val.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label subLbl = new Label(sub); subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + accent + ";");

        card.getChildren().addAll(top, val, subLbl);
        card.setOnMouseEntered(e -> { card.setStyle(hover); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.02); st.setToY(1.02); st.play(); });
        card.setOnMouseExited(e -> { card.setStyle(base); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.0); st.setToY(1.0); st.play(); });
        return card;
    }

    private HBox buildAIBanner() {
        HBox banner = new HBox(24);
        banner.setStyle(
            "-fx-background-color: linear-gradient(to right, " + ts() + ", " + ts2() + ");" +
            "-fx-background-radius: 20; -fx-border-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1;" +
            "-fx-padding: 28 32; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 6);"
        );
        banner.setAlignment(Pos.CENTER_LEFT);
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png")));
            logo.setFitWidth(60); logo.setFitHeight(60); logo.setPreserveRatio(true); logo.setOpacity(0.9);
            banner.getChildren().add(logo);
        } catch (Exception ignored) {}
        VBox text = new VBox(8); HBox.setHgrow(text, Priority.ALWAYS);
        Label t = new Label("INNsight AI Concierge — Powered by Botpress");
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label s = new Label("Book rooms, modify reservations, check availability through natural conversation.");
        s.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(248,250,252,0.6); -fx-wrap-text: true;"); s.setWrapText(true);
        Button chatBtn = new Button("Open AI Chat"); chatBtn.getStyleClass().add("btn-primary"); chatBtn.setOnAction(e -> navigateTo("chat"));
        text.getChildren().addAll(t, s, chatBtn); banner.getChildren().add(text);
        FadeTransition ft = new FadeTransition(Duration.millis(800), banner); ft.setFromValue(0); ft.setToValue(1); ft.play();
        return banner;
    }

    // ════════════════════════════════════════════════════════════
    //  ROOMS  (card grid)
    // ════════════════════════════════════════════════════════════
    private VBox buildRoomsView() {
        VBox view = new VBox(20); view.setPadding(new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(); topBar.setAlignment(Pos.CENTER);
        VBox hdr = pageHeader("Room Management", "Manage your property inventory — add, edit, update rooms");
        HBox.setHgrow(hdr, Priority.ALWAYS);
        Button addBtn = new Button("+ Add Room"); addBtn.getStyleClass().add("btn-primary");
        topBar.getChildren().addAll(hdr, addBtn);

        // Filter tabs
        String[] filters = {"All", "Available", "Booked", "Maintenance"};
        Button[] filterBtns = new Button[filters.length];
        HBox filterBar = new HBox(8); filterBar.setAlignment(Pos.CENTER_LEFT);
        String[] activeFilter = {"All"};

        FlowPane cardPane = new FlowPane(14, 14);
        cardPane.setPrefWrapLength(Double.MAX_VALUE);

        List<Room> allRooms = new ArrayList<>();
        try { allRooms.addAll(roomController.getAllRooms()); } catch (Exception ignored) {}

        for (int i = 0; i < filters.length; i++) {
            final String filter = filters[i];
            Button fb = new Button(filter); filterBtns[i] = fb;
            styleFilterTab(fb, filter.equals("All"), false);
            fb.setOnAction(e -> {
                for (Button b : filterBtns) styleFilterTab(b, false, false);
                styleFilterTab(fb, true, false);
                activeFilter[0] = filter;
                populateRoomCards(cardPane, allRooms, filter);
            });
            filterBar.getChildren().add(fb);
        }

        addBtn.setOnAction(e -> showRoomDialog(null, null));
        populateRoomCards(cardPane, allRooms, "All");

        ScrollPane scroll = new ScrollPane(cardPane);
        scroll.setFitToWidth(true); scroll.getStyleClass().add("main-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(topBar, filterBar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return view;
    }

    private void styleFilterTab(Button btn, boolean active, boolean unused) {
        if (active) {
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: " + ta() + "; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + ta() + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 7 16; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: " + tm() + "; -fx-font-size: 13px; -fx-padding: 7 16; -fx-cursor: hand;");
        }
    }

    private void populateRoomCards(FlowPane pane, List<Room> rooms, String filter) {
        pane.getChildren().clear();
        List<Room> shown = new ArrayList<>();
        for (Room r : rooms) {
            if (filter.equals("All") || r.getStatus().equalsIgnoreCase(filter)) shown.add(r);
        }
        if (shown.isEmpty()) {
            Label empty = new Label("No rooms found for \"" + filter + "\"");
            empty.setStyle("-fx-text-fill: " + tm() + "; -fx-font-size: 14px; -fx-padding: 40;");
            pane.getChildren().add(empty); return;
        }
        for (int i = 0; i < shown.size(); i++) {
            VBox card = buildRoomCard(shown.get(i));
            pane.getChildren().add(card);
            animateIn(card, i * 55);
        }
    }

    private VBox buildRoomCard(Room room) {
        VBox card = new VBox(10); card.setPrefWidth(210); card.setPadding(new Insets(20));
        String base  = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);";
        String hover = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ta() + "; -fx-border-width: 1.5; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 30, 0, 0, 10);";
        card.setStyle(base);

        HBox topRow = new HBox(); topRow.setAlignment(Pos.CENTER_LEFT);
        Label roomNum = new Label("#" + room.getRoomNumber());
        roomNum.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Region rSp = new Region(); HBox.setHgrow(rSp, Priority.ALWAYS);
        Label badge = roomBadge(room.getStatus());
        topRow.getChildren().addAll(roomNum, rSp, badge);

        Label typeLabel = new Label(room.getRoomType().toUpperCase());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + ta() + ";");

        Separator sep = new Separator();

        HBox priceRow = new HBox(3); priceRow.setAlignment(Pos.BASELINE_LEFT);
        Label price = new Label("$" + String.format("%.0f", room.getPricePerNight()));
        price.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label perNight = new Label("/night"); perNight.setStyle("-fx-font-size: 12px; -fx-text-fill: " + tm() + ";");
        priceRow.getChildren().addAll(price, perNight);

        Label cap = new Label("Capacity: " + room.getCapacity() + " guest" + (room.getCapacity() > 1 ? "s" : ""));
        cap.setStyle("-fx-font-size: 12px; -fx-text-fill: " + tm() + ";");

        Region vSp = new Region(); VBox.setVgrow(vSp, Priority.ALWAYS);

        String dimEdit = "-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: " + ts2() + "; -fx-border-width: 1;";
        String dimDel  = "-fx-background-color: transparent; -fx-text-fill: rgba(248,113,113,0.45); -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(239,68,68,0.2); -fx-border-width: 1;";
        String litEdit = "-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: " + ta() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: " + ta() + "; -fx-border-width: 1;";
        String litDel  = "-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #F87171; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(239,68,68,0.4); -fx-border-width: 1;";

        Button editBtn = new Button("Edit"); editBtn.setStyle(dimEdit);
        Button delBtn  = new Button("Delete"); delBtn.setStyle(dimDel);
        HBox actions = new HBox(8, editBtn, delBtn);

        card.getChildren().addAll(topRow, typeLabel, sep, priceRow, cap, vSp, actions);

        card.setOnMouseEntered(e -> {
            card.setStyle(hover); editBtn.setStyle(litEdit); delBtn.setStyle(litDel);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(base); editBtn.setStyle(dimEdit); delBtn.setStyle(dimDel);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        editBtn.setOnAction(e -> showRoomDialog(room, null));
        delBtn.setOnAction(e -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete Room #" + room.getRoomNumber() + "?", ButtonType.YES, ButtonType.NO);
            conf.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            conf.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) { String err = roomController.deleteRoom(room.getRoomId()); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else navigateTo("rooms"); } });
        });
        return card;
    }

    private Label roomBadge(String status) {
        Label b = new Label(status);
        if ("Available".equalsIgnoreCase(status)) b.getStyleClass().add("badge-available");
        else if ("Booked".equalsIgnoreCase(status)) b.getStyleClass().add("badge-booked");
        else if ("Maintenance".equalsIgnoreCase(status)) b.getStyleClass().add("badge-maintenance");
        else b.getStyleClass().add("badge-cancelled");
        return b;
    }

    // ════════════════════════════════════════════════════════════
    //  RESERVATIONS  (booking-pass card grid)
    // ════════════════════════════════════════════════════════════
    private VBox buildReservationsView() {
        VBox view = new VBox(20); view.setPadding(new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(); topBar.setAlignment(Pos.CENTER);
        VBox hdr = pageHeader("Reservations", "Manage all property bookings and guest arrivals");
        HBox.setHgrow(hdr, Priority.ALWAYS);
        Button bookBtn    = new Button("+ New Booking"); bookBtn.getStyleClass().add("btn-success");
        Button refreshBtn = new Button("Refresh");       refreshBtn.getStyleClass().add("btn-outline");
        HBox btns = new HBox(10, bookBtn, refreshBtn); topBar.getChildren().addAll(hdr, btns);

        FlowPane cardPane = new FlowPane(16, 16);
        cardPane.setPrefWrapLength(Double.MAX_VALUE);

        List<Reservation> reservations = new ArrayList<>();
        try { reservations.addAll(reservationController.getAllReservations()); } catch (Exception ignored) {}
        populateReservationCards(cardPane, reservations);

        bookBtn.setOnAction(e -> showBookingDialog(null, null));
        refreshBtn.setOnAction(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(500), refreshBtn);
            rt.setByAngle(360); rt.play();
            navigateTo("reservations");
        });

        ScrollPane scroll = new ScrollPane(cardPane);
        scroll.setFitToWidth(true); scroll.getStyleClass().add("main-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(topBar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return view;
    }

    private void populateReservationCards(FlowPane pane, List<Reservation> reservations) {
        pane.getChildren().clear();
        if (reservations.isEmpty()) {
            Label empty = new Label("No reservations found");
            empty.setStyle("-fx-text-fill: " + tm() + "; -fx-font-size: 14px; -fx-padding: 40;");
            pane.getChildren().add(empty); return;
        }
        for (int i = 0; i < reservations.size(); i++) {
            VBox card = buildReservationCard(reservations.get(i));
            pane.getChildren().add(card);
            animateIn(card, i * 55);
        }
    }

    private VBox buildReservationCard(Reservation res) {
        VBox card = new VBox(0); card.setPrefWidth(320);
        String base  = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);";
        String hover = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ta() + "; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 30, 0, 0, 10);";
        card.setStyle(base);

        // Header: avatar + guest name + booking ID + status badge
        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(16, 16, 12, 16));
        Label av = makeAvatar(res.getCustomerName());
        VBox nameBox = new VBox(3); HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label guestName = new Label(res.getCustomerName() != null ? res.getCustomerName() : "Unknown Guest");
        guestName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label bookingId = new Label("Booking #" + res.getReservationId());
        bookingId.setStyle("-fx-font-size: 11px; -fx-text-fill: " + tm() + ";");
        nameBox.getChildren().addAll(guestName, bookingId);
        Label statusBadge = new Label(res.getReservationStatus());
        if ("Confirmed".equalsIgnoreCase(res.getReservationStatus())) statusBadge.getStyleClass().add("badge-available");
        else if ("Cancelled".equalsIgnoreCase(res.getReservationStatus())) statusBadge.getStyleClass().add("badge-cancelled");
        else statusBadge.getStyleClass().add("badge-booked");
        header.getChildren().addAll(av, nameBox, statusBadge);

        Separator sep1 = new Separator();

        // Room info row
        HBox roomRow = new HBox(8); roomRow.setAlignment(Pos.CENTER_LEFT); roomRow.setPadding(new Insets(10, 16, 10, 16));
        Label roomDot = new Label("■"); roomDot.setStyle("-fx-text-fill: " + ta() + "; -fx-font-size: 9px;");
        Label roomInfo = new Label("Room #" + res.getRoomNumber());
        roomInfo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        roomRow.getChildren().addAll(roomDot, roomInfo);

        Separator sep2 = new Separator();

        // Boarding-pass style date section
        HBox datesSection = new HBox(0); datesSection.setPadding(new Insets(14, 16, 14, 16)); datesSection.setAlignment(Pos.CENTER);
        VBox checkInBox = new VBox(4); checkInBox.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(checkInBox, Priority.ALWAYS);
        Label ciLabel = new Label("CHECK IN"); ciLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + tm() + ";");
        Label ciDate = new Label(res.getCheckInDate() != null ? res.getCheckInDate().toString() : "—");
        ciDate.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        checkInBox.getChildren().addAll(ciLabel, ciDate);
        VBox middleBox = new VBox(4); middleBox.setAlignment(Pos.CENTER); middleBox.setPadding(new Insets(0, 10, 0, 10));
        long nightCount = res.calculateTotalNights();
        Label arrowLbl = new Label("→"); arrowLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: " + ta() + ";");
        Label nightsLbl = new Label(nightCount + (nightCount == 1 ? " night" : " nights")); nightsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + tm() + ";");
        middleBox.getChildren().addAll(arrowLbl, nightsLbl);
        VBox checkOutBox = new VBox(4); checkOutBox.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(checkOutBox, Priority.ALWAYS);
        Label coLabel = new Label("CHECK OUT"); coLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + tm() + ";");
        Label coDate = new Label(res.getCheckOutDate() != null ? res.getCheckOutDate().toString() : "—");
        coDate.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        checkOutBox.getChildren().addAll(coLabel, coDate);
        datesSection.getChildren().addAll(checkInBox, middleBox, checkOutBox);

        Separator sep3 = new Separator();

        // Cost + payment badge row
        HBox costRow = new HBox(8); costRow.setAlignment(Pos.CENTER_LEFT); costRow.setPadding(new Insets(12, 16, 12, 16));
        Label costLabel = new Label(String.format("$%.2f", res.calculateTotalCost()));
        costLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Region cSp = new Region(); HBox.setHgrow(cSp, Priority.ALWAYS);
        Label payBadge = new Label(res.getPaymentStatus());
        payBadge.setStyle("Paid".equalsIgnoreCase(res.getPaymentStatus())
            ? "-fx-background-color: rgba(34,197,94,0.1); -fx-text-fill: #4ADE80; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(34,197,94,0.3); -fx-border-width: 1; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;"
            : "-fx-background-color: rgba(100,116,139,0.1); -fx-text-fill: #94A3B8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(100,116,139,0.25); -fx-border-width: 1; -fx-padding: 3 10; -fx-font-size: 11px;");
        costRow.getChildren().addAll(costLabel, cSp, payBadge);

        Separator sep4 = new Separator();

        // Action buttons
        boolean isCancelled = "Cancelled".equalsIgnoreCase(res.getReservationStatus());
        String dimMod = "-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: " + ts2() + "; -fx-border-width: 1;";
        String dimCan = "-fx-background-color: transparent; -fx-text-fill: rgba(248,113,113,0.45); -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(239,68,68,0.2); -fx-border-width: 1;";
        String dimDis = "-fx-background-color: transparent; -fx-text-fill: rgba(100,116,139,0.3); -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(100,116,139,0.1); -fx-border-width: 1;";
        Button modifyBtn = new Button("Modify"); modifyBtn.setStyle(dimMod);
        Button cancelResBtn = new Button("Cancel Booking"); cancelResBtn.setStyle(isCancelled ? dimDis : dimCan); cancelResBtn.setDisable(isCancelled);
        HBox actions = new HBox(8, modifyBtn, cancelResBtn); actions.setPadding(new Insets(0, 16, 16, 16));

        card.getChildren().addAll(header, sep1, roomRow, sep2, datesSection, sep3, costRow, sep4, actions);
        card.setOnMouseEntered(e -> { card.setStyle(hover); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.02); st.setToY(1.02); st.play(); });
        card.setOnMouseExited(e -> { card.setStyle(base); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.0); st.setToY(1.0); st.play(); });
        modifyBtn.setOnAction(e -> showBookingDialog(res, null));
        cancelResBtn.setOnAction(e -> { Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Cancel reservation #" + res.getReservationId() + "?", ButtonType.YES, ButtonType.NO); c.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); c.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) { String err = reservationController.cancelReservation(res.getReservationId()); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else navigateTo("reservations"); } }); });
        return card;
    }

    private TableView<Reservation> buildReservationTable() {
        TableView<Reservation> table = new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reservation, String> custCol = new TableColumn<>("Guest");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        custCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty); setText(null);
                if (empty || name == null) { setGraphic(null); return; }
                HBox row = new HBox(10, makeAvatar(name), styledLabel(name, tt(), 13));
                row.setAlignment(Pos.CENTER_LEFT); setGraphic(row);
            }
        });

        TableColumn<Reservation, Integer> roomCol  = new TableColumn<>("Room #"); roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber")); roomCol.setPrefWidth(80);
        TableColumn<Reservation, LocalDate> inCol  = new TableColumn<>("Check-In");  inCol.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        TableColumn<Reservation, LocalDate> outCol = new TableColumn<>("Check-Out"); outCol.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        TableColumn<Reservation, String> nightsCol = new TableColumn<>("Nights"); nightsCol.setCellValueFactory(r -> new SimpleStringProperty(String.valueOf(r.getValue().calculateTotalNights()))); nightsCol.setPrefWidth(70);
        TableColumn<Reservation, String> costCol   = new TableColumn<>("Total"); costCol.setCellValueFactory(r -> new SimpleStringProperty(String.format("$%.2f", r.getValue().calculateTotalCost())));

        TableColumn<Reservation, String> statusCol = new TableColumn<>("Status"); statusCol.setCellValueFactory(new PropertyValueFactory<>("reservationStatus"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                if ("Confirmed".equalsIgnoreCase(s)) badge.getStyleClass().add("badge-available");
                else if ("Cancelled".equalsIgnoreCase(s)) badge.getStyleClass().add("badge-cancelled");
                else badge.getStyleClass().add("badge-booked");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Reservation, String> payCol = new TableColumn<>("Payment"); payCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        payCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setStyle("Paid".equalsIgnoreCase(s)
                    ? "-fx-background-color: rgba(34,197,94,0.1); -fx-text-fill: #4ADE80; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(34,197,94,0.3); -fx-border-width: 1; -fx-padding: 3 10; -fx-font-size: 11px; -fx-font-weight: bold;"
                    : "-fx-background-color: rgba(100,116,139,0.1); -fx-text-fill: #94A3B8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(100,116,139,0.25); -fx-border-width: 1; -fx-padding: 3 10; -fx-font-size: 11px;");
                setGraphic(badge); setText(null);
            }
        });

        table.getColumns().addAll(custCol, roomCol, inCol, outCol, nightsCol, costCol, statusCol, payCol);
        return table;
    }

    private void refreshReservationTable(TableView<Reservation> t) { try { t.setItems(FXCollections.observableArrayList(reservationController.getAllReservations())); } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); } }

    // ════════════════════════════════════════════════════════════
    //  CUSTOMERS  (profile card grid)
    // ════════════════════════════════════════════════════════════
    private VBox buildCustomersView() {
        VBox view = new VBox(20); view.setPadding(new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(); topBar.setAlignment(Pos.CENTER);
        VBox hdr = pageHeader("Guest Directory", "View and manage all registered hotel guests");
        HBox.setHgrow(hdr, Priority.ALWAYS);
        Button addBtn     = new Button("+ New Guest"); addBtn.getStyleClass().add("btn-primary");
        Button refreshBtn = new Button("Refresh");     refreshBtn.getStyleClass().add("btn-outline");
        HBox btns = new HBox(10, addBtn, refreshBtn); topBar.getChildren().addAll(hdr, btns);

        FlowPane cardPane = new FlowPane(14, 14);
        cardPane.setPrefWrapLength(Double.MAX_VALUE);

        List<Customer> customers = new ArrayList<>();
        try { customers.addAll(customerController.getAllCustomers()); } catch (Exception ignored) {}
        populateCustomerCards(cardPane, customers);

        addBtn.setOnAction(e -> showCustomerDialog(null, null));
        refreshBtn.setOnAction(e -> {
            RotateTransition rt = new RotateTransition(Duration.millis(500), refreshBtn);
            rt.setByAngle(360); rt.play();
            navigateTo("customers");
        });

        ScrollPane scroll = new ScrollPane(cardPane);
        scroll.setFitToWidth(true); scroll.getStyleClass().add("main-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(topBar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return view;
    }

    private void populateCustomerCards(FlowPane pane, List<Customer> customers) {
        pane.getChildren().clear();
        if (customers.isEmpty()) {
            Label empty = new Label("No guests found");
            empty.setStyle("-fx-text-fill: " + tm() + "; -fx-font-size: 14px; -fx-padding: 40;");
            pane.getChildren().add(empty); return;
        }
        for (int i = 0; i < customers.size(); i++) {
            VBox card = buildCustomerCard(customers.get(i));
            pane.getChildren().add(card);
            animateIn(card, i * 55);
        }
    }

    private VBox buildCustomerCard(Customer customer) {
        String[] avatarPalette = {"#F59E0B","#4F46E5","#22C55E","#06B6D4","#F43F5E","#8B5CF6","#F97316","#14B8A6","#EC4899","#3B82F6"};
        String accentClr = avatarPalette[Math.abs(customer.getFullName().hashCode()) % avatarPalette.length];

        VBox card = new VBox(0); card.setPrefWidth(210); card.setAlignment(Pos.CENTER);
        String base  = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);";
        String hover = "-fx-background-color: " + ts() + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: " + accentClr + "; -fx-border-width: 1.5; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 30, 0, 0, 10);";
        card.setStyle(base);

        // Avatar + name centered at top
        VBox avatarSection = new VBox(10); avatarSection.setAlignment(Pos.CENTER); avatarSection.setPadding(new Insets(24, 16, 16, 16));
        String gName = customer.getFullName() != null ? customer.getFullName() : "Unknown";
        String initials = gName.trim().substring(0, 1).toUpperCase();
        if (gName.trim().contains(" ")) { String[] pts = gName.trim().split("\\s+"); initials = pts[0].substring(0,1).toUpperCase() + pts[pts.length-1].substring(0,1).toUpperCase(); }
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: " + accentClr + "; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 50; -fx-min-width: 62; -fx-max-width: 62; -fx-min-height: 62; -fx-max-height: 62; -fx-alignment: CENTER;");
        Label nameLabel = new Label(gName); nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";"); nameLabel.setWrapText(true); nameLabel.setMaxWidth(180); nameLabel.setTextAlignment(TextAlignment.CENTER);
        avatarSection.getChildren().addAll(avatar, nameLabel);

        Separator sep1 = new Separator();

        // Email + phone info
        VBox infoSection = new VBox(9); infoSection.setPadding(new Insets(14, 16, 14, 16));
        HBox emailRow = new HBox(8); emailRow.setAlignment(Pos.CENTER_LEFT);
        Label atIcon = new Label("@"); atIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + accentClr + "; -fx-min-width: 16;");
        Label emailLbl = new Label(customer.getEmail() != null ? customer.getEmail() : "—"); emailLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + tm() + ";"); emailLbl.setMaxWidth(168);
        if (customer.getEmail() != null) Tooltip.install(emailLbl, new Tooltip(customer.getEmail()));
        emailRow.getChildren().addAll(atIcon, emailLbl);
        HBox phoneRow = new HBox(8); phoneRow.setAlignment(Pos.CENTER_LEFT);
        Label hashIcon = new Label("#"); hashIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + accentClr + "; -fx-min-width: 16;");
        Label phoneLbl = new Label(customer.getPhone() != null ? customer.getPhone() : "—"); phoneLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + tm() + ";");
        phoneRow.getChildren().addAll(hashIcon, phoneLbl);
        infoSection.getChildren().addAll(emailRow, phoneRow);

        Separator sep2 = new Separator();

        // Edit / Delete buttons
        String dimEdit = "-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: " + ts2() + "; -fx-border-width: 1;";
        String dimDel  = "-fx-background-color: transparent; -fx-text-fill: rgba(248,113,113,0.45); -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(239,68,68,0.2); -fx-border-width: 1;";
        String litEdit = "-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: " + ta() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: " + ta() + "; -fx-border-width: 1;";
        String litDel  = "-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #F87171; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 12; -fx-background-radius: 7; -fx-border-radius: 7; -fx-border-color: rgba(239,68,68,0.4); -fx-border-width: 1;";
        Button editBtn = new Button("Edit"); editBtn.setStyle(dimEdit);
        Button delBtn  = new Button("Delete"); delBtn.setStyle(dimDel);
        HBox actions = new HBox(8, editBtn, delBtn); actions.setPadding(new Insets(0, 16, 16, 16));

        card.getChildren().addAll(avatarSection, sep1, infoSection, sep2, actions);
        card.setOnMouseEntered(e -> { card.setStyle(hover); editBtn.setStyle(litEdit); delBtn.setStyle(litDel); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.025); st.setToY(1.025); st.play(); });
        card.setOnMouseExited(e -> { card.setStyle(base); editBtn.setStyle(dimEdit); delBtn.setStyle(dimDel); ScaleTransition st = new ScaleTransition(Duration.millis(150), card); st.setToX(1.0); st.setToY(1.0); st.play(); });
        editBtn.setOnAction(e -> showCustomerDialog(customer, null));
        delBtn.setOnAction(e -> { Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + customer.getFullName() + "?", ButtonType.YES, ButtonType.NO); conf.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); conf.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) { String err = customerController.deleteCustomer(customer.getCustomerId()); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else navigateTo("customers"); } }); });
        return card;
    }

    // ════════════════════════════════════════════════════════════
    //  AI CHAT
    // ════════════════════════════════════════════════════════════
    private VBox buildChatView() {
        VBox view = new VBox(16); view.setPadding(new Insets(0, 0, 30, 0));

        HBox topBar = new HBox(); topBar.setAlignment(Pos.CENTER);
        HBox titleRow = new HBox(12); titleRow.setAlignment(Pos.CENTER_LEFT);
        try { ImageView li = new ImageView(new Image(getClass().getResourceAsStream("/logo_main.png"))); li.setFitWidth(36); li.setFitHeight(36); li.setPreserveRatio(true); titleRow.getChildren().add(li); } catch (Exception ignored) {}
        VBox hdrTxt = new VBox(3);
        Label chatTitle = new Label("AI Receptionist"); chatTitle.getStyleClass().add("page-title");
        Label chatSub   = new Label("Powered by Botpress — book, modify, cancel reservations through natural conversation"); chatSub.getStyleClass().add("page-subtitle");
        hdrTxt.getChildren().addAll(chatTitle, chatSub); titleRow.getChildren().add(hdrTxt); HBox.setHgrow(titleRow, Priority.ALWAYS);

        // Pulsing live indicator
        HBox statusPill = new HBox(6); statusPill.setAlignment(Pos.CENTER);
        statusPill.setStyle("-fx-background-color: rgba(34,197,94,0.1); -fx-background-radius: 20; -fx-border-color: rgba(34,197,94,0.25); -fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 6 14;");
        Label sdot = new Label("●"); sdot.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 10px;");
        ScaleTransition livePulse = new ScaleTransition(Duration.millis(900), sdot);
        livePulse.setFromX(0.7); livePulse.setToX(1.3); livePulse.setFromY(0.7); livePulse.setToY(1.3);
        livePulse.setAutoReverse(true); livePulse.setCycleCount(Timeline.INDEFINITE); livePulse.play();
        Label sTxt = new Label("Live"); sTxt.setStyle("-fx-text-fill: #4ADE80; -fx-font-size: 12px; -fx-font-weight: bold;");
        statusPill.getChildren().addAll(sdot, sTxt); topBar.getChildren().addAll(titleRow, statusPill);

        HBox infoBar = new HBox(12); infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setStyle("-fx-background-color: rgba(245,158,11,0.06); -fx-background-radius: 12; -fx-border-color: rgba(245,158,11,0.2); -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 12 18;");
        Label infoIcon = new Label("i"); infoIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FBBF24; -fx-background-color: rgba(245,158,11,0.15); -fx-background-radius: 12; -fx-min-width: 22; -fx-min-height: 22; -fx-max-width: 22; -fx-max-height: 22; -fx-alignment: CENTER;");
        Label infoTxt = new Label("To enable real-time DB updates: run  ngrok.exe http 8080  in CMD, copy the URL, update it in Botpress Execute Code cards, then republish.");
        infoTxt.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400E; -fx-wrap-text: true;"); infoTxt.setWrapText(true); HBox.setHgrow(infoTxt, Priority.ALWAYS);
        infoBar.getChildren().addAll(infoIcon, infoTxt);

        StackPane webContainer = new StackPane(); VBox.setVgrow(webContainer, Priority.ALWAYS);
        WebView webView = new WebView();
        webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
        webView.getEngine().setJavaScriptEnabled(true); VBox.setVgrow(webView, Priority.ALWAYS);

        VBox errOverlay = new VBox(20); errOverlay.setAlignment(Pos.CENTER);
        errOverlay.setStyle("-fx-background-color: " + ts() + "; -fx-background-radius: 18; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-border-radius: 18; -fx-padding: 60;");
        errOverlay.setVisible(false);

        Label errIcon  = new Label("⚠"); errIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: " + ta() + ";");
        Label errTitle = new Label("Chat failed to load"); errTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + tt() + ";");
        Label errSub   = new Label("This usually fixes itself on retry. Click below to reload.");
        errSub.setStyle("-fx-font-size: 14px; -fx-text-fill: " + tm() + ";"); errSub.setTextAlignment(TextAlignment.CENTER);
        Button retryBtn = new Button("Retry"); retryBtn.getStyleClass().add("btn-primary"); retryBtn.setStyle(retryBtn.getStyle() + "-fx-padding: 12 30; -fx-font-size: 14px;");
        Label orLbl = new Label("— or —"); orLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ts2() + ";");
        Button browserBtn = new Button("Open in browser as last resort");
        browserBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + tm() + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: true; -fx-border-color: transparent;");
        browserBtn.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(BOTPRESS_URL)); } catch (Exception ex) { showAlert(Alert.AlertType.ERROR, "Error", "Could not open browser."); } });
        errOverlay.getChildren().addAll(errIcon, errTitle, errSub, retryBtn, orLbl, browserBtn);
        webContainer.getChildren().addAll(webView, errOverlay);

        int[] retries = {0}; Runnable[] loadRef = {null};
        Runnable load = () -> { errOverlay.setVisible(false); webView.setVisible(true); webView.getEngine().load(BOTPRESS_URL + "&t=" + System.currentTimeMillis()); };
        loadRef[0] = load;

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, nv) -> {
            if (nv == javafx.concurrent.Worker.State.SUCCEEDED) {
                PauseTransition wait = new PauseTransition(Duration.millis(2500));
                wait.setOnFinished(ev -> { try { String body = (String) webView.getEngine().executeScript("document.body ? document.body.innerText : ''"); if (body != null && body.contains("Error loading shareable webchat")) { retries[0]++; if (retries[0] <= 3) loadRef[0].run(); else { retries[0] = 0; webView.setVisible(false); errOverlay.setVisible(true); } } else retries[0] = 0; } catch (Exception ignored) {} });
                wait.play();
            } else if (nv == javafx.concurrent.Worker.State.FAILED) {
                Platform.runLater(() -> { retries[0]++; if (retries[0] <= 3) loadRef[0].run(); else { retries[0] = 0; webView.setVisible(false); errOverlay.setVisible(true); } });
            }
        });

        Timeline checker = new Timeline(new KeyFrame(Duration.seconds(15), ev -> { if (!webView.isVisible()) return; try { String t = (String) webView.getEngine().executeScript("document.body ? document.body.innerText : ''"); if (t != null && t.contains("Error loading shareable webchat")) loadRef[0].run(); } catch (Exception ignored) {} }));
        checker.setCycleCount(Timeline.INDEFINITE); checker.play();
        retryBtn.setOnAction(e -> { retries[0] = 0; loadRef[0].run(); });
        load.run();

        VBox outerCard = new VBox(webContainer);
        outerCard.setStyle("-fx-background-color: " + ts() + "; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: " + ts2() + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 6);");
        VBox.setVgrow(outerCard, Priority.ALWAYS); VBox.setVgrow(webContainer, Priority.ALWAYS);
        view.getChildren().addAll(topBar, infoBar, outerCard);
        VBox.setVgrow(outerCard, Priority.ALWAYS); VBox.setVgrow(view, Priority.ALWAYS);
        return view;
    }

    // ════════════════════════════════════════════════════════════
    //  DIALOGS
    // ════════════════════════════════════════════════════════════
    private void showRoomDialog(Room existing, TableView<Room> table) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Room" : "Edit Room");
        dialog.setHeaderText(existing == null ? "Enter room details" : "Update room details");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        TextField numField   = new TextField(); numField.setPromptText("e.g. 101");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Single","Double","Suite","Deluxe","Penthouse")); typeBox.setPromptText("Select type");
        TextField capField   = new TextField(); capField.setPromptText("e.g. 2");
        TextField priceField = new TextField(); priceField.setPromptText("e.g. 120.00");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Available","Booked","Maintenance")); statusBox.setValue("Available");
        if (existing != null) { numField.setText(String.valueOf(existing.getRoomNumber())); typeBox.setValue(existing.getRoomType()); capField.setText(String.valueOf(existing.getCapacity())); priceField.setText(String.valueOf(existing.getPricePerNight())); statusBox.setValue(existing.getStatus()); }
        grid.add(new Label("Room Number:"), 0, 0); grid.add(numField, 1, 0);
        grid.add(new Label("Room Type:"), 0, 1);   grid.add(typeBox, 1, 1);
        grid.add(new Label("Capacity:"), 0, 2);    grid.add(capField, 1, 2);
        grid.add(new Label("Price/Night ($):"), 0, 3); grid.add(priceField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);      grid.add(statusBox, 1, 4);
        dialog.getDialogPane().setContent(grid); Platform.runLater(numField::requestFocus);
        dialog.setResultConverter(btn -> { if (btn == saveType) { try { Room r = existing != null ? existing : new Room(); r.setRoomNumber(Integer.parseInt(numField.getText().trim())); r.setRoomType(typeBox.getValue()); r.setCapacity(Integer.parseInt(capField.getText().trim())); r.setPricePerNight(Double.parseDouble(priceField.getText().trim())); r.setStatus(statusBox.getValue()); return r; } catch (NumberFormatException ex) { showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers."); return null; } } return null; });
        dialog.showAndWait().ifPresent(room -> { String err = existing != null ? roomController.updateRoom(room) : roomController.addRoom(room); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else { if (table != null) refreshRoomTable(table); else navigateTo("rooms"); } });
    }

    private void refreshRoomTable(TableView<Room> t) { try { t.setItems(FXCollections.observableArrayList(roomController.getAllRooms())); } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); } }

    private void showBookingDialog(Reservation existing, TableView<Reservation> table) {
        Dialog<Reservation> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "New Booking" : "Modify Reservation");
        ButtonType saveType = new ButtonType(existing == null ? "Book" : "Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        ComboBox<Customer> custBox = new ComboBox<>(); try { custBox.setItems(FXCollections.observableArrayList(customerController.getAllCustomers())); } catch (Exception ignored) {} custBox.setPromptText("Select Customer");
        TextField nameField = new TextField(); nameField.setPromptText("Guest Name");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        ComboBox<Room> roomBox = new ComboBox<>(); try { roomBox.setItems(FXCollections.observableArrayList(existing != null ? roomController.getAllRooms() : roomController.getAvailableRooms())); } catch (Exception ignored) {} roomBox.setPromptText("Select Room");
        DatePicker inPicker = new DatePicker(LocalDate.now()); DatePicker outPicker = new DatePicker(LocalDate.now().plusDays(1));
        ComboBox<String> payBox = new ComboBox<>(FXCollections.observableArrayList("Unpaid","Paid")); payBox.setValue("Unpaid");
        if (existing != null) { try { Customer c = customerController.getCustomerById(existing.getCustomerId()); if (c != null) custBox.setValue(c); Room r = roomController.getRoomById(existing.getRoomId()); if (r != null) roomBox.setValue(r); } catch (Exception ignored) {} inPicker.setValue(existing.getCheckInDate()); outPicker.setValue(existing.getCheckOutDate()); payBox.setValue(existing.getPaymentStatus()); }
        ToggleGroup tg = new ToggleGroup(); RadioButton existRb = new RadioButton("Existing Customer"); existRb.setToggleGroup(tg); existRb.setSelected(true); RadioButton newRb = new RadioButton("New Customer"); newRb.setToggleGroup(tg);
        VBox newFields = new VBox(8, nameField, emailField, phoneField); newFields.setVisible(false); newFields.setManaged(false);
        tg.selectedToggleProperty().addListener((obs, o, n) -> { boolean isNew = n == newRb; newFields.setVisible(isNew); newFields.setManaged(isNew); custBox.setVisible(!isNew); custBox.setManaged(!isNew); });
        grid.add(new Label("Customer:"), 0, 0); grid.add(new HBox(10, existRb, newRb), 1, 0); grid.add(custBox, 1, 1); grid.add(newFields, 1, 1); grid.add(new Label("Room:"), 0, 2); grid.add(roomBox, 1, 2); grid.add(new Label("Check-In:"), 0, 3); grid.add(inPicker, 1, 3); grid.add(new Label("Check-Out:"), 0, 4); grid.add(outPicker, 1, 4); grid.add(new Label("Payment:"), 0, 5); grid.add(payBox, 1, 5);
        dialog.getDialogPane().setContent(grid); dialog.getDialogPane().setPrefWidth(450);
        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                Room sr = roomBox.getValue(); LocalDate ci = inPicker.getValue(); LocalDate co = outPicker.getValue();
                if (sr == null) { showAlert(Alert.AlertType.ERROR, "Error", "Please select a room"); return null; }
                if (ci == null || co == null || !co.isAfter(ci)) { showAlert(Alert.AlertType.ERROR, "Error", "Check-out must be after check-in"); return null; }
                Reservation res = existing != null ? existing : new Reservation(); res.setRoomId(sr.getRoomId()); res.setCheckInDate(ci); res.setCheckOutDate(co); res.setPaymentStatus(payBox.getValue());
                if (existing == null) {
                    if (newRb.isSelected()) { String nm = nameField.getText().trim(); String em = emailField.getText().trim(); String ph = phoneField.getText().trim(); if (nm.isEmpty() || em.isEmpty() || ph.isEmpty()) { showAlert(Alert.AlertType.ERROR, "Error", "Please fill all customer fields"); return null; } String err = reservationController.bookRoomWithCustomer(nm, em, ph, sr.getRoomId(), ci, co, payBox.getValue()); if (err != null) showAlert(Alert.AlertType.ERROR, "Booking Failed", err); else { showAlert(Alert.AlertType.INFORMATION, "Success", "Room booked!"); navigateTo("reservations"); } return null; }
                    else { Customer sc = custBox.getValue(); if (sc == null) { showAlert(Alert.AlertType.ERROR, "Error", "Please select a customer"); return null; } res.setCustomerId(sc.getCustomerId()); res.setReservationStatus("Confirmed"); }
                }
                return res;
            }
            return null;
        });
        dialog.showAndWait().ifPresent(res -> { String err = existing != null ? reservationController.modifyReservation(res) : reservationController.bookRoom(res); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else { showAlert(Alert.AlertType.INFORMATION, "Success", existing != null ? "Reservation updated!" : "Room booked!"); navigateTo("reservations"); } });
    }

    private void showCustomerDialog(Customer existing, TableView<Customer> table) {
        Dialog<Customer> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Add Customer" : "Edit Customer");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));
        TextField nameField  = new TextField(existing != null ? existing.getFullName() : ""); nameField.setPromptText("Full Name");
        TextField emailField = new TextField(existing != null ? existing.getEmail() : ""); emailField.setPromptText("Email");
        TextField phoneField = new TextField(existing != null ? existing.getPhone() : ""); phoneField.setPromptText("Phone");
        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);     grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);     grid.add(phoneField, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> { if (btn == saveType) { Customer c = existing != null ? existing : new Customer(); c.setFullName(nameField.getText().trim()); c.setEmail(emailField.getText().trim()); c.setPhone(phoneField.getText().trim()); return c; } return null; });
        dialog.showAndWait().ifPresent(c -> { String err = existing != null ? customerController.updateCustomer(c) : customerController.addCustomer(c); if (err != null) showAlert(Alert.AlertType.ERROR, "Error", err); else navigateTo("customers"); });
    }

    // ════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════
    private Label makeAvatar(String name) {
        String[] avatarColors = {"#F59E0B","#4F46E5","#22C55E","#06B6D4","#F43F5E","#8B5CF6","#F97316","#14B8A6","#EC4899","#3B82F6"};
        String initials = name == null || name.isEmpty() ? "?" : name.trim().substring(0, 1).toUpperCase();
        if (name != null && name.trim().contains(" ")) {
            String[] parts = name.trim().split("\\s+");
            initials = parts[0].substring(0, 1).toUpperCase() + parts[parts.length - 1].substring(0, 1).toUpperCase();
        }
        String color = avatarColors[Math.abs((name == null ? 0 : name.hashCode())) % avatarColors.length];
        Label av = new Label(initials);
        av.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 50; -fx-min-width: 32; -fx-max-width: 32; -fx-min-height: 32; -fx-max-height: 32; -fx-alignment: CENTER;");
        return av;
    }

    private Label styledLabel(String text, String color, int size) {
        Label l = new Label(text); l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;"); return l;
    }

    private void animateIn(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0); node.setScaleX(0.90); node.setScaleY(0.90);
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(220), node); ft.setToValue(1);
            ScaleTransition st = new ScaleTransition(Duration.millis(220), node); st.setToX(1.0); st.setToY(1.0);
            new ParallelTransition(ft, st).play();
        });
        pause.play();
    }

    private VBox pageHeader(String title, String sub) {
        Label t = new Label(title); t.getStyleClass().add("page-title");
        Label s = new Label(sub);   s.getStyleClass().add("page-subtitle");
        return new VBox(4, t, s);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alert.showAndWait();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setByX(12); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0)); tt.play();
    }

    @Override
    public void stop() { if (apiServer != null) apiServer.stop(); DatabaseManager.getInstance().closeConnection(); }
    public static void main(String[] args) { launch(args); }
}
