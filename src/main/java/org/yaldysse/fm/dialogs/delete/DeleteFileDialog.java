package org.yaldysse.fm.dialogs.delete;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.yaldysse.fm.FM_GUI;
import org.yaldysse.fm.dialogs.*;
import org.yaldysse.patterns.observer.Observer;
import org.yaldysse.patterns.observer.Subject;
import org.yaldysse.tools.StorageCapacity;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Описывает диалоговое окно с удалением файлов. После согласия на удаление
 * отображается окно с процессом удаления. Удаление происходит в отдельном потоке,
 * поведение которого описывает класс {@link org.yaldysse.fm.dialogs.delete.DeleteFiles}.
 * 14.09.2022 Был изменен под использование паттерна Наблюдатель, вследствии чего
 * отпала надобность в реализации интерфейса DeleteProgress.
 */
public class DeleteFileDialog implements FilesNumberAndSizeCalculator,
        Observer
{
    public static final double rem = new Text("").getBoundsInParent().getHeight();

    private Scene scene;
    private VBox root;

    private Label fileName_Label;
    private Label totalFilesNumber_Label;
    private Label fileNameValue_Label;
    private Label totalFilesNumberValue_Label;
    private Label totalSize_Label;
    private Label totalSizeValue_Label;
    private Label deletedFiles_Label;
    private Label deletedFilesValue_Label;
    private Label totalSizeBytes_Label;
    private Label totalSizeBytesValue_Label;
    private Label progressFiles_Label;
    private Label progressFilesValue_Label;
    private ProgressBar progressBar;
    private ProgressIndicator progressIndicator;
    private GridPane operationInfo_GridPane;
    private HBox progress_HBox;
    private Button stopDeleting_Button;
    private BorderPane buttons_BorderPane;
    private int countFiles;
    private int deletedFiles;
    private long totalSize;
    private Path[] paths;
    private Thread calculateFilesNumberAndTotalSize_Thread;
    private ConfirmOperationDialog confirmOperationDialog;
    private Stage stage;
    private Thread deleteFiles_Thread;
    private DeleteFiles deleteFilesRunnable;
    private DeleteOperationResult operationResult;
    private Button ok_Button;
    private VBox errors_VBox;
    private TitledPane errorLog_TitledPane;
    private Properties language;

    private VBox filesToDeleting_VBox;
    private Label[] files_Labels;
    private FM_GUI fm_gui;


    /**
     * В таком случае в обязательном порядке воспользоваться методом {@link #setTargetPaths(Path[])}
     */
    public DeleteFileDialog(FM_GUI aFmGui, final Properties newLanguageProperties)
    {
        language = newLanguageProperties;
        initializeComponents();
        countFiles = 0;
        totalSize = 0;
        deletedFiles = 0;
        fm_gui = aFmGui;

        initializeConfirmDialogComponents();
    }

    public DeleteFileDialog(final Path[] targetPaths, FM_GUI aFmGui, final Properties newLanguageProperties)
    {
        language = newLanguageProperties;
        initializeComponents();
        countFiles = 0;
        totalSize = 0;
        deletedFiles = 0;
        paths = new Path[targetPaths.length];
        fm_gui = aFmGui;

        paths = Arrays.copyOf(targetPaths, targetPaths.length);

        createAndStartThread();
        initializeConfirmDialogComponents();
    }

    private void initializeComponents()
    {
        root = new VBox(rem * 0.8D);
        root.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.MEDIUM, new Insets(rem * 0.4D))));
        root.setPadding(new Insets(rem * 0.8D));

        Label title_Label = new Label("Deleting");
        title_Label.setFont(Font.font(title_Label.getFont().getName(),
                FontWeight.EXTRA_BOLD, 18.0D));
        //header_Label.setPadding(new Insets(0.0D, rem * 0.4D, 0.0D, 0.4D));
        title_Label.setTextFill(Color.GHOSTWHITE);

        HBox title_HBox = new HBox();
        title_HBox.setBackground(new Background(new BackgroundFill(Color.INDIANRED,
                CornerRadii.EMPTY, Insets.EMPTY)));
        title_HBox.getChildren().add(title_Label);
        title_HBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(title_HBox, Priority.ALWAYS);

        fileName_Label = new Label(language.getProperty("fileName_label",
                "Name of file:"));

        fileNameValue_Label = new Label("?");
        fileNameValue_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 11.0D));

        totalFilesNumber_Label = new Label(language.getProperty("totalFilesNumber_label",
                "Total files:"));
        totalFilesNumberValue_Label = new Label("?");

        totalSize_Label = new Label(language.getProperty("totalSize_label",
                "Total size:"));
        totalSize_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        totalSizeValue_Label = new Label("?");

        deletedFiles_Label = new Label(language.getProperty("deletedFiles_label",
                "Deleted files:"));
        deletedFilesValue_Label = new Label("? of ?");

        totalSizeBytes_Label = new Label(language.getProperty("totalSizeBytes_label",
                "Total size (bytes):"));
        totalSizeBytes_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));

        totalSizeBytesValue_Label = new Label("?");
        totalSizeBytesValue_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.MEDIUM, 11.0D));
        totalSizeBytesValue_Label.setOpacity(0.8D);

        VBox totalSizeValues_VBox = new VBox(0.1D, totalSizeValue_Label, new Separator(Orientation.HORIZONTAL),
                totalSizeBytesValue_Label);
        totalSizeValues_VBox.setFillWidth(true);

        progressFiles_Label = new Label(language.getProperty("progressFiles_label",
                "Progress:"));
        progressFiles_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        progressFilesValue_Label = new Label("?");

        progressBar = new ProgressBar(0.0D);
        progressBar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        progressBar.setMinHeight(rem);


        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefHeight(20.0D);

        errors_VBox = new VBox(rem * 0.5D);
        errors_VBox.setFillWidth(true);
        errors_VBox.setPadding(new Insets(rem * 0.3D));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(errors_VBox);

        errorLog_TitledPane = new TitledPane();
        errorLog_TitledPane.setText(language.getProperty("errorsLog_titledPane",
                "Errors"));
        errorLog_TitledPane.setContent(scrollPane);
        errorLog_TitledPane.setVisible(false);


        operationInfo_GridPane = new GridPane();
        operationInfo_GridPane.setHgap(rem * 1.15D);
        operationInfo_GridPane.setVgap(rem * 0.35D);

