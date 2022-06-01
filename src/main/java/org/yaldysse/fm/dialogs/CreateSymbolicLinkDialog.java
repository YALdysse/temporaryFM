package org.yaldysse.fm.dialogs;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.yaldysse.fm.dialogs.ConfirmDialogButtonType;
import org.yaldysse.fm.ConfirmOperationButton;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Описывает компонент программы, отвечающий за создание символической ссылки.
 * Построен с использованием класса {@link ConfirmOperationDialog}, который описывает интерфейс GUI для некоторых
 * операций с файлами.
 *
 * @see ConfirmOperationDialog
 */
public class CreateSymbolicLinkDialog
{
    public static final double rem = new Text("").getBoundsInParent().getHeight();
    private Path targetLinkPath;
    private Path linkLocationPath;

    private ConfirmOperationDialog confirmOperationDialog;
    private TextField linkPath_TextField;
    private TextField targetPath_TextField;
    private FileChooser target_FileChooser;
    private FileChooser linkLocation_FileChooser;

    public CreateSymbolicLinkDialog(final Path aTargetLinkPath, final Path aLinkLocationPath)
    {
        targetLinkPath = aTargetLinkPath;
        linkLocationPath = aLinkLocationPath;
        initializeComponents();
    }

    public CreateSymbolicLinkDialog()
    {
        initializeComponents();
    }

    private void initializeComponents()
    {
        VBox fileProperties_VBox = new VBox(rem * 1.2D);
        fileProperties_VBox.setAlignment(Pos.CENTER);
        fileProperties_VBox.setPadding(new Insets(rem * 0.25D));
//        fileName_VBox.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN,
//                CornerRadii.EMPTY, Insets.EMPTY)));

        //---------------------------------------------------
        Label targetPath_Label = new Label("Target path");
        targetPath_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL, 11.0D));

        Label linkPath_Label = new Label("Link path (Location of link)");
        linkPath_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL, 11.0D));


        targetPath_TextField = new TextField();
        targetPath_TextField.setPromptText("Enter target of link here");
        if (targetLinkPath != null)
        {
            targetPath_TextField.setText(targetLinkPath.toAbsolutePath().toString());

//            Platform.runLater(() ->
//            {
//                    targetPath_TextField.positionCaret(targetPath_TextField.getText().length()-1);
//            });
        }


        linkPath_TextField = new TextField();
        linkPath_TextField.setPromptText("Enter location of link here");

        /*Ссылка на кнопку продолжения действия. Используется для предотвращения
         * указания пустого пути.*/
        ConfirmOperationButton[] ok_Button = new ConfirmOperationButton[1];

        Button chooseTargetPath_Button = new Button("»");
        chooseTargetPath_Button.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 16.0D));
        chooseTargetPath_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        chooseTargetPath_Button.setOnAction(this::chooseTargetPath_ButtonAction);

        Button chooseLinkPath_Button = new Button("»");
        chooseLinkPath_Button.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 16.0D));
        chooseLinkPath_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        chooseLinkPath_Button.setOnAction(this::chooseLinkPath_ButtonAction);

        ConfirmOperationButton[] finalOk_Button = ok_Button;
        linkPath_TextField.textProperty().addListener((eventText, oldText, newText) ->
        {
            if (newText == null || newText.equals(""))
            {
                finalOk_Button[0].setDisable(true);
            }
            else
            {
                finalOk_Button[0].setDisable(false);
            }
        });

        targetPath_TextField.textProperty().addListener((eventText, oldText, newText) ->
        {
            if (newText == null || newText.equals(""))
            {
                finalOk_Button[0].setDisable(true);
            }
            else
            {
                finalOk_Button[0].setDisable(false);
            }
            targetPath_TextField.end();
        });


        HBox targetPath_HBox = new HBox(rem * 0.35D, targetPath_TextField, chooseTargetPath_Button);
        HBox linkPath_HBox = new HBox(rem * 0.35D, linkPath_TextField, chooseLinkPath_Button);

        HBox.setHgrow(targetPath_TextField, Priority.ALWAYS);
        HBox.setHgrow(linkPath_TextField, Priority.ALWAYS);


        VBox targetPath_VBox = new VBox(rem * 0.1D, targetPath_Label, targetPath_HBox);
        VBox linkPath_VBox = new VBox(rem * 0.1D, linkPath_Label, linkPath_HBox);

