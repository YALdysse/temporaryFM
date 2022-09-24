package org.yaldysse.fm.dialogs;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.yaldysse.fm.javaFXgui;
import org.yaldysse.tools.ChecksumAlgorithms;
import org.yaldysse.tools.ChecksumCalculator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

public class ChecksumCheckerDialog
{
    private Stage stage;
    private Scene scene;
    private VBox root;
    private HBox checksum_HBox;
    private Label checksum_Label;
    private TextField checksum_TextField;
    private Properties language;
    private Path targetFilePath;
    private VBox md5FileChecksum;
    private VBox sha1FileChecksum;
    private HBox md5_HBox;
    private HBox sha1_HBox;
    private TextField md5FileChecksum_TextField;
    private TextField sha1FileChecksum_TextField;
    private Label md5Equals_Label;
    private Label sha1Equals_Label;
    private Label checksumAlgorithm_Label;
    private Label fileName_Label;
    private VBox fileChecksums_VBox;
    private Thread calculatingChecksum_Thread;
    private ProgressIndicator md5_ProgressIndicator;
    private ProgressIndicator sha1_ProgressIndicator;
    private BorderPane borderPane;

    public ChecksumCheckerDialog(final Path targetFile, final Properties languageProperties)
    {
        targetFilePath = targetFile;
        language = languageProperties;

        initializeComponents();
    }

