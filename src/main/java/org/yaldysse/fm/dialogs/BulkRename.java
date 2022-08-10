package org.yaldysse.fm.dialogs;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.yaldysse.fm.FileNameTip;
import org.yaldysse.fm.Utils;
import org.yaldysse.fm.javaFXgui;
import org.yaldysse.tools.StorageCapacity;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;

public class BulkRename
{
    private Path[] targetPaths;

    private Stage stage;
    private Scene scene;
    private VBox root;

    private HBox resultNameButtons_HBox;
    private FlowPane availableNameButtons_FlowPane;
    private Label availableModifiers_Label;
    private Label nameModifiers_Label;
    private Button rename_Button;
    private Button cancel_Button;

    private VBox availableModifiers_Pane;
    private HBox availableModifiersLabel_HBox;
    public final Properties language;
    private BorderPane buttons_BorderPane;
    private VBox resultName_Pane;
    private ScrollPane nameModifiers_ScrollPane;
    private int numberModifiers_count;
    private Scene result_Scene;
    private Label preview_Label;
    private VBox previewNames_VBox;
    private VBox preview_Pane;
    private TitledPane errorLog_TitledPane;
    private ScrollPane errorLog_ScrollPane;
    private Label success_Label;
    private Button back_Button;
    private boolean haveErrors;
    private int existsIndex;

    private ArrayList<NameModifier> selectedModifiers_ArrayList;
    private ArrayList<Spinner<Integer>> numberCustomValueSpinners;


    public BulkRename(final Path[] paths, final Properties languageProperties)
    {
        targetPaths = paths;
        language = languageProperties;
        numberModifiers_count = 0;
        existsIndex = 1;
        numberCustomValueSpinners = new ArrayList<>();

        initializeComponents();
    }