//        fileProperties_VBox.getChildren().addAll(fileName_TextField, fileAlreadyExists_Label,
//                accordion);
        fileProperties_VBox.getChildren().addAll(targetPath_VBox, linkPath_VBox);

        target_FileChooser = new FileChooser();
        target_FileChooser.setTitle("Select target of symbolic link");
        target_FileChooser.setInitialDirectory(targetLinkPath.toFile());

        linkLocation_FileChooser = new FileChooser();
        linkLocation_FileChooser.setTitle("Select location of symbolic link");
        linkLocation_FileChooser.setInitialDirectory(targetLinkPath.toFile());


        confirmOperationDialog = new ConfirmOperationDialog(StageStyle.UTILITY,
                "");
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        //confirmOperationDialog.initOwner(stage);
        confirmOperationDialog.initModality(Modality.APPLICATION_MODAL);
        confirmOperationDialog.setTitle("Create Symbolic link");
        confirmOperationDialog.setHeaderText("Create Symbolic link");
        confirmOperationDialog.setHeaderColor(Color.PALEVIOLETRED);
        confirmOperationDialog.setMessageText("Enter a name of directory below.");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(fileProperties_VBox);
        confirmOperationDialog.setConfirmOperationOnEnterKey(true);
        confirmOperationDialog.setBackgroundToRootNode(new Background(new BackgroundFill(Color.PINK, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMinHeight(confirmOperationDialog.rem * 16);
        confirmOperationDialog.setMaxHeight(confirmOperationDialog.rem * 25);

        ObservableList<Node> nodes = confirmOperationDialog.getOperationButtons();
        for (int k = 0; k < nodes.size(); k++)
        {
            ConfirmOperationButton temporaryConfirmOperationButton = (ConfirmOperationButton) nodes.get(k);
            temporaryConfirmOperationButton.setBackground(Background.EMPTY);
            temporaryConfirmOperationButton.setBorder(new Border(new BorderStroke(Color.BLACK,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.MEDIUM, Insets.EMPTY)));
            if (temporaryConfirmOperationButton.getButtonType() == ConfirmDialogButtonType.OK)
            {
                finalOk_Button[0] = temporaryConfirmOperationButton;
                ok_Button[0].setDisable(true);
            }
        }
    }

    public Path showAndWait()
    {
        return showDialog();
    }


    /**
     * @param aLinkLocationPath Не используется.
     */
    public Path showAndWait(final Path aTargetLinkPath, final Path aLinkLocationPath)
    {
        targetPath_TextField.setText(aTargetLinkPath.toAbsolutePath().toString());
        targetPath_TextField.end();

        target_FileChooser.setInitialDirectory(aTargetLinkPath.getParent().toFile());
        linkLocation_FileChooser.setInitialDirectory(aTargetLinkPath.getParent().toFile());
        linkPath_TextField.setText("");
        return showDialog();
    }

    private Path showDialog()
    {
        Path resultPath = null;
        while (true)
        {
            confirmOperationDialog.showAndWait();

            System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

            if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
            {
                try
                {
                    Path temporaryTargetLinkPath = Paths.get(targetPath_TextField.getText());
                    Path temporaryLinkLocationPath = Paths.get(linkPath_TextField.getText());

                    if (!temporaryLinkLocationPath.isAbsolute())
                    {
                        //Создать в текущем каталоге
                        temporaryLinkLocationPath = temporaryTargetLinkPath.getParent().resolve(
                                linkPath_TextField.getText());
                        System.out.println("Абсолютный путь не указан. Используэм текущий: "
                                + temporaryLinkLocationPath.toAbsolutePath().toString());
                    }

                    resultPath = Files.createSymbolicLink(temporaryLinkLocationPath,
                            temporaryTargetLinkPath);

                    if (resultPath != null && Files.exists(resultPath))
                    {
                        System.out.println("Символическая ссылка должна быть создана.");
                        return resultPath;
                    }
                }
                catch (FileAlreadyExistsException fileAlreadyExistsException)
                {
                    //fileAlreadyExistsException.printStackTrace();
                    System.out.println("Файл с таким именем уже существует.");
                }
//                catch (NoSuchFileException noSuchFileException)
//                {
//                    //fileAlreadyExistsException.printStackTrace();
//                    System.out.println("Обычно эта ошибка означает, что имя недопустимое.");
//                    break;
//                }
                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }
            else if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.CANCEL)
            {
                break;
            }
        }
        return null;
    }


    private void chooseLinkPath_ButtonAction(ActionEvent event)
    {
        File targetFile = null;
        if ((targetFile = linkLocation_FileChooser.showSaveDialog(null)) != null)
        {
            linkPath_TextField.setText(targetFile.getAbsolutePath());
        }
    }

    private void chooseTargetPath_ButtonAction(ActionEvent event)
    {
        File targetFile = null;
        if ((targetFile = target_FileChooser.showOpenDialog(null)) != null)
        {
            targetPath_TextField.setText(targetFile.getAbsolutePath());
        }
    }
}
