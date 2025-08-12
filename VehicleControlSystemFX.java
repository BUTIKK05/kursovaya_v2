package com.example.kursov;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ThreadLocalRandom;

public class VehicleControlSystemFX extends Application {
    private final Gauge clutchTempGauge = new Gauge("Clutch Temp", Color.PINK);
    private final Gauge gearboxTempGauge = new Gauge("Gearbox Temp", Color.BLUE);
    private final Gauge brakeTempGauge = new Gauge("Brake Temp", Color.PURPLE);
    private final Text warningText = new Text();
    private Timeline timeline;

    private Connection connection;
    private static final String DB_PATH = "C:/Program Files/DB Browser for SQLite/kursovaya.db";

    // Гейджи, чтобы были доступны в методе обновления
    private Gauge throttleGauge;
    private Gauge brakeGauge;
    private Gauge frontLeftGripGauge;
    private Gauge frontRightGripGauge;
    private Gauge rearLeftGripGauge;
    private Gauge rearRightGripGauge;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Control System");

        connectToDatabase();

        VBox mainPane = new VBox(10);
        mainPane.setAlignment(Pos.CENTER);

        Text mainTitle = new Text("ОСНОВНЫЕ ПОКАЗАТЕЛИ");
        mainTitle.setFont(Font.font(20));
        mainTitle.setFill(Color.WHITE);

        HBox sensorPane = new HBox(20);
        sensorPane.setAlignment(Pos.CENTER);
        sensorPane.setPrefHeight(300);

        throttleGauge = new Gauge("Throttle", Color.LIME);
        brakeGauge = new Gauge("Brake", Color.ORANGE);

        sensorPane.getChildren().addAll(throttleGauge, brakeGauge, clutchTempGauge, gearboxTempGauge, brakeTempGauge);
        mainPane.getChildren().addAll(mainTitle, sensorPane);

        VBox gripPane = new VBox(10);
        gripPane.setAlignment(Pos.CENTER);
        Text gripTitle = new Text("СЦЕПЛЕНИЕ КОЛЕС");
        gripTitle.setFont(Font.font(20));
        gripTitle.setFill(Color.WHITE);

        HBox frontGripPane = new HBox(20);
        frontGripPane.setAlignment(Pos.CENTER);
        frontLeftGripGauge = new Gauge("Front Left Grip", Color.GREEN, 50);
        frontRightGripGauge = new Gauge("Front Right Grip", Color.GREEN, 50);

        HBox rearGripPane = new HBox(20);
        rearGripPane.setAlignment(Pos.CENTER);
        rearLeftGripGauge = new Gauge("Rear Left Grip", Color.GREEN, 50);
        rearRightGripGauge = new Gauge("Rear Right Grip", Color.GREEN, 50);

        gripPane.getChildren().addAll(gripTitle, frontGripPane, rearGripPane);
        frontGripPane.getChildren().addAll(frontLeftGripGauge, frontRightGripGauge);
        rearGripPane.getChildren().addAll(rearLeftGripGauge, rearRightGripGauge);

        VBox forcePane = new VBox(10);
        forcePane.setAlignment(Pos.CENTER);
        Text forceTitle = new Text("РАСПРЕДЕЛЕНИЕ МОЩНОСТИ ПО ОСЯМ");
        forceTitle.setFont(Font.font(20));
        forceTitle.setFill(Color.WHITE);

        HBox forceSensorPane = new HBox(20);
        forceSensorPane.setAlignment(Pos.CENTER);
        Gauge frontAxleGauge = new Gauge("Front Axle", Color.CYAN, 50);
        Gauge rearAxleGauge = new Gauge("Rear Axle", Color.CYAN, 50);
        frontAxleGauge.setValue(50);
        rearAxleGauge.setValue(50);


        forceSensorPane.getChildren().addAll(frontAxleGauge, rearAxleGauge);
        forcePane.getChildren().addAll(forceTitle, forceSensorPane);

        HBox buttonPane = new HBox(20);
        buttonPane.setAlignment(Pos.CENTER);
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");

        ToggleButton driveModeButton = new ToggleButton("Switch to 2WD");
        driveModeButton.setStyle("-fx-background-color: #404040; -fx-text-fill: white;");

        buttonPane.getChildren().addAll(startButton, stopButton, driveModeButton);