    private void initializeComponents()
    {
        selectedModifiers_ArrayList = new ArrayList<>();
        stage = new Stage();
        stage.setTitle("Bulk Rename");

        root = new VBox(javaFXgui.rem * 1.85D);
        root.setPadding(new Insets(javaFXgui.rem * 0.7D));
        root.setFillWidth(true);

        availableModifiers_Label = new Label(language.getProperty("availableModifiers_label",
                "Available Modifiers"));
        availableModifiers_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL,
                11.0D));

        nameModifiers_Label = new Label(language.getProperty("nameModifiers_str",
                "Name modifiers"));
        nameModifiers_Label.setFont(availableModifiers_Label.getFont());
        HBox nameModifiersLabel_HBox = new HBox(nameModifiers_Label);
        nameModifiersLabel_HBox.setAlignment(Pos.CENTER_LEFT);

        availableModifiersLabel_HBox = new HBox(availableModifiers_Label);
        availableModifiersLabel_HBox.setAlignment(Pos.CENTER_RIGHT);

        availableNameButtons_FlowPane = new FlowPane(javaFXgui.rem * 0.35D,
                javaFXgui.rem * 0.3D);
        availableNameButtons_FlowPane.setPadding(new Insets(javaFXgui.rem * 0.2D));
        availableNameButtons_FlowPane.setBorder(new Border(new BorderStroke(
                Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                BorderStroke.THIN)));

        initializeModifierButtons();

        availableModifiers_Pane = new VBox(availableModifiersLabel_HBox,
                availableNameButtons_FlowPane);

        //------------------ Итоговое имя

        resultNameButtons_HBox = new HBox(javaFXgui.rem * 0.4D);
        //resultNameButtons_HBox.setFillHeight(true);

        nameModifiers_ScrollPane = new ScrollPane();
        //nameModifiers_ScrollPane.setFitToWidth(true);
        //nameModifiers_ScrollPane.setFitToHeight(true);
        nameModifiers_ScrollPane.setContent(resultNameButtons_HBox);
        nameModifiers_ScrollPane.setPadding(new Insets(javaFXgui.rem * 0.3D));
        nameModifiers_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        nameModifiers_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        nameModifiers_ScrollPane.prefViewportHeightProperty().bind(resultNameButtons_HBox.heightProperty());

        resultName_Pane = new VBox(nameModifiersLabel_HBox, nameModifiers_ScrollPane);
        resultName_Pane.setFillWidth(true);

        //VBox.setVgrow(nameModifiers_ScrollPane,Priority.ALWAYS);

        //------------------ Превью
        preview_Label = new Label("Preview");
        preview_Label.setFont(availableModifiers_Label.getFont());

        HBox previewLabel_HBox = new HBox(preview_Label);
        previewLabel_HBox.setAlignment(Pos.CENTER_LEFT);


        preview_Pane = new VBox(preview_Label);
        preview_Pane.setFillWidth(true);

        //------------------ Нижние кнопки

        rename_Button = new Button(language.getProperty("rename_button",
                "Rename"));
        rename_Button.setOnAction(this::renameButton_Action);

        cancel_Button = new Button(language.getProperty("stopOperation_button",
                "Cancel"));
        cancel_Button.setOnAction(this::cancelButton_Action);

        back_Button = new Button(language.getProperty("back_button",
                "Back"));
        back_Button.setOnAction(this::backButton_Action);
        back_Button.setVisible(false);

        HBox bottomButtons_HBox = new HBox(javaFXgui.rem * 1.4D, cancel_Button,
                rename_Button, back_Button);

        buttons_BorderPane = new BorderPane();
        buttons_BorderPane.setBottom(bottomButtons_HBox);

        Label firstExample_Label = new Label();
        Label secondExample_Label = new Label();

        firstExample_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.NORMAL, 12.0D));
        secondExample_Label.setFont(firstExample_Label.getFont());

        previewNames_VBox = new VBox(javaFXgui.rem * 0.3, firstExample_Label,
                secondExample_Label);
        previewNames_VBox.setPadding(new Insets(javaFXgui.rem * 0.2D));
        previewNames_VBox.setBorder(new Border(new BorderStroke(
                Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                BorderStroke.THIN)));

        preview_Pane.getChildren().addAll(previewLabel_HBox, previewNames_VBox);
        preview_Pane.setBorder(nameModifiers_ScrollPane.getBorder());

        root.getChildren().addAll(availableModifiers_Pane, resultName_Pane,
                preview_Pane, buttons_BorderPane);

        scene = new Scene(root);
        stage.setScene(scene);

        initializeResultScene();
    }

    private void initializeResultScene()
    {
        success_Label = new Label(language.getProperty("success_str",
                "Rename has been successfully completed."));
        success_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD,
                17.0D));
        success_Label.setTextFill(Color.WHITESMOKE);
        success_Label.setEffect(new DropShadow(javaFXgui.rem * 0.1D, Color.LIGHTGRAY));

        VBox result_root_VBox = new VBox();
        //result_VBox.setAlignment(Pos.CENTER);
        result_root_VBox.setFillWidth(true);
        result_root_VBox.setPadding(root.getPadding());

        errorLog_ScrollPane = new ScrollPane();
        errorLog_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        errorLog_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        errorLog_ScrollPane.setFitToWidth(true);
        errorLog_ScrollPane.setVisible(false);

        result_root_VBox.getChildren().addAll(success_Label, errorLog_ScrollPane);
        result_root_VBox.setBackground(new Background(new BackgroundFill(
                Color.MEDIUMSEAGREEN, CornerRadii.EMPTY, Insets.EMPTY)));

        result_Scene = new Scene(result_root_VBox);
    }

    private void initializeModifierButtons()
    {
        Button generalName_Button = new Button(language.getProperty("generalName_modifierButton",
                "Name"));
        generalName_Button.setOnMouseClicked(event ->
        {
            if (event.getClickCount() >= 2)
            {
                selectModifier(NameModifier.NAME);
            }
        });


        Button numberModifier_Button = new Button(language.getProperty("",
                "Number"));
        numberModifier_Button.setOnMouseClicked(event ->
        {
            if (event.getClickCount() >= 2)
            {
                selectModifier(NameModifier.NUMBER);
            }
        });

        ComboBox<String> date_ComboBox = new ComboBox<>();
        date_ComboBox.getItems().addAll("Date (ISO)", "Day (Number)",
                "Day of year (Number)", "Month (Number)",
                "Year (Number)");
        date_ComboBox.setEditable(false);
        date_ComboBox.getSelectionModel().select(0);
        date_ComboBox.setOnMouseClicked(event ->
        {
            if (event.getClickCount() >= 2)
            {
                NameModifier temporaryModifier = NameModifier.DATE;
                int selectedIndex = date_ComboBox.getSelectionModel().getSelectedIndex();
                if (selectedIndex == 1)
                {
                    temporaryModifier = NameModifier.DAY_OF_MONTH;
                }
                else if (selectedIndex == 2)
                {
                    temporaryModifier = NameModifier.DAY_OF_YEAR;
                }
                else if (selectedIndex == 3)
                {
                    temporaryModifier = NameModifier.MONTH_OF_YEAR;

                }
                else if (selectedIndex == 4)
                {
                    temporaryModifier = NameModifier.YEAR;
                }

                selectModifier(temporaryModifier);
            }
        });
        date_ComboBox.getSelectionModel().selectedIndexProperty().addListener(event ->
        {
            javaFXgui.fitRegionWidthToText(date_ComboBox, date_ComboBox.getSelectionModel()
                    .getSelectedItem() + "_________", date_ComboBox.getEditor().getFont());
        });
        javaFXgui.fitRegionWidthToText(date_ComboBox, date_ComboBox.getSelectionModel()
                .getSelectedItem() + "_________", date_ComboBox.getEditor().getFont());

        ComboBox<String> time_ComboBox = new ComboBox<>();
        time_ComboBox.getItems().addAll("Time (ISO)", "Hour (Number)",
                "Minute (Number)", "Second (Number)",
                "Nanosecond (Number)");
        time_ComboBox.setEditable(false);
        time_ComboBox.getSelectionModel().select(0);
        time_ComboBox.setOnMouseClicked(event ->
        {
            if (event.getClickCount() >= 2)
            {
                NameModifier temporaryModifier = NameModifier.TIME;
                int selectedIndex = time_ComboBox.getSelectionModel().getSelectedIndex();
                if (selectedIndex == 1)
                {
                    temporaryModifier = NameModifier.HOUR;
                }
                else if (selectedIndex == 2)
                {
                    temporaryModifier = NameModifier.MINUTE;
                }
                else if (selectedIndex == 3)
                {
                    temporaryModifier = NameModifier.SECOND;

                }
                else if (selectedIndex == 4)
                {
                    temporaryModifier = NameModifier.NANOSECOND;
                }

                selectModifier(temporaryModifier);
            }
        });
        time_ComboBox.getSelectionModel().selectedIndexProperty().addListener(event ->
        {
            javaFXgui.fitRegionWidthToText(time_ComboBox, time_ComboBox.getSelectionModel()
                    .getSelectedItem() + "_________", time_ComboBox.getEditor().getFont());
        });
        javaFXgui.fitRegionWidthToText(time_ComboBox, time_ComboBox.getSelectionModel()
                        .getSelectedItem() + "_________",
                time_ComboBox.getEditor().getFont());

        availableNameButtons_FlowPane.getChildren().addAll(generalName_Button,
                date_ComboBox, time_ComboBox, numberModifier_Button);
    }


    private void selectModifier(NameModifier modifier)
    {
        selectedModifiers_ArrayList.add(modifier);

        if (modifier == NameModifier.NAME)
        {
            TextField textField = createNameTextField();
            resultNameButtons_HBox.getChildren().add(textField);
        }
        else
        {
            Spinner<Integer> temporarySpinner = null;

            Button temporaryButton = new Button(modifier.name());
            temporaryButton.setMinWidth(new Text(temporaryButton.getText() + "__")
                    .getBoundsInParent().getWidth());

            resultNameButtons_HBox.getChildren().add(temporaryButton);
            HBox.setHgrow(temporaryButton, Priority.ALWAYS);

            if (modifier == NameModifier.NUMBER)
            {
                temporaryButton.setTooltip(new FileNameTip(language.getProperty("toConfigurePressLMB_str",
                        "To configure press left mouse button.")));
                numberModifiers_count++;

                Spinner<Integer> tempSpinner = new Spinner<>(0, (int) StorageCapacity.MEBI_BYTE_VALUE,
                        0);
                tempSpinner.setEditable(true);
                tempSpinner.setOnMouseExited(event ->
                {
                    int temporaryIndex = resultNameButtons_HBox.getChildren().indexOf(tempSpinner);
                    resultNameButtons_HBox.getChildren().remove(temporaryIndex);
                    resultNameButtons_HBox.getChildren().add(temporaryIndex, temporaryButton);
                });
                tempSpinner.setOnMouseClicked(event ->
                {
                    if (event.getButton() == MouseButton.SECONDARY &&
                            event.getClickCount() == 1)
                    {
                        int temporaryIndex = resultNameButtons_HBox.getChildren().indexOf(tempSpinner);
                        unselectModifier(temporaryIndex);
                    }
                });

                Spinner<Integer> finalTemporarySpinner1 = tempSpinner;
                tempSpinner.valueProperty().addListener(event ->
                {
                    javaFXgui.fitRegionWidthToText(finalTemporarySpinner1, "_________" + tempSpinner.getValue(),
                            finalTemporarySpinner1.getEditor().getFont());
                    updatePreview();
                });
                javaFXgui.fitRegionWidthToText(finalTemporarySpinner1, "_________" + tempSpinner.getValue(),
                        finalTemporarySpinner1.getEditor().getFont());
                numberCustomValueSpinners.add(tempSpinner);
                temporarySpinner = tempSpinner;
            }

            Spinner<Integer> finalTemporarySpinner = temporarySpinner;
            temporaryButton.setOnMouseClicked(event ->
            {
                if (event.getClickCount() == 1 && event.getButton() == MouseButton.PRIMARY)
                {
                    int temporaryIndex = resultNameButtons_HBox.getChildren().indexOf(temporaryButton);
                    resultNameButtons_HBox.getChildren().remove(temporaryIndex);
                    resultNameButtons_HBox.getChildren().add(temporaryIndex, finalTemporarySpinner);
                }
                else if (event.getButton() == MouseButton.SECONDARY &&
                        event.getClickCount() == 1)
                {
                    unselectModifier(resultNameButtons_HBox.getChildren().indexOf(temporaryButton));
                }
            });
        }

        updatePreview();
    }

    private void unselectModifier(final int index)
    {
        if (selectedModifiers_ArrayList.get(index) == NameModifier.NUMBER)
        {
            removeNumberSpinnerByModifierNodeIndex(index);
        }

        selectedModifiers_ArrayList.remove(index);
        resultNameButtons_HBox.getChildren().remove(index);
        updatePreview();
    }

    private void renameButton_Action(ActionEvent event)
    {
        existsIndex = 1;
        StringBuilder newName = new StringBuilder();
        NameModifier temporaryModifier = null;
        int[] numberModifiersValues = new int[numberModifiers_count];
        int numberModifierIndex = 0;
        int numberCustomValueSpinnerIndex = 0;
        Path sourcePath = null;

        for (int j = 0; j < targetPaths.length; j++)
        //for (Path sourcePath : targetPaths)
        {
            sourcePath = targetPaths[j];
            newName.delete(0, newName.length());
            numberCustomValueSpinnerIndex = 0;
            numberModifierIndex = 0;

            for (int k = 0; k < selectedModifiers_ArrayList.size(); k++)
            {
                temporaryModifier = selectedModifiers_ArrayList.get(k);
                if (temporaryModifier == NameModifier.NAME)
                {
                    TextField tt = (TextField) resultNameButtons_HBox.getChildren().get(k);
                    newName.append(tt.getText());
                }
                else if (temporaryModifier == NameModifier.TIME)
                {
                    newName.append(LocalTime.now().format(DateTimeFormatter.ISO_TIME));
                }
                else if (temporaryModifier == NameModifier.DATE)
                {
                    newName.append(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                }
                else if (temporaryModifier == NameModifier.NUMBER)
                {
                    if (j == 0)
                    {
                        numberModifiersValues[numberModifierIndex] =
                                numberCustomValueSpinners.get(numberCustomValueSpinnerIndex++).getValue();
                    }
                    numberModifiersValues[numberModifierIndex++] += 1;

                    //numberModifiersValues[numberModifierIndex++] += 1;
                    newName.append(numberModifiersValues[numberModifierIndex - 1]);
                }
                else if (temporaryModifier == NameModifier.DAY_OF_WEEK)
                {
                    newName.append(LocalDate.now().getDayOfWeek().getValue());
                }
                else if (temporaryModifier == NameModifier.DAY_OF_MONTH)
                {
                    newName.append(LocalDate.now().getDayOfMonth());
                }
                else if (temporaryModifier == NameModifier.DAY_OF_YEAR)
                {
                    newName.append(LocalDate.now().getDayOfYear());
                }
                else if (temporaryModifier == NameModifier.MONTH_OF_YEAR)
                {
                    newName.append(LocalDate.now().getMonthValue());
                }
                else if (temporaryModifier == NameModifier.YEAR)
                {
                    newName.append(LocalDate.now().getYear());
                }
                else if (temporaryModifier == NameModifier.HOUR)
                {
                    newName.append(LocalTime.now().getHour());
                }
                else if (temporaryModifier == NameModifier.MINUTE)
                {
                    newName.append(LocalTime.now().getMinute());
                }
                else if (temporaryModifier == NameModifier.SECOND)
                {
                    newName.append(LocalTime.now().getSecond());
                }
                else if (temporaryModifier == NameModifier.NANOSECOND)
                {
                    newName.append(LocalTime.now().getNano());
                }
            }

            String baseName = newName.toString();
            newName.append(".");
            newName.append(Utils.getExtension(sourcePath).toLowerCase());

            try
            {
                Files.move(sourcePath, sourcePath.getParent().resolve(newName.toString()));
            }
            catch (FileAlreadyExistsException fileAlreadyExistsException)
            {
                System.out.println("Файл с таким именем уже существует. Добавляем в имя.");
                renameWithUniqueName(sourcePath, baseName);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }

        }

        stage.setScene(result_Scene);
    }


    /**
     * Переименовывает указанный файл используя базовое имя.
     * К базовому имени добавляется номер и точка во времени в наносекундах.
     */
    private void renameWithUniqueName(final Path path, final String baseName)
    {
        try
        {
            Files.move(path, path.getParent().resolve(baseName + "_exists_" + existsIndex + "-"
                    + LocalTime.now().getNano() + "." + Utils.getExtension(path).toLowerCase()));
            existsIndex++;
        }
        catch (IOException fileAlreadyExistsException)
        {
            fileAlreadyExistsException.printStackTrace();
        }
    }

    private void updatePreview()
    {
        StringBuilder newName = new StringBuilder();
        NameModifier temporaryModifier = null;
        int[] numberModifiersValues = new int[numberModifiers_count];
        int numberModifierIndex = 0;
        int numberCustomValueSpinnerIndex = 0;
        Label temporaryLabel = null;

        for (int j = 0; j < previewNames_VBox.getChildren().size(); j++)
        {
            newName.delete(0, newName.length());
            numberModifierIndex = 0;
            numberCustomValueSpinnerIndex = 0;

            for (int k = 0; k < selectedModifiers_ArrayList.size(); k++)
            {
                temporaryModifier = selectedModifiers_ArrayList.get(k);
                if (temporaryModifier == NameModifier.NAME)
                {
                    TextField tt = (TextField) resultNameButtons_HBox.getChildren().get(k);
                    newName.append(tt.getText());
                }
                else if (temporaryModifier == NameModifier.TIME)
                {
                    newName.append(LocalTime.now().format(DateTimeFormatter.ISO_TIME));
                }
                else if (temporaryModifier == NameModifier.DATE)
                {
                    newName.append(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                }
                else if (temporaryModifier == NameModifier.NUMBER)
                {
                    if (j == 0)
                    {
                        numberModifiersValues[numberModifierIndex] =
                                numberCustomValueSpinners.get(numberCustomValueSpinnerIndex++).getValue();
                    }
                    numberModifiersValues[numberModifierIndex++] += 1;

                    //numberModifiersValues[numberModifierIndex++] += 1;
                    newName.append(numberModifiersValues[numberModifierIndex - 1]);
                }
                else if (temporaryModifier == NameModifier.DAY_OF_WEEK)
                {
                    newName.append(LocalDate.now().getDayOfWeek().getValue());
                }
                else if (temporaryModifier == NameModifier.DAY_OF_MONTH)
                {
                    newName.append(LocalDate.now().getDayOfMonth());
                }
                else if (temporaryModifier == NameModifier.DAY_OF_YEAR)
                {
                    newName.append(LocalDate.now().getDayOfYear());
                }
                else if (temporaryModifier == NameModifier.MONTH_OF_YEAR)
                {
                    newName.append(LocalDate.now().getMonthValue());
                }
                else if (temporaryModifier == NameModifier.HOUR)
                {
                    newName.append(LocalTime.now().getHour());
                }
                else if (temporaryModifier == NameModifier.MINUTE)
                {
                    newName.append(LocalTime.now().getMinute());
                }
                else if (temporaryModifier == NameModifier.SECOND)
                {
                    newName.append(LocalTime.now().getSecond());
                }
                else if (temporaryModifier == NameModifier.NANOSECOND)
                {
                    newName.append(LocalTime.now().getNano());
                }

            }

            temporaryLabel = (Label) previewNames_VBox.getChildren().get(j);
            temporaryLabel.setText(newName.toString());
        }
    }

    private void removeNumberSpinnerByModifierNodeIndex(final int indexInHBox)
    {
        int indexNumberSpinner = -1;
        for (int k = 0; k < resultNameButtons_HBox.getChildren().size(); k++)
        {
            if (selectedModifiers_ArrayList.get(k) == NameModifier.NUMBER)
            {
                indexNumberSpinner++;
                if (k == indexInHBox && indexNumberSpinner > -1)
                {
                    numberCustomValueSpinners.remove(indexNumberSpinner);
                }
            }
        }
    }

    private TextField createNameTextField()
    {
        TextField textField = new TextField();

        Text temporaryText = new Text("");
        temporaryText.setFont(textField.getFont());

        textField.textProperty().addListener((event, oldValue, newValue) ->
        {
            temporaryText.setText(textField.getText() + "____");
            textField.setPrefWidth(temporaryText.getBoundsInParent().getWidth());
            updatePreview();
        });
        textField.setPromptText("Enter name");
        textField.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.SECONDARY &&
                    event.getClickCount() == 1)
            {
                int index = resultNameButtons_HBox.getChildren().indexOf(textField);
                unselectModifier(index);
            }
        });
        temporaryText.setText(textField.getText() + "____");
        textField.setPrefWidth(temporaryText.getBoundsInParent().getWidth());
        return textField;
    }

    public void show()
    {
        selectedModifiers_ArrayList.clear();
        resultNameButtons_HBox.getChildren().clear();
        stage.show();
    }

    private void cancelButton_Action(ActionEvent event)
    {
        stage.show();
    }

    private void backButton_Action(ActionEvent event)
    {
        stage.setScene(scene);
    }

}

enum NameModifier
{
    NUMBER, TIME, DATE, NAME, DAY_OF_WEEK, DAY_OF_MONTH, MONTH_OF_YEAR, YEAR,
    DAY_OF_YEAR, HOUR, MINUTE, SECOND, NANOSECOND
}
