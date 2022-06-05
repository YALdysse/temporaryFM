package org.yaldysse.fm.dialogs.copy;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.yaldysse.fm.ConfirmOperationButton;
import org.yaldysse.fm.FM_GUI;
import org.yaldysse.fm.dialogs.ConfirmDialogButtonType;
import org.yaldysse.fm.dialogs.ConfirmOperationDialog;
import org.yaldysse.fm.dialogs.FilesNumberAndSizeCalculator;
import org.yaldysse.fm.dialogs.SimpleFileSizeAndNumberCounter;
import org.yaldysse.fm.dialogs.delete.DeleteOperationResult;
import org.yaldysse.tools.StorageCapacity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;

public class CopyFilesDialog implements CopyOperationProgress, FilesNumberAndSizeCalculator
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
    private Label copiedBytes_Label;
    private Label copiedBytesValue_Label;
    private Label copiedSize_Label;
    private Label copiedSizeValue_Label;
    private Label totalSizeBytes_Label;
    private Label totalSizeBytesValue_Label;
    private Label progressFiles_Label;
    private Label progressFilesValue_Label;
    private Label speed_Label;
    private Label speedValue_Label;
    private ProgressBar progressBar;
    private ProgressIndicator progressIndicator;
    private GridPane operationInfo_GridPane;
    private HBox progress_HBox;
    private Button stopOperation_Button;
    private BorderPane buttons_BorderPane;
    private int filesNumber;
    private int copiedFiles;
    private long totalSize;
    private Path[] paths;
    private Path[] destinationPaths;

    private Thread calculateFilesNumberAndTotalSize_Thread;
    private ConfirmOperationDialog confirmOperationDialog;
    private Stage stage;
    private Thread copyFiles_Thread;
    private CopyFiles copyFilesRunnable;
    private DeleteOperationResult operationResult;
    private Button ok_Button;

    private VBox fileAlreadyExists_VBox;
    private Label[] files_Labels;
    private FM_GUI fm_gui;

    private Label sourceFile_Label;
    private Label destinationFile_Label;
    private Label sourceFileValue_Label;
    private Label destinationFileValue_Label;
    private Separator separator;
    private Label sourceFileSize_Label;
    private Label sourceFileSizeValue_Label;
    private Label destinationFileSize_Label;
    private Label destinationFileSizeValue_Label;
    private Label sourceFilePath_Label;
    private Label sourceFilePathValue_Label;
    private Label destinationFilePath_Label;
    private Label destinationFilePathValue_Label;

    public CopyFilesDialog(final Path aSourcePath, final Path aDestinationPath)
    {
        initializeComponents();
        filesNumber = 0;
        totalSize = 0;
        copiedFiles = 0;
        paths = new Path[1];
        paths[0] = aSourcePath;
        destinationPaths = new Path[1];
        destinationPaths[0] = aDestinationPath;
        createAndStartThread();
        initializeConfirmDialogComponents();
    }

    /**
     * В таком случае в обязательном порядке воспользоваться методом {@link #setPathsToCopy(Path[], Path[])}}
     */
    public CopyFilesDialog(FM_GUI aFmGui)
    {
        initializeComponents();
        filesNumber = 0;
        totalSize = 0;
        copiedFiles = 0;
        fm_gui = aFmGui;

        initializeConfirmDialogComponents();
    }

    public CopyFilesDialog(final Path[] aSourcePaths, final Path[] aDestinationPaths,
                           FM_GUI aFmGui)
    {
        initializeComponents();
        filesNumber = 0;
        totalSize = 0;
        copiedFiles = 0;
        paths = aSourcePaths;
        destinationPaths = aDestinationPaths;
        fm_gui = aFmGui;

        createAndStartThread();
        initializeConfirmDialogComponents();
    }

    private void initializeComponents()
    {
        root = new VBox(rem * 0.8D);
        root.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.MEDIUM, new Insets(rem * 0.4D))));
        root.setPadding(new Insets(rem * 0.8D));

        Label title_Label = new Label("Copying");
        title_Label.setFont(Font.font(title_Label.getFont().getName(),
                FontWeight.EXTRA_BOLD, 18.0D));
        //header_Label.setPadding(new Insets(0.0D, rem * 0.4D, 0.0D, 0.4D));
        title_Label.setTextFill(Color.GHOSTWHITE);

        HBox title_HBox = new HBox();
        title_HBox.setBackground(new Background(new BackgroundFill(Color.GREEN,
                CornerRadii.EMPTY, Insets.EMPTY)));
        title_HBox.getChildren().add(title_Label);
        title_HBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(title_HBox, Priority.ALWAYS);

        fileName_Label = new Label("Name of file:");

        fileNameValue_Label = new Label("?");
        fileNameValue_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 11.0D));

        totalFilesNumber_Label = new Label("Total files:");
        totalFilesNumberValue_Label = new Label("?");

        totalSize_Label = new Label("Total size:");
        totalSize_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        totalSizeValue_Label = new Label("?");

        copiedBytes_Label = new Label("Copied bytes:");
        copiedBytes_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        copiedBytesValue_Label = new Label("? of ?");

        totalSizeBytes_Label = new Label("Total size (bytes):");
        totalSizeBytes_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        totalSizeBytesValue_Label = new Label("?");

        copiedSize_Label = new Label("Copied size:");
        copiedSize_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));

        copiedSizeValue_Label = new Label();

        speed_Label = new Label("Speed:");
        speed_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));

        speedValue_Label = new Label();
        speedValue_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 11.0D));


        progressFiles_Label = new Label("Progress:");
        progressFiles_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        progressFilesValue_Label = new Label("?");

        progressBar = new ProgressBar(0.0D);
        progressBar.setPrefWidth(Region.USE_COMPUTED_SIZE);


        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefHeight(20.0D);

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
                totalSizeValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), totalSizeBytes_Label,
                totalSizeBytesValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), copiedSize_Label,
                copiedSizeValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), copiedBytes_Label,
                copiedBytesValue_Label);
        //operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), speedValue_Label);
        operationInfo_GridPane.addRow(operationInfo_GridPane.getRowCount(), progressIndicator);


        progress_HBox = new HBox(progressBar);
        progress_HBox.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        HBox speed_HBox = new HBox(speedValue_Label);
        speed_HBox.setAlignment(Pos.TOP_RIGHT);

        VBox fileNameAndProgressBar_VBox = new VBox(fileNameValue_Label, progress_HBox,
                speed_HBox);


        progressBar.prefWidthProperty().bind(progress_HBox.widthProperty());

        stopOperation_Button = new Button("Cancel");
        stopOperation_Button.setOnAction(this::stopOperation_ButtonAction);

        ok_Button = new Button("OK");
        ok_Button.setOnAction(this::okButton_Action);
        ok_Button.setDisable(true);

        HBox buttons_HBox = new HBox(rem * 0.45D, stopOperation_Button, ok_Button);
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
        stage.setTitle("Copying Files");
        stage.initStyle(StageStyle.TRANSPARENT);
    }

    private void createAndStartThread()
    {
        calculateFilesNumberAndTotalSize_Thread = new Thread(
                new SimpleFileSizeAndNumberCounter(paths, this),
                "Calculate files number and total size of them");
        calculateFilesNumberAndTotalSize_Thread.start();
    }

    private void stopOperation_ButtonAction(ActionEvent event)
    {
        copyFilesRunnable.setInterrupt(true);
        fileNameValue_Label.setText("Поток копирования файлов должен быть прерван.");
        stopOperation_Button.setDisable(true);
        ok_Button.setDisable(false);

    }

    private void okButton_Action(ActionEvent event)
    {
        stage.hide();
    }

    public void show()
    {
        stage.show();
        copyFilesRunnable = new CopyFiles(paths, destinationPaths, this);
        copyFiles_Thread = new Thread(copyFilesRunnable,
                "Copying files");

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
        copyFiles_Thread.start();
    }


    /**
     * Предназначен для вызова из потока копирования.
     */
    @Override
    public void appearOperationProgress(boolean completed, final boolean interrupted
            , int copiedFilesNumber, final int finishedFilesNumber, long copiedBytes,
                                        final long copiedBytesPortion, final Path processedFilePath)
    {
        progressFilesValue_Label.setText("" + finishedFilesNumber + " of " + filesNumber);
        copiedBytesValue_Label.setText(NumberFormat.getNumberInstance().format(copiedBytes));
        copiedSizeValue_Label.setText(StorageCapacity.ofBytes(copiedBytes).toString());
        //System.out.println("Порция: " + copiedBytesPortion);
        speedValue_Label.setText(StorageCapacity.ofBytes(copiedBytesPortion).toString());
        fileNameValue_Label.setText(processedFilePath.getFileName().toString());

        double progressPercent = (((double) copiedBytes * 100.0D) / (double) totalSize) / 100.0D;
        //System.out.println("Percent: " + progressPercent);
        progressBar.setProgress(progressPercent);

        if (completed)
        {
            if (interrupted)
            {
                stopOperation_Button.setDisable(true);
                ok_Button.setDisable(false);
                fileNameValue_Label.setText("Copying files have been stopped.");
            }
            else
            {
                stopOperation_Button.setDisable(true);
                ok_Button.setDisable(false);
                fileNameValue_Label.setText("Copying files have been completed.");
            }
            fm_gui.updateFilesListAfterCopying(destinationPaths);
        }
    }

    @Override
    public void fileAlreadyExists(Path sourceFilePath, Path destinationFilePath)
    {
        System.out.println("Поток копирования должен быть переведен в режим " +
                "ожидания. Текущий: " + Thread.currentThread().getName());
        System.out.println("Состояние потока копирования: " + copyFiles_Thread.getState().name());

        sourceFileValue_Label.setText(sourceFilePath.getFileName().toString());

        try
        {
            sourceFileSizeValue_Label.setText(StorageCapacity.ofBytes(Files.size(sourceFilePath)).toString());
            destinationFileSizeValue_Label.setText(StorageCapacity.ofBytes(Files.size(destinationFilePath)).toString());
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }


        //confirmOperationDialog.hide();
        confirmOperationDialog.showAndWait();
        copyFilesRunnable.setCopyOption(confirmOperationDialog.getActivatedOperationButton());
        synchronized (copyFilesRunnable.forLock)
        {
            copyFilesRunnable.forLock.notify();
        }
    }

    @Override
    public void appearInNodes(int aFilesNumber, long aTotalSize)
    {
        filesNumber = aFilesNumber;
        totalSize = aTotalSize;

        totalSizeValue_Label.setText("" + StorageCapacity.ofBytes(aTotalSize).toString());
        //totalFilesNumberValue_Label.setText("" + aFilesNumber);
        totalSizeBytesValue_Label.setText("" + NumberFormat.getNumberInstance().format(aTotalSize));
        progressFilesValue_Label.setText(" of " + filesNumber);
        operationInfo_GridPane.getChildren().remove(progressIndicator);
        scene.getWindow().sizeToScene();
    }

    /**
     * Позволяет задать файлы, над которыми нужно будет применить операцию. Автоматически
     * запрашивает пересчет количества файлов и размер.
     *
     * @see
     */
    public void setPathToCopy(final Path aSourcePath, final Path aDestinationPaths)
    {
        paths = null;
        paths = new Path[1];
        paths[0] = aSourcePath;

        destinationPaths = null;
        destinationPaths = new Path[1];
        destinationPaths[0] = aDestinationPaths;
        updateInfo();
        createAndStartThread();
        if (!operationInfo_GridPane.getChildren().contains(progressIndicator))
        {
            operationInfo_GridPane.getChildren().add(progressIndicator);
        }
    }

    /**
     * Позволяет задать файлы, над которыми нужно будет применить операцию.
     * Автоматически запрашивает пересчет размера и количества файлов.
     */
    public void setPathsToCopy(final Path[] aSourcePaths, final Path[] aDestinationPaths)
    {
        paths = null;
        paths = aSourcePaths;
        destinationPaths = aDestinationPaths;

        updateInfo();
        createAndStartThread();
        if (!operationInfo_GridPane.getChildren().contains(progressIndicator))
        {
            operationInfo_GridPane.getChildren().add(progressIndicator);
        }
    }

    private void updateInfo()
    {
        copiedFiles = 0;
        totalSize = 0;
        filesNumber = 0;
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

    private void initializeConfirmDialogComponents()
    {
        fileAlreadyExists_VBox = new VBox();

        sourceFile_Label = new Label("Source File:");
        sourceFile_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, FontPosture.ITALIC, 15.0D));

        destinationFile_Label = new Label("Destination File:");

        sourceFileValue_Label = new Label();
        sourceFileValue_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, FontPosture.ITALIC, 15.0D));

        destinationFileValue_Label = new Label();

        separator = new Separator(Orientation.VERTICAL);

        sourceFileSize_Label = new Label("Size:");
        sourceFileSize_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 14.0D));

        sourceFileSizeValue_Label = new Label();


        destinationFileSize_Label = new Label("Size:");
        destinationFileSize_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 14.0D));

        destinationFileSizeValue_Label = new Label();


        HBox sourceFileSize_HBox = new HBox(rem * 0.35D, sourceFileSize_Label,
                sourceFileSizeValue_Label);
        HBox destinationFileSize_HBox = new HBox(rem * 0.35D, destinationFileSize_Label,
                destinationFileSizeValue_Label);

        sourceFilePath_Label = new Label("Path:");
        sourceFilePathValue_Label = new Label();

        destinationFilePath_Label = new Label("Path:");
        destinationFilePathValue_Label = new Label();

        HBox sourceFile_HBox = new HBox(sourceFileValue_Label);
        sourceFile_HBox.setAlignment(Pos.TOP_CENTER);

        HBox destinationFile_HBox = new HBox(destinationFile_Label);
        destinationFile_HBox.setAlignment(Pos.TOP_CENTER);


        GridPane sourceFileInfo_GridPane = new GridPane();
        sourceFileInfo_GridPane.setHgap(rem * 0.25D);
        sourceFileInfo_GridPane.setVgap(rem * 0.4D);

        sourceFileInfo_GridPane.addRow(sourceFileInfo_GridPane.getRowCount(),
                sourceFileSize_Label, sourceFileSizeValue_Label);

        GridPane destinationFileInfo_GridPane = new GridPane();
        destinationFileInfo_GridPane.setHgap(rem * 0.25D);
        destinationFileInfo_GridPane.setVgap(rem * 0.4D);
        destinationFileInfo_GridPane.addRow(destinationFileInfo_GridPane.getRowCount(),
                destinationFileSize_Label, destinationFileSizeValue_Label);
        GridPane.setHgrow(sourceFileInfo_GridPane, Priority.ALWAYS);
        GridPane.setHgrow(destinationFileInfo_GridPane, Priority.ALWAYS);

        sourceFileInfo_GridPane.setAlignment(Pos.TOP_LEFT);
        destinationFileInfo_GridPane.setAlignment(Pos.TOP_RIGHT);

        Label arrow_Label = new Label("➩");
        arrow_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 22.0D));

        HBox filesInfo_HBox = new HBox(rem * 0.45D,
                sourceFileInfo_GridPane, arrow_Label, destinationFileInfo_GridPane);
        filesInfo_HBox.setPadding(new Insets(rem * 0.5D, 0.0D, 0.0D, 0.0D));
        HBox.setHgrow(filesInfo_HBox, Priority.ALWAYS);

        fileAlreadyExists_VBox.getChildren().addAll(sourceFile_HBox,
                filesInfo_HBox);
        fileAlreadyExists_VBox.setFillWidth(true);
        VBox.setVgrow(separator, Priority.ALWAYS);


        confirmOperationDialog = new ConfirmOperationDialog(StageStyle.UTILITY);
        confirmOperationDialog.initModality(Modality.APPLICATION_MODAL);
        confirmOperationDialog.setTitle("File already exists");
        confirmOperationDialog.setHeaderText("Copy");
        confirmOperationDialog.setHeaderColor(Color.GREEN);
        confirmOperationDialog.setMessageText("File with same name already exists in this directory. What would yo like do ?");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.UNITE, ConfirmDialogButtonType.UNITE_ALL, ConfirmDialogButtonType.SKIP);
        ConfirmOperationButton confirmButton = (ConfirmOperationButton) confirmOperationDialog.getOperationButtons().get(1);
        confirmButton.setText("Unite");
        confirmOperationDialog.setContent(fileAlreadyExists_VBox);
        confirmOperationDialog.setConfirmOperationOnEnterKey(true);
        confirmOperationDialog.setBackgroundToRootNode(new Background(new BackgroundFill(Color.LIGHTGREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMinHeight(rem * 16.0D);
        confirmOperationDialog.setMinWidth(rem * 23.0D);
        //confirmOperationDialog.showAndWait();

    }


}