        warningText.setFill(Color.RED);
        warningText.setFont(Font.font(18));
        warningText.setVisible(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2E2E2E; -fx-padding: 20;");
        root.getChildren().addAll(mainPane, gripPane, forcePane, buttonPane, warningText);

        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            simulateTemperatures();
            updateGaugesFromDB();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        startButton.setOnAction(e -> timeline.play());
        stopButton.setOnAction(e -> timeline.stop());

        Timeline dbCheckTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> checkAndFixGripInDB()));
        dbCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        dbCheckTimeline.play();
    }

    private void connectToDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                System.err.println("❌ База данных не найдена по пути: " + DB_PATH);
                return;
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            System.out.println("✅ Подключено к базе данных: " + DB_PATH);
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Драйвер SQLite не найден! Добавьте sqlite-jdbc в classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Ошибка подключения к базе данных: " + e.getMessage());
        }
    }

    private void simulateTemperatures() {
        float clutchTemp = ThreadLocalRandom.current().nextFloat(190, 210);
        float gearboxTemp = ThreadLocalRandom.current().nextFloat(95, 127);
        float brakeTemp = ThreadLocalRandom.current().nextFloat(460, 515);

        clutchTempGauge.setValue(clutchTemp);
        gearboxTempGauge.setValue(gearboxTemp);
        brakeTempGauge.setValue(brakeTemp);

        boolean isOverheated = clutchTemp > 200 || gearboxTemp > 120 || brakeTemp > 500;
        warningText.setVisible(isOverheated);
        warningText.setText(isOverheated ? "ЗАГЛУШИТЕ ДВИГАТЕЛЬ!" : "");
    }

    private void updateGaugesFromDB() {
        if (connection == null) {
            System.err.println("Нет подключения к базе данных");
            return;
        }

        String sql = "SELECT * FROM vehicles ORDER BY id DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int id = rs.getInt("id");
                float throttle = rs.getFloat("throttle_position");
                float brake = rs.getFloat("brake_position");
                float frontLeftGrip = rs.getFloat("front_left_grip");
                float frontRightGrip = rs.getFloat("front_right_grip");
                float rearLeftGrip = rs.getFloat("rear_left_grip");
                float rearRightGrip = rs.getFloat("rear_right_grip");

                // Если throttle < 60 — обновляем на рандом от 9 до 13 и записываем в БД
                if (throttle < 60) {
                    float newThrottle = ThreadLocalRandom.current().nextFloat(9, 13);
                    String updateThrottleSql = "UPDATE vehicles SET throttle_position = ? WHERE id = ?";
                    try (PreparedStatement pstmt = connection.prepareStatement(updateThrottleSql)) {
                        pstmt.setFloat(1, newThrottle);
                        pstmt.setInt(2, id);
                        int rows = pstmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println("Throttle < 60, обновлено throttle_position на " + newThrottle);
                            throttle = newThrottle; // Обновляем локально для отображения
                        }
                    }
                }

                throttleGauge.setValue(throttle);
                brakeGauge.setValue(brake);
                frontLeftGripGauge.setValue(frontLeftGrip);
                frontRightGripGauge.setValue(frontRightGrip);
                rearLeftGripGauge.setValue(rearLeftGrip);
                rearRightGripGauge.setValue(rearRightGrip);
            } else {
                System.out.println("В таблице vehicles нет данных");
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении датчиков из БД: " + e.getMessage());
        }
    }

    private void checkAndFixGripInDB() {
        if (connection == null) {
            System.err.println("Нет подключения к базе данных.");
            return;
        }

        String selectSql = "SELECT * FROM vehicles ORDER BY id DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            if (rs.next()) {
                int id = rs.getInt("id");
                float throttle = rs.getFloat("throttle_position");
                float frontLeftGrip = rs.getFloat("front_left_grip");
                float frontRightGrip = rs.getFloat("front_right_grip");
                float rearLeftGrip = rs.getFloat("rear_left_grip");
                float rearRightGrip = rs.getFloat("rear_right_grip");

                boolean frontGripBad = (frontLeftGrip != 100) || (frontRightGrip != 100);
                boolean rearGripBad = (rearLeftGrip != 100) || (rearRightGrip != 100);

                if (frontGripBad || rearGripBad) {
                    float newGripValue = 100;

                    float newThrottle;
                    if (rearGripBad) {
                        newThrottle = Math.max(0, throttle - 9);
                    } else if (frontGripBad) {
                        newThrottle = 0;
                    } else {
                        newThrottle = throttle;
                    }

                    String updateSql = "UPDATE vehicles SET " +
                            "throttle_position = ?, " +
                            "front_left_grip = ?, front_right_grip = ?, " +
                            "rear_left_grip = ?, rear_right_grip = ? " +
                            "WHERE id = ?";

                    try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                        pstmt.setFloat(1, newThrottle);
                        pstmt.setFloat(2, newGripValue);
                        pstmt.setFloat(3, newGripValue);
                        pstmt.setFloat(4, newGripValue);
                        pstmt.setFloat(5, newGripValue);
                        pstmt.setInt(6, id);

                        int rowsUpdated = pstmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            System.out.println("Исправлены grip и throttle для записи id=" + id);
                        }
                    }
                } else {
                    System.out.println("Все grip равны 100, изменений нет.");
                }
            } else {
                System.out.println("В таблице vehicles нет данных.");
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при проверке и исправлении grip: " + e.getMessage());
        }
    }

    static class Gauge extends StackPane {
        private final Circle backgroundCircle;
        private final Circle valueCircle;
        private final Text label;
        private final Text valueText;
        private final double radius;

        public Gauge(String labelText, Color color, double radius) {
            this.radius = radius;
            setAlignment(Pos.CENTER);

            backgroundCircle = new Circle(radius);
            backgroundCircle.setFill(Color.DARKGRAY);
            backgroundCircle.setStroke(Color.BLACK);
            backgroundCircle.setStrokeWidth(3);

            valueCircle = new Circle(radius);
            valueCircle.setFill(Color.TRANSPARENT);
            valueCircle.setStroke(color);
            valueCircle.setStrokeWidth(10);
            valueCircle.setStrokeDashOffset(2 * Math.PI * radius);

            label = new Text(labelText);
            label.setFill(Color.WHITE);
            label.setFont(Font.font(radius / 5));

            valueText = new Text("0");
            valueText.setFill(Color.WHITE);
            valueText.setFont(Font.font(radius / 4));

            VBox textPane = new VBox(label, valueText);
            textPane.setAlignment(Pos.CENTER);

            getChildren().addAll(backgroundCircle, valueCircle, textPane);
        }

        public Gauge(String labelText, Color color) {
            this(labelText, color, 100);
        }

        public void setValue(float value) {
            double percentage = Math.min(1.0, Math.max(0.0, value / 100.0));
            valueCircle.setStrokeDashOffset(2 * Math.PI * radius * (1 - percentage));
            valueText.setText(String.format("%.1f", value));
        }

        public void setColor(Color color) {
            valueCircle.setStroke(color);
        }
    }

    @Override
    public void stop() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Соединение с БД закрыто.");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
