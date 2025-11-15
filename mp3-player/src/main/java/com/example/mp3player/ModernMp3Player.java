package com.example.mp3player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Objects;

public class ModernMp3Player extends Application {
    private final SimpleBooleanProperty mediaLoaded = new SimpleBooleanProperty(false);
    private MediaPlayer mediaPlayer;
    private Timeline progressUpdater;
    private Slider progressSlider;
    private Label elapsedLabel;
    private Label durationLabel;
    private Label trackTitleLabel;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Aurora MP3 Player");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("background");

        StackPane card = createGlassCard();
        VBox content = new VBox(24);
        content.setAlignment(Pos.CENTER);

        trackTitleLabel = new Label("MP3 파일을 열어주세요");
        trackTitleLabel.getStyleClass().add("track-title");

        statusLabel = new Label("READY");
        statusLabel.getStyleClass().add("status-label");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setDisable(true);
        progressSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (progressSlider.isValueChanging() && mediaPlayer != null) {
                Duration total = mediaPlayer.getTotalDuration();
                if (!total.isUnknown()) {
                    mediaPlayer.seek(total.multiply(newValue.doubleValue() / 100.0));
                }
            }
        });
        progressSlider.setOnMouseReleased(event -> seekToSlider());
        progressSlider.setOnTouchReleased(event -> seekToSlider());
        progressSlider.getStyleClass().add("progress-slider");

        elapsedLabel = new Label("00:00");
        elapsedLabel.getStyleClass().add("time-label");

        durationLabel = new Label("--:--");
        durationLabel.getStyleClass().add("time-label");

        HBox timeBox = new HBox(12, elapsedLabel, progressSlider, durationLabel);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPrefWidth(480);

        HBox controls = createControls(primaryStage);

        content.getChildren().addAll(trackTitleLabel, statusLabel, timeBox, controls);
        card.getChildren().add(content);

        root.setCenter(card);

        Scene scene = new Scene(root, 720, 480);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createControls(Stage stage) {
        Button openButton = createPillButton("열기", "accent");
        Button playPauseButton = createPillButton("재생", "primary");
        Button stopButton = createPillButton("정지", "secondary");

        openButton.setOnAction(event -> openMedia(stage));
        playPauseButton.setOnAction(event -> togglePlayPause(playPauseButton));
        stopButton.setOnAction(event -> stopPlayback(playPauseButton));

        playPauseButton.disableProperty().bind(mediaLoaded.not());
        stopButton.disableProperty().bind(mediaLoaded.not());

        HBox controls = new HBox(16, openButton, playPauseButton, stopButton);
        controls.setAlignment(Pos.CENTER);
        return controls;
    }

    private Button createPillButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("pill-button", styleClass);
        button.setPrefWidth(120);
        button.setPrefHeight(48);
        button.setFocusTraversable(false);
        return button;
    }

    private StackPane createGlassCard() {
        StackPane container = new StackPane();
        container.setPadding(new Insets(40));

        Rectangle glass = new Rectangle();
        glass.setArcWidth(40);
        glass.setArcHeight(40);
        glass.widthProperty().bind(container.widthProperty().subtract(80));
        glass.heightProperty().bind(container.heightProperty().subtract(80));
        glass.setFill(Color.web("#ffffff22"));
        glass.setStroke(Color.web("#ffffff55"));
        glass.setStrokeWidth(1.5);
        glass.setEffect(new DropShadow(30, Color.web("#00000040")));

        container.getChildren().add(glass);
        return container;
    }

    private void openMedia(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("MP3 파일 선택");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            prepareMedia(file);
        }
    }

    private void prepareMedia(File file) {
        if (mediaPlayer != null) {
            cleanupMediaPlayer();
        }

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
        } catch (MediaException ex) {
            statusLabel.setText("지원하지 않는 오디오 형식입니다");
            mediaLoaded.set(false);
            return;
        }

        trackTitleLabel.setText(file.getName());
        statusLabel.setText("READY");
        mediaLoaded.set(true);
        progressSlider.setDisable(false);
        progressSlider.setValue(0);
        durationLabel.textProperty().unbind();
        durationLabel.setText("--:--");
        elapsedLabel.setText("00:00");

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            durationLabel.setText(formatDuration(total));
            bindSliderToPlayback();
        });

        mediaPlayer.setOnPlaying(() -> statusLabel.setText("PLAYING"));
        mediaPlayer.setOnPaused(() -> statusLabel.setText("PAUSED"));
        mediaPlayer.setOnStopped(() -> statusLabel.setText("STOPPED"));
        mediaPlayer.setOnEndOfMedia(() -> {
            statusLabel.setText("FINISHED");
            if (progressUpdater != null) {
                progressUpdater.stop();
            }
            progressSlider.setValue(100);
        });
    }

    private void bindSliderToPlayback() {
        if (progressUpdater != null) {
            progressUpdater.stop();
        }

        progressUpdater = new Timeline(new KeyFrame(Duration.millis(250), event -> {
            if (mediaPlayer == null) {
                return;
            }
            Duration currentTime = mediaPlayer.getCurrentTime();
            Duration total = mediaPlayer.getTotalDuration();
            if (total == null || total.isUnknown()) {
                return;
            }
            double progress = currentTime.toMillis() / total.toMillis() * 100.0;
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(progress);
            }
            elapsedLabel.setText(formatDuration(currentTime));
        }));
        progressUpdater.setCycleCount(Timeline.INDEFINITE);
        progressUpdater.play();

        durationLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            return total == null || total.isUnknown() ? "--:--" : formatDuration(total);
        }, mediaPlayer.totalDurationProperty()));
    }

    private void togglePlayPause(Button playPauseButton) {
        if (mediaPlayer == null) {
            return;
        }

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseButton.setText("재생");
        } else {
            mediaPlayer.play();
            playPauseButton.setText("일시정지");
        }
    }

    private void stopPlayback(Button playPauseButton) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playPauseButton.setText("재생");
            progressSlider.setValue(0);
            elapsedLabel.setText("00:00");
        }
    }

    private void seekToSlider() {
        if (mediaPlayer != null && !progressSlider.isDisabled()) {
            Duration total = mediaPlayer.getTotalDuration();
            if (!total.isUnknown()) {
                mediaPlayer.seek(total.multiply(progressSlider.getValue() / 100.0));
            }
        }
    }

    private void cleanupMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.dispose();
        if (progressUpdater != null) {
            progressUpdater.stop();
        }
        progressUpdater = null;
        durationLabel.textProperty().unbind();
        mediaLoaded.set(false);
        progressSlider.setDisable(true);
        progressSlider.setValue(0);
        elapsedLabel.setText("00:00");
        statusLabel.setText("STOPPED");
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "--:--";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            cleanupMediaPlayer();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