    private void initializeComponents()
    {
        root = new VBox(javaFXgui.rem * 0.4D);
        root.setPadding(new Insets(javaFXgui.rem * 0.6D));

        scene = new Scene(root);

        fileName_Label = new Label(targetFilePath.getFileName().toString());
        fileName_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL,
                12.0D));

        HBox fileName_HBox = new HBox(fileName_Label);
        fileName_HBox.setAlignment(Pos.CENTER);

        checksum_Label = new Label(language.getProperty("checksum_label",
                "Checksum"));
        checksum_TextField = new TextField();
        checksum_TextField.setPromptText(language.getProperty("enterChecksumHere_str",
                "Enter checksum of file here."));
        checksum_TextField.textProperty().addListener(this::checksum_TextChanged);

        checksumAlgorithm_Label = new Label("");
        checksumAlgorithm_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 12.0D));
        checksumAlgorithm_Label.setTextFill(Color.FORESTGREEN);

        checksum_HBox = new HBox(javaFXgui.rem * 0.6D, checksum_Label, checksum_TextField,
                checksumAlgorithm_Label);
        checksum_HBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(checksum_HBox, Priority.ALWAYS);
        HBox.setHgrow(checksum_TextField, Priority.ALWAYS);

        Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.setMinHeight(javaFXgui.rem * 0.2D);
        separator.setPrefHeight(javaFXgui.rem * 0.3D);

        //==================== MD5
        Label md5FileChecksum_Label = new Label("MD5");
        md5FileChecksum_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL,
                10.0D));

        md5FileChecksum_TextField = new TextField();
        md5FileChecksum_TextField.setEditable(false);

        md5_ProgressIndicator = new ProgressIndicator();
        md5_ProgressIndicator.setMaxHeight(javaFXgui.rem * 1.4D);

        md5Equals_Label = new Label();
        md5Equals_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 24.0D));

        md5_HBox = new HBox(javaFXgui.rem * 0.4D, md5FileChecksum_TextField,
                md5_ProgressIndicator, md5Equals_Label);
        md5_HBox.setAlignment(Pos.CENTER_LEFT);

        md5FileChecksum = new VBox(md5FileChecksum_Label, md5_HBox);
        HBox.setHgrow(md5_HBox, Priority.ALWAYS);
        HBox.setHgrow(md5FileChecksum_TextField, Priority.ALWAYS);

        //==================== SHA1
        Label sha1FileChecksum_Label = new Label("SHA1");
        sha1FileChecksum_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL,
                10.0D));

        sha1FileChecksum_TextField = new TextField();
        sha1FileChecksum_TextField.setEditable(false);

        sha1_ProgressIndicator = new ProgressIndicator();
        sha1_ProgressIndicator.setMaxHeight(md5_ProgressIndicator.getMaxHeight());

        sha1Equals_Label = new Label();
        sha1Equals_Label.setFont(md5Equals_Label.getFont());
        sha1_HBox = new HBox(javaFXgui.rem * 0.4D, sha1FileChecksum_TextField,
                sha1_ProgressIndicator, sha1Equals_Label);
        sha1_HBox.setAlignment(Pos.CENTER_LEFT);


        sha1FileChecksum = new VBox(sha1FileChecksum_Label, sha1_HBox);
        sha1FileChecksum.setFillWidth(true);
        HBox.setHgrow(sha1_HBox, Priority.ALWAYS);
        HBox.setHgrow(sha1FileChecksum_TextField, Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxHeight(javaFXgui.rem * 0.2D);

        borderPane = new BorderPane();
        borderPane.setBottom(progressBar);


        fileChecksums_VBox = new VBox(javaFXgui.rem * 1.2D, md5FileChecksum,
                sha1FileChecksum);

        root.getChildren().addAll(fileName_HBox,
                checksum_HBox, separator, fileChecksums_VBox, borderPane);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Checksum");
        stage.setMinWidth(javaFXgui.rem * 19.0D);
        stage.setMinHeight(javaFXgui.rem * 14.0D);

    }

    private void createAndStartThread()
    {
        calculatingChecksum_Thread = new Thread(() ->
        {
            try
            {
                Map<ChecksumAlgorithms, String> calculatedChecksums = ChecksumCalculator.calculateChecksums(new BufferedInputStream(
                                Files.newInputStream(targetFilePath, StandardOpenOption.READ)), ChecksumAlgorithms.MD5,
                        ChecksumAlgorithms.SHA1);
                Platform.runLater(() ->
                {
                    appearChecksum(calculatedChecksums);
                });
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        });

        calculatingChecksum_Thread.start();
    }

    private void appearChecksum(Map<ChecksumAlgorithms, String> checksums)
    {
        for (ChecksumAlgorithms algorithm : checksums.keySet())
        {
            if (algorithm == ChecksumAlgorithms.MD5)
            {
                md5FileChecksum_TextField.setText(checksums.get(algorithm));
            }
            else if (algorithm == ChecksumAlgorithms.SHA1)
            {
                sha1FileChecksum_TextField.setText(checksums.get(algorithm));
            }
        }

        md5_HBox.getChildren().remove(md5_ProgressIndicator);
        sha1_HBox.getChildren().remove(sha1_ProgressIndicator);
        checkChecksumEquals();
    }

    private void checksum_TextChanged(ObservableValue<?> value, String oldValue,
                                      String newValue)
    {
        checkChecksumEquals();
    }

    private void checkChecksumEquals()
    {
        if (calculatingChecksum_Thread.isAlive())
        {
            return;
        }

        if (checksum_TextField.getText() != null &&
                !checksum_TextField.getText().equals("")
                && !checksum_TextField.getText().equals(" "))
        {
            if (md5FileChecksum_TextField.getText().equals(checksum_TextField.getText()
                    .toLowerCase()))
            {
                md5Equals_Label.setText("✅");
                md5Equals_Label.setTextFill(Color.DARKGREEN);
                checksumAlgorithm_Label.setText(ChecksumAlgorithms.MD5.name());

                sha1Equals_Label.setText("⛔");
                sha1Equals_Label.setTextFill(Color.DARKORANGE);

            }
            else if (sha1FileChecksum_TextField.getText().equals(checksum_TextField.getText()
                    .toLowerCase()))
            {
                sha1Equals_Label.setText("✅");
                sha1Equals_Label.setTextFill(Color.DARKGREEN);
                checksumAlgorithm_Label.setText(ChecksumAlgorithms.SHA1.name());

                md5Equals_Label.setText("⛔");
                md5Equals_Label.setTextFill(Color.DARKORANGE);

            }
            else
            {
                md5Equals_Label.setText("⛔");
                md5Equals_Label.setTextFill(Color.DARKORANGE);
                sha1Equals_Label.setText("⛔");
                sha1Equals_Label.setTextFill(Color.DARKORANGE);
                checksumAlgorithm_Label.setText("");
            }
        }
    }

    public void show()
    {
        stage.show();
        createAndStartThread();
    }
}
