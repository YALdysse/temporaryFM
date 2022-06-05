package org.yaldysse.fm.dialogs;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.yaldysse.fm.FM_GUI;
import org.yaldysse.fm.dialogs.delete.DeleteFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class OpenWithCustomProgramDialog
{
    public static final double rem = new Text("").getBoundsInParent().getHeight();

    private Scene scene;
    private VBox root;

    private TextField command_TextField;
    private Label programName_Label;
    private Label targetFile_Label;
    private TextField programName_TextField;
    private Button chooseProgram_Button;
    private Button execute_Button;
    private CheckBox customCommand_CheckBox;

    private BorderPane buttons_BorderPane;
    private Path filePath;
    private Thread calculateFilesNumberAndTotalSize_Thread;
    private ConfirmOperationDialog confirmOperationDialog;
    private Stage stage;
    private Thread deleteFiles_Thread;
    private DeleteFiles deleteFilesRunnable;

    private FM_GUI fm_gui;
    private File pathToProgram;


    public OpenWithCustomProgramDialog(final Path pathToFile)
    {
        filePath = pathToFile;

        initializeComponents();
    }

    private void initializeComponents()
    {
        root = new VBox(rem * 0.8D);
        root.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.MEDIUM, new Insets(rem * 0.4D))));
        root.setPadding(new Insets(rem * 0.8D));

        programName_Label = new Label("Name of program");
        programName_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.NORMAL, 10.0D));


        programName_TextField = new TextField();
        programName_TextField.setPromptText("Enter name of program here");
        programName_TextField.textProperty().addListener((event, oldValue, newValue) ->
        {
            nameProgramChanged_Listener(event, oldValue, newValue);
        });

        chooseProgram_Button = new Button("⇒");
        chooseProgram_Button.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 17.0D));
        chooseProgram_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        chooseProgram_Button.setOnAction(this::chooseProgramButton_Action);

        HBox programNameAndChooser_HBox = new HBox(rem * 0.5D, programName_TextField, chooseProgram_Button);
        programNameAndChooser_HBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(programName_TextField, Priority.ALWAYS);
        HBox.setHgrow(programNameAndChooser_HBox, Priority.ALWAYS);

        VBox programName_VBox = new VBox(programName_Label, programNameAndChooser_HBox);

        targetFile_Label = new Label("Open file '" + filePath.getFileName() + "' with program:");
        targetFile_Label.setWrapText(true);

        command_TextField = new TextField();
        command_TextField.setPromptText("Enter command to execute here");
        command_TextField.setVisible(false);

        customCommand_CheckBox = new CheckBox("Use custom command");
        customCommand_CheckBox.setOnAction(this::useCustomCommandCheckBox_Action);

        VBox customCommand_VBox = new VBox(rem * 0.45D,customCommand_CheckBox,
                command_TextField);
        customCommand_VBox.setPadding(new Insets(rem * 1.15D, 0.0D, 0.0D, 0.0D));

        execute_Button = new Button("Execute");
        execute_Button.setDisable(true);
        execute_Button.setOnAction(this::executeButton_Action);


        HBox buttons_HBox = new HBox(rem * 0.45D, execute_Button);
        buttons_HBox.setAlignment(Pos.BOTTOM_RIGHT);
        buttons_HBox.setFillHeight(true);

        buttons_BorderPane = new BorderPane();
        buttons_BorderPane.setBottom(buttons_HBox);

//        Label title_Label = new Label("Deleting");
//        title_Label.setFont(Font.font(title_Label.getFont().getName(),
//                FontWeight.EXTRA_BOLD, 18.0D));
//        //header_Label.setPadding(new Insets(0.0D, rem * 0.4D, 0.0D, 0.4D));
//        title_Label.setTextFill(Color.GHOSTWHITE);
//

        //root.setFillWidth(true);
        root.getChildren().addAll(targetFile_Label, programName_VBox,
                customCommand_VBox, buttons_BorderPane);

        scene = new Scene(root);

        stage = new Stage();
        stage.setMaxWidth(rem * 20.0D);
        stage.setScene(scene);
        stage.setTitle("Open with ...");
        stage.initStyle(StageStyle.DECORATED);
    }


    private void chooseProgramButton_Action(ActionEvent event)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select program");
        pathToProgram = fileChooser.showOpenDialog(null);

        if (pathToProgram != null)
        {
            execute_Button.setDisable(false);
            programName_TextField.setText(pathToProgram.toPath().getFileName().toString());
        }
    }

    private void executeButton_Action(ActionEvent event)
    {
        if (pathToProgram != null && !customCommand_CheckBox.isSelected())
        {
            ProcessBuilder processBuilder = new ProcessBuilder(pathToProgram.getAbsolutePath(),
                    filePath.toAbsolutePath().toString());
            try
            {
                Process process = processBuilder.start();

            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
        else if (programName_TextField.getText() != null &&
                !programName_TextField.getText().equals(""))
        {
            ProcessBuilder processBuilder = new ProcessBuilder(programName_TextField.getText(),
                    filePath.toAbsolutePath().toString());
            try
            {
                Process process = processBuilder.start();

            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
        else if (customCommand_CheckBox.isSelected() &&
                command_TextField.getText() != null &&
                !command_TextField.getText().equals(""))
        {
            System.out.println("Нету реализации.");
        }

        stage.hide();
    }

    public void show()
    {
        stage.show();
    }

    private void useCustomCommandCheckBox_Action(ActionEvent event)
    {
        if (customCommand_CheckBox.isSelected())
        {
            programName_TextField.setDisable(true);
            command_TextField.setVisible(true);
            chooseProgram_Button.setDisable(true);
        }
        else
        {
            programName_TextField.setDisable(false);
            chooseProgram_Button.setDisable(false);
            command_TextField.setVisible(false);

        }
    }

    private void nameProgramChanged_Listener(ObservableValue<? extends String> event, String oldValue, String newValue)
    {
        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder(programName_TextField.getText());
            processBuilder.redirectErrorStream();
            Process process = processBuilder.start();
            process.destroy();
            System.out.println("Скорее всего приложение существует.");
            execute_Button.setDisable(false);
        }
        catch (IOException ioException)
        {
            //ioException.printStackTrace();
            System.out.println("Не возможно выполнить программу." + ioException);
            execute_Button.setDisable(true);
            if (pathToProgram != null)
            {
                execute_Button.setDisable(false);
            }
        }
    }


}