//        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), fileName_Label,
//                fileNameValue_Label);
//        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), totalFilesNumber_Label,
//                totalFilesNumberValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), progressFiles_Label,
                progressFilesValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), totalSize_Label,
                totalSizeValues_VBox);
//        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), totalSizeBytes_Label,
//                totalSizeBytesValue_Label);
//        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), deletedFiles_Label,
//                deletedFilesValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), progressIndicator);
        operationInfo_GridPane.add(errorLog_TitledPane, 0, operationInfo_GridPane.getRowCount(),
                3, 1);
        GridPane.setHgrow(errorLog_TitledPane, Priority.ALWAYS);


        progress_HBox = new HBox(progressBar);
        progress_HBox.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox fileNameAndProgressBar_VBox = new VBox(fileNameValue_Label, progress_HBox);


        progressBar.prefWidthProperty().bind(progress_HBox.widthProperty());

        stopDeleting_Button = new Button(language.getProperty("stopOperation_button",
                "Cancel"));
        stopDeleting_Button.setOnAction(this::stopDeleting_ButtonAction);

        ok_Button = new Button("OK");
        ok_Button.setOnAction(this::okButton_Action);
        ok_Button.setDisable(true);

        HBox buttons_HBox = new HBox(rem * 0.45D, stopDeleting_Button, ok_Button);
        buttons_HBox.setAlignment(Pos.BOTTOM_RIGHT);

        buttons_BorderPane = new BorderPane();
        buttons_BorderPane.setBottom(buttons_HBox);

        root.setFillWidth(true);
        root.getChildren().addAll(title_HBox, fileNameAndProgressBar_VBox, operationInfo_GridPane,
                buttons_BorderPane);
        HBox.setHgrow(progress_HBox, Priority.ALWAYS);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(language.getProperty("deletingFiles_dialogTitle",
                "Deleting Files"));
        stage.initStyle(StageStyle.TRANSPARENT);
    }


    private void initializeConfirmDialogComponents()
    {
        filesToDeleting_VBox = new VBox(rem * 0.15D);
        files_Labels = null;

        if (paths != null)
        {
            files_Labels = new Label[paths.length];
            for (int k = 0; k < paths.length; k++)
            {
                files_Labels[k] = new Label(paths[k].getFileName().toString());
                filesToDeleting_VBox.getChildren().add(files_Labels[k]);
            }
        }

        confirmOperationDialog = new ConfirmOperationDialog(StageStyle.UTILITY);
        confirmOperationDialog.initModality(Modality.APPLICATION_MODAL);
        confirmOperationDialog.setHeaderText("Delete");
        confirmOperationDialog.setHeaderColor(Color.INDIANRED);
        confirmOperationDialog.setMessageText("Are you sure You want to delete these files ?");
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(filesToDeleting_VBox);
        confirmOperationDialog.setBackgroundToRootNode(new Background(
                new BackgroundFill(Color.DARKSALMON, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 13.0D));

    }

    private void addItemToErrorsVBox(final Path targetPath, final String description)
    {
        if (errors_VBox.getChildren().size() >= 10)
        {
            return;
        }

        VBox item_VBox = new VBox(rem * 0.2D);
        item_VBox.setFillWidth(true);

        Label targetFilePath_Label = new Label(targetPath.getFileName().toString());
        targetFilePath_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 14.0D));

        Label errorDescription_Label = new Label(description);
        errorDescription_Label.setTextFill(Color.WHITESMOKE);
        errorDescription_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 12.0D));

        HBox errorDescription_HBox = new HBox(errorDescription_Label);
        errorDescription_HBox.setPrefWidth(
                errorDescription_Label.getBoundsInParent().getWidth() + rem * 2.0D);
        errorDescription_HBox.setAlignment(Pos.CENTER);
        errorDescription_HBox.setBackground(new Background(new BackgroundFill(
                Color.CRIMSON, new CornerRadii(2D), Insets.EMPTY)));
        errorDescription_HBox.setOnMouseClicked(event ->
        {
            new FileAttributesEditor(targetPath, language).show();
        });

        item_VBox.getChildren().addAll(targetFilePath_Label, errorDescription_HBox,
                new Separator(Orientation.HORIZONTAL));

        errors_VBox.getChildren().add(item_VBox);
    }


    /**
     * Позволяет задать файлы, над которыми нужно будет применить операцию.
     * Автоматически запрашивает пересчет размера и количества файлов.
     */
    public void setTargetPaths(final Path[] targetPaths)
    {
        paths = null;
        paths = new Path[targetPaths.length];

        paths = Arrays.copyOf(targetPaths, targetPaths.length);

        countFiles = 0;
        totalSize = 0;
        deletedFiles = 0;

        updateFilesList();
        createAndStartThread();
        errors_VBox.getChildren().clear();
        if (!operationInfo_GridPane.getChildren().contains(progressIndicator))
        {
            operationInfo_GridPane.getChildren().add(progressIndicator);
        }
    }

    private void updateFilesList()
    {
        if (paths != null)
        {
            filesToDeleting_VBox.getChildren().clear();
            files_Labels = new Label[paths.length];
            for (int k = 0; k < paths.length; k++)
            {
                files_Labels[k] = new Label(paths[k].getFileName().toString());
                filesToDeleting_VBox.getChildren().add(files_Labels[k]);
            }
        }
    }


    private void createAndStartThread()
    {
        calculateFilesNumberAndTotalSize_Thread = new Thread(
                new SimpleFileSizeAndNumberCounter(paths, this),
                "Calculate files number and total size of them");
        calculateFilesNumberAndTotalSize_Thread.start();
    }


    /**
     * Не только скрывает сцену диалога, но и обнуляет значения на случай повторного
     * использования того же объекта.
     */
    public void hide()
    {
        stage.hide();
        totalSizeValue_Label.setText("?");
        totalFilesNumberValue_Label.setText("?");
        totalSizeBytesValue_Label.setText("?");
    }

    public void show()
    {
        confirmOperationDialog.showAndWait();

        if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
        {
            stage.show();
            deleteFilesRunnable = new DeleteFiles(paths, this);
            deleteFiles_Thread = new Thread(deleteFilesRunnable,
                    "Deleting files");

            try
            {
                if (calculateFilesNumberAndTotalSize_Thread.isAlive())
                {
                    System.out.println("В ожидании завершения потока перебора файлов.");
                    calculateFilesNumberAndTotalSize_Thread.join();
                    operationInfo_GridPane.getChildren().remove(progressIndicator);
                }
            }
            catch (InterruptedException interruptedException)
            {
                interruptedException.printStackTrace();
            }
            deleteFiles_Thread.start();
        }

    }

    @Override
    public void appearInNodes(int aFilesNumber, long aTotalSize)
    {
        countFiles = aFilesNumber;
        totalSize = aTotalSize;

        totalSizeValue_Label.setText("" + StorageCapacity.ofBytes(aTotalSize).toString());
        //totalFilesNumberValue_Label.setText("" + aFilesNumber);
        totalSizeBytesValue_Label.setText("" + NumberFormat.getNumberInstance().format(aTotalSize));
        progressFilesValue_Label.setText(" of " + countFiles);
        operationInfo_GridPane.getChildren().remove(progressIndicator);
        scene.getWindow().sizeToScene();
    }

    /**
     * Этот метод вызывается из потока удаления файлов для отображения прогреса.
     */
    public void appearDeleteProgress(int aDeletedFilesNumber, Path currentPath,
                                     final boolean completed, final boolean canceled,
                                     final ArrayList<Path> accessDeniedErrorsPaths)
    {
        fileNameValue_Label.setText(currentPath.toAbsolutePath().toString());
        //deletedFilesValue_Label.setText("" + aDeletedFilesNumber);
        progressFilesValue_Label.setText(aDeletedFilesNumber + " of " + countFiles);

        deletedFiles = aDeletedFilesNumber;

        //рассчитываем процент выполнения операции по отношению к количеству удаленных файлов

        progressBar.setProgress((double) deletedFiles * 100.0D / (double) countFiles);

        if (completed)
        {
            if (canceled)
            {
                fileNameValue_Label.setText(language.getProperty("copyingFilesHaveBeenStopped_str",
                        "Deleting files have been canceled."));
                operationResult = DeleteOperationResult.CANCELED;

            }
            else if (deletedFiles == countFiles)
            {
                fileNameValue_Label.setText(language.getProperty("deletingFilesHaveBeenCompleted_str",
                        "Files have been successfully deleted."));
                operationResult = DeleteOperationResult.COMPLETED_SUCCESSFULLY;
            }
            else
            {
                fileNameValue_Label.setText(language.getProperty("deletingOperationWithErrors_str",
                        "Deleting operation have been ended with errors."));
                operationResult = DeleteOperationResult.COMPLETED_WITH_ERRORS;

                if (accessDeniedErrorsPaths.size() > 0)
                {
                    for (Path temporaryPath : accessDeniedErrorsPaths)
                        errorDetected(temporaryPath, language.getProperty("accessDenied_str",
                                "Access Denied"));
                }
            }
            stopDeleting_Button.setDisable(true);
            ok_Button.setDisable(false);
            //fm_gui.updateFilesListAfterDeleting(operationResult);
        }
    }


    public void errorDetected(Path targetPath, String description)
    {
        //System.out.println(targetPath.toString());
        if (!errorLog_TitledPane.isVisible())
        {
            errorLog_TitledPane.setVisible(true);
        }
        if (errors_VBox.getChildren().size() == 9)
        {
            addItemToErrorsVBox(Path.of("<more>"), description);
        }
        if (errors_VBox.getChildren().size() >= 10)
        {
            return;
        }
        addItemToErrorsVBox(targetPath, description);
    }

    private void stopDeleting_ButtonAction(ActionEvent event)
    {
        deleteFilesRunnable.setInterrupt(true);
        fileNameValue_Label.setText("Поток удаления должен быть прерван.");

    }

    public Path[] getFilesToDeleting()
    {
        return paths;
    }

    private void okButton_Action(ActionEvent event)
    {
        stage.hide();
    }

    @Override
    public void updateData(Subject subject)
    {
        DeleteFiles deleteFilesOperation = (DeleteFiles) subject;
        appearDeleteProgress(deleteFilesOperation.getDeletedFilesNumber(), deleteFilesOperation.getCurrentFilePath(),
                deleteFilesOperation.isCompleted(), deleteFilesOperation.isInterrupted(),
                deleteFilesOperation.getAccessDeniedErrorPaths());
    }
}

