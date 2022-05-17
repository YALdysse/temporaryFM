package org.yaldysse.fm;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class FileAttributesEditor
{
    private Path targetFile_Path;

    private Stage stage;
    private Scene scene;
    private VBox root;
    private TabPane info_edit_TabPane;
    private GridPane attributes_GridPane;
    private VBox info_VBox;
    private VBox edit_VBox;
    private GridPane permissionSelector_GridPane;
    private CheckBox[][] permissionsCheckBoxes;
    private Button apply_Button;
    private ScrollPane edit_ScrollPane;
    private ScrollPane info_ScrollPane;
    private DatePicker creationTime_Picker;
    private TextField creationTime_TextField;
    private DatePicker lastModifiedDate_Picker;
    private TextField lastModifiedTime_TextField;
    private int preferredWidth;
    private int preferredHeight;
    private UserPrincipal[] newUserPrincipal;
    private GroupPrincipal[] newGroupPrincipal;
    private HBox permissionTitle_HBox;
    private Popup warning_Popup;
    private HBox editOwner_HBox;

    private ComboBox<String> attributesNames_ComboBox;
    private TextField extendedAttributeValue_TextField;

    private Label permission_Label;

    private VBox ownerEditorPane_VBox;
    private VBox groupEditorPane_VBox;
    private VBox creationTimeEditorPane_VBox;
    private VBox lastModifiedTimeEditorPane_VBox;
    private VBox extendedAttributesEditorPane_VBox;

    private CheckBox editOwner_CheckBox;
    private CheckBox editGroup_CheckBox;
    private CheckBox editCreationTime_CheckBox;
    private CheckBox editLastModifiedTime_CheckBox;
    private CheckBox editPermissions_CheckBox;
    private CheckBox editExtendedAttributes_CheckBox;

    private Label ownerAttribute_Label;
    private Label groupAttribute_Label;
    private Label sizeBytesAttribute_Label;
    private Label creationTimeAttribute_Label;
    private Label lastModifiedTimeAttribute_Label;

    private Label ownerAttributeValue_Label;
    private Label groupAttributeValue_Label;
    private Label sizeBytesAttributeValue_Label;
    private Label creationTimeAttributeValue_Label;
    private Label lastModifiedTimeAttributeValue_Label;

    private Separator firstSeparator;
    private Separator secondSeparator;

    private VBox ownerPermissionsBlock_VBox;
    private VBox groupPermissionsBlock_VBox;
    private VBox otherPermissionsBlock_VBox;

    private Label ownerPermissionsBlock_Label;
    private Label groupPermissionsBlock_Label;
    private Label otherPermissionsBlock_Label;

    /**
     * Предназначены для хранения HBox под индикаторы.
     * Первый элемент для владельца,
     * второй - для группы,
     * третий - для остальных
     */
    private HBox[] readPermissionsIndicator_HBox;
    private HBox[] writePermissionsIndicator_HBox;
    private HBox[] executePermissionsIndicator_HBox;

    private Label[] readPermissionsIndicator;
    private Label[] writePermissionsIndicator;
    private Label[] executePermissionsIndicator;

    private ImageView[] readPermissionsIndicator_ImageView;
    private ImageView[] writePermissionsIndicator_ImageView;
    private ImageView[] executePermissionsIndicator_ImageView;

    private Label[] extendedAttributesNames_Label;
    private Label[] extendedAttributesValues_Label;

    private Label deleteExtendedAttribute_Label;

    public FileAttributesEditor(final Path file)
    {
        targetFile_Path = file;
        initializeComponents();
    }


    private void initializeComponents()
    {
        stage = new Stage();

        root = new VBox(FM_GUI.rem * 0.15D);
        root.setPadding(new Insets(FM_GUI.rem * 0.85D));
        root.setOnKeyReleased(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE)
            {
                stage.hide();
            }
        });

        preferredWidth = (int) (FM_GUI.rem * 18.0D);
        preferredHeight = (int) (FM_GUI.rem * 21.0D);

        info_edit_TabPane = new TabPane();

        Tab infoAboutFile_Tab = new Tab("Info");
        infoAboutFile_Tab.setClosable(false);
        infoAboutFile_Tab.selectedProperty().addListener(event ->
        {
            if (infoAboutFile_Tab.isSelected())
            {
                System.out.println("Информация.");
                info_edit_TabPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE,
                        CornerRadii.EMPTY, Insets.EMPTY)));

                if (info_ScrollPane != null)
                {
                    info_ScrollPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE,
                            CornerRadii.EMPTY, Insets.EMPTY)));
                    updateInfoTabContent();

                }
                root.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE,
                        CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });

        Tab editAttributes_Tab = new Tab("Edit");
        editAttributes_Tab.setClosable(false);
        editAttributes_Tab.selectedProperty().addListener(event ->
        {
            if (editAttributes_Tab.isSelected())
            {
                System.out.println("Редактирование.");
                edit_VBox.setBackground(new Background(new BackgroundFill(Color.MEDIUMPURPLE,
                        CornerRadii.EMPTY, Insets.EMPTY)));
                info_edit_TabPane.setBackground(new Background(new BackgroundFill(Color.MEDIUMPURPLE,
                        CornerRadii.EMPTY, Insets.EMPTY)));
                root.setBackground(new Background(new BackgroundFill(Color.MEDIUMPURPLE,
                        CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });

        info_edit_TabPane.getTabs().addAll(infoAboutFile_Tab, editAttributes_Tab);

        initializeInfoTab();
        initializeEditTab();

        infoAboutFile_Tab.setContent(info_ScrollPane);
        editAttributes_Tab.setContent(edit_ScrollPane);

        root.getChildren().add(info_edit_TabPane);

        scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(preferredWidth);
        stage.setHeight(preferredHeight);
        stage.setMinWidth(preferredWidth - 2);
        stage.setMinHeight(preferredHeight - 2);
    }

    private void initializeInfoTab()
    {
        Label fileName_Label = new Label(targetFile_Path.getFileName().toString());
        fileName_Label.setFont(Font.font(fileName_Label.getFont().getName(), FontWeight.BOLD, 20.0D));

        attributes_GridPane = new GridPane();
        attributes_GridPane.setHgap(FM_GUI.rem * 0.56D);
        //attributes_GridPane.setGridLinesVisible(true);
        attributes_GridPane.setVgap(FM_GUI.rem * 0.35D);

        ownerAttribute_Label = new Label("Owner:");
        groupAttribute_Label = new Label("Group:");
        sizeBytesAttribute_Label = new Label("Size (bytes):");
        creationTimeAttribute_Label = new Label("Creation time:");
        lastModifiedTimeAttribute_Label = new Label("Last Modified time:");

        firstSeparator = new Separator(Orientation.HORIZONTAL);

        ownerAttributeValue_Label = new Label();
        groupAttributeValue_Label = new Label();
        sizeBytesAttributeValue_Label = new Label();
        creationTimeAttributeValue_Label = new Label();
        lastModifiedTimeAttributeValue_Label = new Label();

        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), ownerAttribute_Label,
                ownerAttributeValue_Label);
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), groupAttribute_Label,
                groupAttributeValue_Label);
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), sizeBytesAttribute_Label,
                sizeBytesAttributeValue_Label);
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), creationTimeAttribute_Label,
                creationTimeAttributeValue_Label);
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), lastModifiedTimeAttribute_Label,
                lastModifiedTimeAttributeValue_Label);
        attributes_GridPane.add(firstSeparator, 0, attributes_GridPane.getRowCount(), 2, 1);

        readExtendedAttributesAndCreateGuiNodes();

        info_VBox = new VBox(FM_GUI.rem * 0.35D);
        info_VBox.getChildren().addAll(fileName_Label, attributes_GridPane);
        info_VBox.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE,
                CornerRadii.EMPTY, Insets.EMPTY)));

        info_ScrollPane = new ScrollPane(info_VBox);
        info_ScrollPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSTEELBLUE,
                CornerRadii.EMPTY, Insets.EMPTY)));
        info_ScrollPane.setFitToWidth(true);
        info_ScrollPane.setFitToHeight(true);
        info_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        info_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //------------------------- Подготовка узлов для прав доступа
        permission_Label = new Label("Permissions");
        permissionTitle_HBox = new HBox(permission_Label);
        permissionTitle_HBox.setAlignment(Pos.CENTER);

        ownerPermissionsBlock_Label = new Label("Owner:");
        groupPermissionsBlock_Label = new Label("Group:");
        otherPermissionsBlock_Label = new Label("Other:");

        ownerPermissionsBlock_VBox = new VBox(ownerPermissionsBlock_Label);
        ownerPermissionsBlock_VBox.setAlignment(Pos.CENTER);

        groupPermissionsBlock_VBox = new VBox(groupPermissionsBlock_Label);
        groupPermissionsBlock_VBox.setAlignment(Pos.CENTER);

        otherPermissionsBlock_VBox = new VBox(otherPermissionsBlock_Label);
        otherPermissionsBlock_VBox.setAlignment(Pos.CENTER);

        readPermissionsIndicator = new Label[3];
        writePermissionsIndicator = new Label[3];
        executePermissionsIndicator = new Label[3];

        readPermissionsIndicator_ImageView = new ImageView[readPermissionsIndicator.length];
        writePermissionsIndicator_ImageView = new ImageView[writePermissionsIndicator.length];
        executePermissionsIndicator_ImageView = new ImageView[executePermissionsIndicator.length];

        for (int k = 0; k < readPermissionsIndicator.length; k++)
        {
            readPermissionsIndicator[k] = new Label("Read");
            readPermissionsIndicator_ImageView[k] = new ImageView();
        }

        for (int k = 0; k < writePermissionsIndicator.length; k++)
        {
            writePermissionsIndicator[k] = new Label("Write");
            writePermissionsIndicator_ImageView[k] = new ImageView();
        }

        for (int k = 0; k < executePermissionsIndicator.length; k++)
        {
            executePermissionsIndicator[k] = new Label("Execute");
            executePermissionsIndicator_ImageView[k] = new ImageView();
        }


        readPermissionsIndicator_HBox = new HBox[readPermissionsIndicator.length];
        writePermissionsIndicator_HBox = new HBox[writePermissionsIndicator.length];
        executePermissionsIndicator_HBox = new HBox[executePermissionsIndicator.length];

        for (int k = 0; k < readPermissionsIndicator_HBox.length; k++)
        {
            readPermissionsIndicator_HBox[k] = new HBox(FM_GUI.rem * 0.35D,
                    readPermissionsIndicator[k], readPermissionsIndicator_ImageView[k]);
        }

        for (int k = 0; k < writePermissionsIndicator_HBox.length; k++)
        {
            writePermissionsIndicator_HBox[k] = new HBox(FM_GUI.rem * 0.35D,
                    writePermissionsIndicator[k], writePermissionsIndicator_ImageView[k]);
        }

        for (int k = 0; k < executePermissionsIndicator_HBox.length; k++)
        {
            executePermissionsIndicator_HBox[k] = new HBox(FM_GUI.rem * 0.35D,
                    executePermissionsIndicator[k], executePermissionsIndicator_ImageView[k]);
        }
        //==========================
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());
        attributes_GridPane.add(permissionTitle_HBox, 0, attributes_GridPane.getRowCount(), 2, 1);

        //Owner
        attributes_GridPane.add(readPermissionsIndicator_HBox[0], 1, attributes_GridPane.getRowCount());
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), ownerPermissionsBlock_VBox,
                writePermissionsIndicator_HBox[0]);
        attributes_GridPane.add(executePermissionsIndicator_HBox[0], 1, attributes_GridPane.getRowCount());
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());

        //Group
        attributes_GridPane.add(readPermissionsIndicator_HBox[1], 1, attributes_GridPane.getRowCount());
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), groupPermissionsBlock_VBox,
                writePermissionsIndicator_HBox[1]);
        attributes_GridPane.add(executePermissionsIndicator_HBox[1], 1, attributes_GridPane.getRowCount());
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());

        //Other
        attributes_GridPane.add(readPermissionsIndicator_HBox[2], 1, attributes_GridPane.getRowCount());
        attributes_GridPane.addRow(attributes_GridPane.getRowCount(), otherPermissionsBlock_VBox,
                writePermissionsIndicator_HBox[2]);
        attributes_GridPane.add(executePermissionsIndicator_HBox[2], 1, attributes_GridPane.getRowCount());
        //attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());

        readAttributes();

    }

    private void readAttributes()
    {
        try
        {
            BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(targetFile_Path, BasicFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            BasicFileAttributes basicFileAttributes = basicFileAttributeView.readAttributes();

            PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            PosixFileAttributes posixFileAttributes = posixFileAttributeView.readAttributes();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

            ownerAttributeValue_Label.setText(posixFileAttributes.owner().getName());
            groupAttributeValue_Label.setText(String.valueOf(posixFileAttributes.group().getName()));
            sizeBytesAttributeValue_Label.setText(NumberFormat.getNumberInstance().format(posixFileAttributes.size()));
            creationTimeAttributeValue_Label.setText(LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(),
                    ZoneId.systemDefault()).format(formatter));
            lastModifiedTimeAttributeValue_Label.setText(LocalDateTime.ofInstant(basicFileAttributes.lastModifiedTime().toInstant(),
                    ZoneId.systemDefault()).format(formatter));


            Image granted_Image = new Image(getClass().getResourceAsStream("/Images/granted.png"));
            Image hrenZnayet_Image = new Image(getClass().getResourceAsStream("/Images/cancel.png"));

            //Заполняем все ImageView изображениями с запретом
            for (int k = 0; k < readPermissionsIndicator_ImageView.length; k++)
            {
                setImageToImageView(hrenZnayet_Image, readPermissionsIndicator_ImageView[k]);
                setImageToImageView(hrenZnayet_Image, writePermissionsIndicator_ImageView[k]);
                setImageToImageView(hrenZnayet_Image, executePermissionsIndicator_ImageView[k]);
            }

            Iterator<PosixFilePermission> iterator = posixFileAttributes.permissions().iterator();
            while (iterator.hasNext())
            {
                PosixFilePermission temporaryPermission = iterator.next();

                if (temporaryPermission == PosixFilePermission.OWNER_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[2]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[2]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[2]);
                }
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void readExtendedAttributesAndCreateGuiNodes()
    {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(targetFile_Path,
                UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

        try
        {
            List<String> extendedAttributesNames_List = view.list();
            if (extendedAttributesNames_List.size() == 0)
            {
                System.out.println("Расширенных аттрибутов не обнаружено.");
            }
            else
            {
                extendedAttributesNames_Label = new Label[extendedAttributesNames_List.size()];
                extendedAttributesValues_Label = new Label[extendedAttributesNames_List.size()];

                Iterator<String> extendedAttributes_Iterator = extendedAttributesNames_List.iterator();
                int index = 0;
                String temporaryName = null;
                String temporaryValue = null;
                ByteBuffer buffer = null;

                while (extendedAttributes_Iterator.hasNext())
                {
                    temporaryName = extendedAttributes_Iterator.next();
                    buffer = ByteBuffer.allocate(view.size(temporaryName));
                    view.read(temporaryName, buffer);
                    buffer.flip();
                    temporaryValue = Charset.defaultCharset().decode(buffer).toString();
                    extendedAttributesNames_Label[index] = new Label(temporaryName + ":");
                    extendedAttributesValues_Label[index] = new Label(temporaryValue);

                    attributes_GridPane.addRow(attributes_GridPane.getRowCount(),
                            extendedAttributesNames_Label[index], extendedAttributesValues_Label[index]);
                    index++;

                }
                secondSeparator = new Separator(Orientation.HORIZONTAL);
                attributes_GridPane.add(secondSeparator, 0, attributes_GridPane.getRowCount(), 2, 1);
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void setImageToImageView(final Image image, ImageView targetImageView)
    {
        targetImageView.setImage(image);
        targetImageView.setPreserveRatio(true);
        targetImageView.setFitHeight(20.0D);
        targetImageView.setSmooth(true);
    }

    private void initializeEditTab()
    {
        editCreationTime_CheckBox = new CheckBox("Creation time ------------------------");
        editCreationTime_CheckBox.setTextFill(Color.HONEYDEW);
        editCreationTime_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editCreationTime_CheckBox.setOnAction(event ->
        {
            if (editCreationTime_CheckBox.isSelected())
            {
                if (creationTimeEditorPane_VBox == null)
                {
                    initializeCreationTimeEditorPane();
                }
                if (!edit_VBox.getChildren().contains(creationTimeEditorPane_VBox))
                {
                    edit_VBox.getChildren().add(1, creationTimeEditorPane_VBox);
                }
                apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(creationTimeEditorPane_VBox);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editGroup_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        editLastModifiedTime_CheckBox = new CheckBox("Last modified time ----------------");
        editLastModifiedTime_CheckBox.setBackground(Background.EMPTY);
        editLastModifiedTime_CheckBox.setTextFill(Color.HONEYDEW);
        editLastModifiedTime_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editLastModifiedTime_CheckBox.setOnAction(event ->
        {
            if (editLastModifiedTime_CheckBox.isSelected())
            {
                if (lastModifiedTimeEditorPane_VBox == null)
                {
                    initializeLastModifiedTimeEditorPane();
                }
                edit_VBox.getChildren().add(edit_VBox.getChildren().indexOf(editLastModifiedTime_CheckBox) + 1,
                        lastModifiedTimeEditorPane_VBox);
                apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(lastModifiedTimeEditorPane_VBox);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editOwner_CheckBox.isSelected() &&
                        !editGroup_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        editPermissions_CheckBox = new CheckBox("Permissions --------------------------");
        editPermissions_CheckBox.setTextFill(Color.HONEYDEW);
        editPermissions_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editPermissions_CheckBox.setOnAction(event ->
        {
            if (editPermissions_CheckBox.isSelected())
            {
                if (permissionSelector_GridPane == null)
                {
                    initializePermissionSelector();
                }
                setPermissionValuesToCheckBoxws();
                edit_VBox.getChildren().add(edit_VBox.getChildren().indexOf(editPermissions_CheckBox) + 1, permissionSelector_GridPane);
                apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(permissionSelector_GridPane);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editOwner_CheckBox.isSelected() &&
                        !editGroup_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        editOwner_CheckBox = new CheckBox("Owner ----------------------------------");
        editOwner_CheckBox.setTextFill(Color.HONEYDEW);
        editOwner_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editOwner_CheckBox.setOnAction(event ->
        {
            if (editOwner_CheckBox.isSelected())
            {
                if (ownerEditorPane_VBox == null)
                {
                    initializeOwnerEditorPane();
                }
                edit_VBox.getChildren().add(edit_VBox.getChildren().indexOf(editOwner_HBox) + 1, ownerEditorPane_VBox);
                apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(ownerEditorPane_VBox);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editOwner_CheckBox.isSelected() &&
                        !editGroup_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        editGroup_CheckBox = new CheckBox("Group ----------------------------------");
        editGroup_CheckBox.setTextFill(Color.HONEYDEW);
        editGroup_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editGroup_CheckBox.setOnAction(event ->
        {
            if (editGroup_CheckBox.isSelected())
            {
                if (groupEditorPane_VBox == null)
                {
                    initializeGroupEditorPane();
                }
                edit_VBox.getChildren().add(edit_VBox.getChildren().indexOf(editGroup_CheckBox) + 1, groupEditorPane_VBox);
                apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(groupEditorPane_VBox);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editOwner_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        editExtendedAttributes_CheckBox = new CheckBox("Extended Attributes ---------");
        editExtendedAttributes_CheckBox.setTextFill(Color.HONEYDEW);
        editExtendedAttributes_CheckBox.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
        editExtendedAttributes_CheckBox.setOnAction(event ->
        {
            if (editExtendedAttributes_CheckBox.isSelected())
            {
                if (extendedAttributesEditorPane_VBox == null)
                {
                    initializeExtendedAttributesEditorPane();
                }
                edit_VBox.getChildren().add(edit_VBox.getChildren().indexOf(editExtendedAttributes_CheckBox) + 1, extendedAttributesEditorPane_VBox);
                //apply_Button.setDisable(false);
            }
            else
            {
                edit_VBox.getChildren().remove(extendedAttributesEditorPane_VBox);
                if (!editCreationTime_CheckBox.isSelected() &&
                        !editLastModifiedTime_CheckBox.isSelected() &&
                        !editPermissions_CheckBox.isSelected() &&
                        !editOwner_CheckBox.isSelected() &&
                        !editGroup_CheckBox.isSelected())
                {
                    apply_Button.setDisable(true);
                }
            }
        });

        Image warning_Image = new Image(getClass().getResourceAsStream("/Images/caution.png"));
        ImageView warning_ImageView = new ImageView(warning_Image);
        warning_ImageView.setPreserveRatio(true);
        warning_ImageView.setFitHeight(30.0D);
        warning_ImageView.setSmooth(true);
        warning_ImageView.setOnMouseEntered(event ->
        {
            warning_MouseEntered();
        });
        warning_ImageView.setOnMouseExited(event ->
        {
            warning_MouseExited();
        });

        editOwner_HBox = new HBox(FM_GUI.rem * 0.7D, editOwner_CheckBox, warning_ImageView);

        apply_Button = new Button("Apply");
        apply_Button.setDisable(true);
        //apply_Button.setTextFill(Color.HONEYDEW);
        apply_Button.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        apply_Button.setBackground(Background.EMPTY);
        apply_Button.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.MEDIUM)));
        apply_Button.setOnAction(event ->
        {
            applyNewAttributes();
        });
        HBox apply_HBox = new HBox(apply_Button);
        apply_HBox.setAlignment(Pos.CENTER_RIGHT);

        edit_VBox = new VBox(FM_GUI.rem * 0.5D, editCreationTime_CheckBox, editLastModifiedTime_CheckBox,
                editOwner_HBox, editGroup_CheckBox, editPermissions_CheckBox, editExtendedAttributes_CheckBox, apply_HBox);
        edit_VBox.setPadding(new Insets(FM_GUI.rem * 0.5, 0.0D, 0.0D, 0.0D));

        edit_ScrollPane = new ScrollPane(edit_VBox);
        edit_ScrollPane.setFitToWidth(true);
        edit_ScrollPane.setFitToHeight(true);
        edit_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void initializePermissionSelector()
    {
        permissionSelector_GridPane = new GridPane();
        permissionSelector_GridPane.setHgap(FM_GUI.rem * 0.9D);
        permissionSelector_GridPane.setVgap(FM_GUI.rem * 0.9D);
        permissionSelector_GridPane.setAlignment(Pos.CENTER);
        permissionSelector_GridPane.setPadding(new Insets(0.0D, 0.0D, FM_GUI.rem * 0.95, 0.0D));
        permissionSelector_GridPane.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN,
                new Insets(FM_GUI.rem * 0.05, FM_GUI.rem * 0.95D, FM_GUI.rem * 0.01D,
                        FM_GUI.rem * 2.35D))));

        Label owner_Label = new Label("Owner");
        owner_Label.setTextFill(Color.HONEYDEW);
        owner_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Label group_Label = new Label("Group");
        group_Label.setTextFill(Color.HONEYDEW);
        group_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Label other_Label = new Label("Other");
        other_Label.setTextFill(Color.HONEYDEW);
        other_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Label read_Label = new Label("R");
        read_Label.setTextFill(Color.HONEYDEW);
        read_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Label write_Label = new Label("W");
        write_Label.setTextFill(Color.HONEYDEW);
        write_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Label execute_Label = new Label("E");
        execute_Label.setTextFill(Color.HONEYDEW);
        execute_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        permissionsCheckBoxes = new CheckBox[3][3];
        for (int k = 0; k < permissionsCheckBoxes.length; k++)
        {
            for (int j = 0; j < permissionsCheckBoxes[k].length; j++)
            {
                permissionsCheckBoxes[k][j] = new CheckBox();
            }
        }

        permissionSelector_GridPane.add(read_Label, 1, 1);
        permissionSelector_GridPane.add(write_Label, 2, 1);
        permissionSelector_GridPane.add(execute_Label, 3, 1);
        permissionSelector_GridPane.addRow(permissionSelector_GridPane.getRowCount(), owner_Label, permissionsCheckBoxes[0][0],
                permissionsCheckBoxes[0][1], permissionsCheckBoxes[0][2]);
        permissionSelector_GridPane.addRow(permissionSelector_GridPane.getRowCount(), group_Label, permissionsCheckBoxes[1][0],
                permissionsCheckBoxes[1][1], permissionsCheckBoxes[1][2]);
        permissionSelector_GridPane.addRow(permissionSelector_GridPane.getRowCount(), other_Label, permissionsCheckBoxes[2][0],
                permissionsCheckBoxes[2][1], permissionsCheckBoxes[2][2]);

    }

    /**
     * Устанавливает значение для флажков доступа в соответствии с действующими
     * параметрами доступа. Получение параметров доступа происходит при каждом
     * вызове.
     */
    private void setPermissionValuesToCheckBoxws()
    {
        for (int k = 0; k < permissionsCheckBoxes.length; k++)
        {
            for (int j = 0; j < permissionsCheckBoxes[k].length; j++)
            {
                permissionsCheckBoxes[k][j].setSelected(false);
            }
        }
        try
        {
            PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            PosixFileAttributes posixFileAttributes = posixFileAttributeView.readAttributes();

            Iterator<PosixFilePermission> iterator = posixFileAttributes.permissions().iterator();

            while (iterator.hasNext())
            {
                PosixFilePermission temporaryPermission = iterator.next();
                if (temporaryPermission == PosixFilePermission.OWNER_READ)
                {
                    permissionsCheckBoxes[0][0].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_WRITE)
                {
                    permissionsCheckBoxes[0][1].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_EXECUTE)
                {
                    permissionsCheckBoxes[0][2].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_READ)
                {
                    permissionsCheckBoxes[1][0].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_WRITE)
                {
                    permissionsCheckBoxes[1][1].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_EXECUTE)
                {
                    permissionsCheckBoxes[1][2].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_READ)
                {
                    permissionsCheckBoxes[2][0].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_WRITE)
                {
                    permissionsCheckBoxes[2][1].setSelected(true);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_EXECUTE)
                {
                    permissionsCheckBoxes[2][2].setSelected(true);
                }
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void initializeCreationTimeEditorPane()
    {
        creationTimeEditorPane_VBox = new VBox(FM_GUI.rem * 0.45D);
        creationTimeEditorPane_VBox.setPadding(new Insets(FM_GUI.rem * 0.7D, FM_GUI.rem * 0.7D,
                FM_GUI.rem * 0.7D, FM_GUI.rem * 1.5D));
        creationTimeEditorPane_VBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(0.0D,
                FM_GUI.rem * 0.95D, FM_GUI.rem * 0.55D, FM_GUI.rem * 1.0D))));

        PosixFileAttributeView posixFileAttributeView = null;
        PosixFileAttributes posixFileAttributes = null;

        try
        {
            posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            posixFileAttributes = posixFileAttributeView.readAttributes();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        LocalDate creationDate = LocalDate.ofInstant(posixFileAttributes.creationTime().toInstant(), ZoneId.systemDefault());

        creationTime_Picker = new DatePicker(creationDate);
        creationTime_Picker.setShowWeekNumbers(true);
        creationTime_Picker.setPrefWidth(new Text("28.04.2022_282828").getBoundsInParent().getWidth());

        Label date_Label = new Label("Date");
        date_Label.setTextFill(Color.HONEYDEW);
        date_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));


        HBox date_HBox = new HBox(FM_GUI.rem * 0.85D, date_Label, creationTime_Picker);

        String[] previousTextFieldText = new String[1];
        previousTextFieldText[0] = "";

        creationTime_TextField = new TextField();
        creationTime_TextField.setPromptText("HH:MM:SS");
        creationTime_TextField.setText(LocalDateTime.ofInstant(posixFileAttributes.creationTime().toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        creationTime_TextField.setPrefColumnCount(6);
        creationTime_TextField.setOnKeyTyped((event) ->
        {
            System.out.println("value: " + creationTime_TextField.getText());
            char currentCharacter = event.getCharacter().charAt(0);
            System.out.println((int) currentCharacter);

            if (creationTime_TextField.getText().length() > 8)
            {
                creationTime_TextField.setText(previousTextFieldText[0]);
                creationTime_TextField.end();
            }
            else
            {
                if ((int) currentCharacter < 48 || (int) currentCharacter > 57)
                {
                    if ((int) currentCharacter == 58 || (int) currentCharacter == 8)//: и backspace
                    {

                    }
                    else
                    {
                        creationTime_TextField.setText(previousTextFieldText[0]);
                        creationTime_TextField.end();
                    }
                }
            }
            previousTextFieldText[0] = creationTime_TextField.getText();
        });

        Label time_Label = new Label("Time");
        time_Label.setTextFill(Color.HONEYDEW);
        time_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        HBox time_HBox = new HBox(FM_GUI.rem * 0.85D, time_Label, creationTime_TextField);

        creationTimeEditorPane_VBox.getChildren().addAll(date_HBox, time_HBox);
    }

    private void initializeLastModifiedTimeEditorPane()
    {
        lastModifiedTimeEditorPane_VBox = new VBox(FM_GUI.rem * 0.45D);
        lastModifiedTimeEditorPane_VBox.setPadding(new Insets(FM_GUI.rem * 0.7D, FM_GUI.rem * 0.7D,
                FM_GUI.rem * 0.7D, FM_GUI.rem * 1.5D));
        lastModifiedTimeEditorPane_VBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(0.0D,
                FM_GUI.rem * 0.95D, FM_GUI.rem * 0.55D, FM_GUI.rem * 1.0D))));

        PosixFileAttributeView posixFileAttributeView = null;
        PosixFileAttributes posixFileAttributes = null;

        try
        {
            posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            posixFileAttributes = posixFileAttributeView.readAttributes();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        LocalDate lastModifiedDate = LocalDate.ofInstant(posixFileAttributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        lastModifiedDate_Picker = new DatePicker(lastModifiedDate);
        lastModifiedDate_Picker.setShowWeekNumbers(true);
        lastModifiedDate_Picker.setPrefWidth(new Text("28.04.2022_282828").getBoundsInParent().getWidth());

        Label date_Label = new Label("Date");
        date_Label.setTextFill(Color.HONEYDEW);
        date_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        HBox date_HBox = new HBox(FM_GUI.rem * 0.85D, date_Label, lastModifiedDate_Picker);

        String[] previousTextFieldText = new String[1];
        previousTextFieldText[0] = "";

        lastModifiedTime_TextField = new TextField();
        lastModifiedTime_TextField.setPromptText("HH:MM:SS");
        lastModifiedTime_TextField.setText(LocalDateTime.ofInstant(posixFileAttributes.creationTime().toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lastModifiedTime_TextField.setPrefColumnCount(6);
        lastModifiedTime_TextField.setOnKeyTyped((event) ->
        {
            System.out.println("value: " + lastModifiedTime_TextField.getText());
            char currentCharater = event.getCharacter().charAt(0);
            System.out.println((int) currentCharater);

            if (lastModifiedTime_TextField.getText().length() > 8)
            {
                lastModifiedTime_TextField.setText(previousTextFieldText[0]);
                lastModifiedTime_TextField.end();
            }
            else
            {
                if ((int) currentCharater < 48 || (int) currentCharater > 57)
                {
                    if ((int) currentCharater == 58 || (int) currentCharater == 8)//: и backspace
                    {

                    }
                    else
                    {
                        lastModifiedTime_TextField.setText(previousTextFieldText[0]);
                        lastModifiedTime_TextField.end();
                    }
                }
            }
            previousTextFieldText[0] = lastModifiedTime_TextField.getText();
        });

        Label time_Label = new Label("Time");
        time_Label.setTextFill(Color.HONEYDEW);
        time_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        HBox time_HBox = new HBox(FM_GUI.rem * 0.85D, time_Label, lastModifiedTime_TextField);

        lastModifiedTimeEditorPane_VBox.getChildren().addAll(date_HBox, time_HBox);
    }

    private void initializeOwnerEditorPane()
    {
        ownerEditorPane_VBox = new VBox(FM_GUI.rem * 0.45D);
        ownerEditorPane_VBox.setPadding(new Insets(FM_GUI.rem * 0.7D, FM_GUI.rem * 0.7D,
                FM_GUI.rem * 0.7D, FM_GUI.rem * 1.5D));
        ownerEditorPane_VBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(0.0D,
                FM_GUI.rem * 0.95D, FM_GUI.rem * 0.55D, FM_GUI.rem * 1.0D))));

        PosixFileAttributeView posixFileAttributeView = null;
        PosixFileAttributes posixFileAttributes = null;

        try
        {
            posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            posixFileAttributes = posixFileAttributeView.readAttributes();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        newUserPrincipal = new UserPrincipal[1];
        UserPrincipalLookupService userPrincipalLookupService = targetFile_Path.getFileSystem().getUserPrincipalLookupService();

        Label name_Label = new Label("Name");
        name_Label.setTextFill(Color.HONEYDEW);
        name_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Text forCalculateWidth_Text = new Text(name_Label.getText());
        forCalculateWidth_Text.setFont(name_Label.getFont());
        name_Label.setMinWidth(forCalculateWidth_Text.getBoundsInParent().getWidth());

        Label ownerNotFound_Label = new Label("User with specified name doesn't exist.");
        ownerNotFound_Label.setTextFill(Color.FIREBRICK);
        ownerNotFound_Label.setEffect(new DropShadow(20.0D, Color.GAINSBORO));
        ownerNotFound_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        ownerNotFound_Label.setVisible(false);

        HBox ownerNotFound_HBox = new HBox(ownerNotFound_Label);
        ownerNotFound_HBox.setAlignment(Pos.CENTER_RIGHT);

        TextField ownerName_TextField = new TextField();
        ownerName_TextField.setPromptText("Enter here name of new owner");
        ownerName_TextField.setText(posixFileAttributes.owner().toString());
        //ownerName_TextField.setPrefColumnCount(12);
        ownerName_TextField.setOnKeyTyped(event ->
        {
            try
            {
                UserPrincipal temporaryUserPrincipal = userPrincipalLookupService.lookupPrincipalByName(ownerName_TextField.getText());
                if (temporaryUserPrincipal != null)
                {
                    newUserPrincipal[0] = temporaryUserPrincipal;
                }
                System.out.println("Неуж-то есть такой пользователь.");
                ownerNotFound_Label.setVisible(false);
                apply_Button.setDisable(false);
            }
            catch (UserPrincipalNotFoundException userPrincipalNotFoundException)
            {
                System.out.println("Пользователя с таким именем не существует.");
                ownerNotFound_Label.setVisible(true);
                apply_Button.setDisable(true);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        });


        HBox ownerName_HBox = new HBox(FM_GUI.rem * 0.85D, name_Label, ownerName_TextField);

        ownerEditorPane_VBox.getChildren().addAll(ownerName_HBox, ownerNotFound_HBox);
    }

    private void initializeGroupEditorPane()
    {
        groupEditorPane_VBox = new VBox(FM_GUI.rem * 0.45D);
        groupEditorPane_VBox.setPadding(new Insets(FM_GUI.rem * 0.7D, FM_GUI.rem * 0.7D,
                FM_GUI.rem * 0.7D, FM_GUI.rem * 1.5D));
        groupEditorPane_VBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(0.0D,
                FM_GUI.rem * 0.95D, FM_GUI.rem * 0.55D, FM_GUI.rem * 1.0D))));

        PosixFileAttributeView posixFileAttributeView = null;
        PosixFileAttributes posixFileAttributes = null;

        try
        {
            posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            posixFileAttributes = posixFileAttributeView.readAttributes();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        newGroupPrincipal = new GroupPrincipal[1];
        UserPrincipalLookupService userPrincipalLookupService = targetFile_Path.getFileSystem().getUserPrincipalLookupService();

        Label name_Label = new Label("Name");
        name_Label.setTextFill(Color.HONEYDEW);
        name_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        Text forCalculateWidth_Text = new Text(name_Label.getText());
        forCalculateWidth_Text.setFont(name_Label.getFont());
        name_Label.setMinWidth(forCalculateWidth_Text.getBoundsInParent().getWidth());

        Label groupNotFound_Label = new Label("Group with specified name doesn't exist.");
        groupNotFound_Label.setTextFill(Color.FIREBRICK);
        groupNotFound_Label.setEffect(new DropShadow(20.0D, Color.GAINSBORO));
        groupNotFound_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        groupNotFound_Label.setVisible(false);

        HBox groupNotFound_HBox = new HBox(groupNotFound_Label);
        groupNotFound_HBox.setAlignment(Pos.CENTER_RIGHT);

        TextField groupName_TextField = new TextField();
        groupName_TextField.setPromptText("Enter new name of group here");
        groupName_TextField.setText(posixFileAttributes.group().getName());
        groupName_TextField.setOnKeyTyped(event ->
        {
            try
            {
                GroupPrincipal temporaryGroupPrincipal = userPrincipalLookupService.lookupPrincipalByGroupName(groupName_TextField.getText());
                if (temporaryGroupPrincipal != null)
                {
                    newGroupPrincipal[0] = temporaryGroupPrincipal;
                }
                System.out.println("Неуж-то есть такая группа пользователей.");
                groupNotFound_Label.setVisible(false);
                apply_Button.setDisable(false);
            }
            catch (UserPrincipalNotFoundException userPrincipalNotFoundException)
            {
                System.out.println("Группы пользователей с таким именем не существует.");
                groupNotFound_Label.setVisible(true);
                apply_Button.setDisable(true);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        });


        HBox ownerName_HBox = new HBox(FM_GUI.rem * 0.85D, name_Label, groupName_TextField);

        groupEditorPane_VBox.getChildren().addAll(ownerName_HBox, groupNotFound_HBox);
    }

    private void initializeExtendedAttributesEditorPane()
    {
        extendedAttributesEditorPane_VBox = new VBox(FM_GUI.rem * 0.45D);
        extendedAttributesEditorPane_VBox.setPadding(new Insets(FM_GUI.rem * 0.7D, FM_GUI.rem * 0.7D,
                FM_GUI.rem * 0.7D, FM_GUI.rem * 1.5D));
        extendedAttributesEditorPane_VBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(0.0D,
                FM_GUI.rem * 0.95D, FM_GUI.rem * 0.55D, FM_GUI.rem * 1.0D))));

        UserDefinedFileAttributeView view = null;
        List<String> extendedAttributesNames_List = null;

        try
        {
            view = Files.getFileAttributeView(targetFile_Path, UserDefinedFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            extendedAttributesNames_List = view.list();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        Text forCalculateWidth_Text = new Text();

        Label name_Label = new Label("Name");
        name_Label.setTextFill(Color.HONEYDEW);
        name_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        forCalculateWidth_Text.setText(name_Label.getText());
        forCalculateWidth_Text.setFont(name_Label.getFont());
        name_Label.setMinWidth(forCalculateWidth_Text.getBoundsInParent().getWidth());

        attributesNames_ComboBox = new ComboBox<>();
        attributesNames_ComboBox.getItems().add("<New>");
        attributesNames_ComboBox.setOnAction(event ->
        {
            int selectedIndex = attributesNames_ComboBox.getSelectionModel().getSelectedIndex();
            System.out.println("Выбран єлемент с индексом: " + selectedIndex);

            if (selectedIndex == 0)
            {
                attributesNames_ComboBox.setEditable(true);
                deleteExtendedAttribute_Label.setVisible(false);
            }
            else if (selectedIndex == -1)
            {
                System.out.println("Новый єлемент: " + attributesNames_ComboBox.getValue());
                attributesNames_ComboBox.getItems().add(attributesNames_ComboBox.getValue());
                deleteExtendedAttribute_Label.setVisible(true);
            }
            else
            {
                attributesNames_ComboBox.setEditable(false);
                deleteExtendedAttribute_Label.setVisible(true);
            }
            apply_Button.setDisable(false);
        });

        deleteExtendedAttribute_Label = new Label("✘");
        deleteExtendedAttribute_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 24.0D));
        deleteExtendedAttribute_Label.setTextFill(Color.CRIMSON);
        deleteExtendedAttribute_Label.setEffect(new DropShadow(4.0D, Color.BLACK));
        deleteExtendedAttribute_Label.setTooltip(new Tooltip("Delete attribute"));
        deleteExtendedAttribute_Label.setOnMouseClicked(event -> deleteExtendedAttribute_Action(null));
        deleteExtendedAttribute_Label.setVisible(false);

        Iterator<String> extendedAttributes_Iterator = extendedAttributesNames_List.iterator();
        while (extendedAttributes_Iterator.hasNext())
        {
            attributesNames_ComboBox.getItems().add(extendedAttributes_Iterator.next());
        }


        extendedAttributeValue_TextField = new TextField();
        extendedAttributeValue_TextField.setPromptText("Enter new value here");
        extendedAttributeValue_TextField.setText("");
        extendedAttributeValue_TextField.textProperty().addListener((event, value1, value2) ->
        {
            if(value2.length() > 255)
            {
                extendedAttributeValue_TextField.setText(value1);
            }
        });

        Label value_Label = new Label("Value");
        value_Label.setTextFill(Color.HONEYDEW);
        value_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

        forCalculateWidth_Text.setText(value_Label.getText());
        forCalculateWidth_Text.setFont(value_Label.getFont());
        value_Label.setMinWidth(forCalculateWidth_Text.getBoundsInParent().getWidth());

        HBox name_HBox = new HBox(FM_GUI.rem * 0.85D, name_Label, attributesNames_ComboBox, deleteExtendedAttribute_Label);
        HBox value_HBox = new HBox(FM_GUI.rem * 0.85D, value_Label, extendedAttributeValue_TextField);

        extendedAttributesEditorPane_VBox.getChildren().addAll(name_HBox, value_HBox);
    }


    private void applyNewAttributes()
    {
        try
        {
            if (editCreationTime_CheckBox.isSelected())
            {
                if (System.getProperty("os.name").contains("Linux"))
                {
                    applyNewCreationTimeOnLinux();
                }
                else
                {
                    applyNewCreationTime();
                }
            }
        }
        catch (AccessDeniedException accessDeniedException)
        {
            System.out.println("Недостаточно привилегий для редактирования даты создания.");
            creationTimeEditorPane_VBox.getChildren().clear();
            Label accessDenied = new Label("You can not change creation time of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            creationTimeEditorPane_VBox.getChildren().add(accessDenied);
            creationTimeEditorPane_VBox.setAlignment(Pos.CENTER);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }


        try
        {
            if (editLastModifiedTime_CheckBox.isSelected())
            {
                if (System.getProperty("os.name").contains("Linux"))
                {
                    applyNewLastModifiedTimeOnLinux();
                }
                else
                {
                    applyNewLastModifiedTime();
                }
            }
        }
        catch (AccessDeniedException accessDeniedException)
        {
            System.out.println("Недостаточно привилегий для редактирования даты последнего редактирования.");
            lastModifiedTimeEditorPane_VBox.getChildren().clear();
            Label accessDenied = new Label("You can not change last modified time of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            lastModifiedTimeEditorPane_VBox.getChildren().add(accessDenied);
            lastModifiedTimeEditorPane_VBox.setAlignment(Pos.CENTER);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }

        try
        {
            if (editExtendedAttributes_CheckBox.isSelected())
            {
                applyNewExtendedAttribute();
                apply_Button.setDisable(true);
            }
        }
        catch (FileSystemException fileSystemException)
        {
            System.out.println("Недостаточно привилегий для изменения расширенных аттрибутов.");
            extendedAttributesEditorPane_VBox.getChildren().clear();
            Label accessDenied = new Label("You can not change extended attributes of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            extendedAttributesEditorPane_VBox.getChildren().add(accessDenied);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }

        try
        {
            if (editOwner_CheckBox.isSelected())
            {
                applyNewOwner();
                apply_Button.setDisable(true);
            }
        }
        catch (FileSystemException fileSystemException)
        {
            System.out.println("Недостаточно привилегий для изменения владельца.");
            ownerEditorPane_VBox.getChildren().clear();
            Label accessDenied = new Label("You can not change owner of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            ownerEditorPane_VBox.getChildren().add(accessDenied);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }

        try
        {
            if (editGroup_CheckBox.isSelected())
            {
                applyNewGroup();
                apply_Button.setDisable(true);
            }
        }
        catch (FileSystemException fileSystemException)
        {
            System.out.println("Недостаточно привилегий для изменения группы.");
            groupEditorPane_VBox.getChildren().clear();
            Label accessDenied = new Label("You can not change group of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            groupEditorPane_VBox.getChildren().add(accessDenied);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }


        try
        {
            if (editPermissions_CheckBox.isSelected())
            {
                applyNewPermissionsToFile();
                apply_Button.setDisable(true);
            }
        }
        catch (FileSystemException fileSystemException)
        {
            System.out.println("Недостаточно привилегий для редактирования прав доступа.");
            permissionSelector_GridPane.getChildren().clear();
            Label accessDenied = new Label("You can not change permissions of this file. Required Root privilege.");
            accessDenied.setWrapText(true);
            accessDenied.setTextFill(Color.LIGHTCORAL);
            accessDenied.setEffect(new DropShadow(11.0D, Color.BLACK));
            accessDenied.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14.0D));
            permissionSelector_GridPane.getChildren().add(accessDenied);
            disableAllEditCheckBoxes();
            return;
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
            return;
        }

        FM_GUI.showLittleNotification(stage, "Changes have been successfully saved.", 3);
        apply_Button.setDisable(true);
        info_edit_TabPane.getSelectionModel().select(0);
        updateInfoTabContent();
        unselectAllEditCheckBoxes();
    }


    /**
     * Задает новые сведения о дате создания файла, а заодно и о дате последнего чтения
     * и редактирования. Если изменять только один из этих параметров, то ничего
     * не будет применено.
     */
    private void applyNewCreationTimeOnLinux() throws AccessDeniedException, IOException
    {
        LocalDate newCreationDate_LocalDate = creationTime_Picker.getValue();

        String[] newTimes = creationTime_TextField.getText().split(":");

        LocalTime newCreationTime_LocalTime = LocalTime.of(Integer.parseInt(newTimes[0]),
                Integer.parseInt(newTimes[1]), Integer.parseInt(newTimes[2]));

        Instant newCreationTime_Instant = LocalDateTime.of(newCreationDate_LocalDate, newCreationTime_LocalTime)
                .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));

        FileTime newCreationTime_FileTime = FileTime.from(newCreationTime_Instant);
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        System.out.println(newCreationTime_FileTime.toString());

        posixFileAttributeView.setTimes(newCreationTime_FileTime, newCreationTime_FileTime,
                newCreationTime_FileTime);
        System.out.println("Новое время создания должно быть записано.");
    }

    private void applyNewCreationTime() throws AccessDeniedException, IOException
    {
        LocalDate newCreationDate_LocalDate = creationTime_Picker.getValue();

        String[] newTimes = creationTime_TextField.getText().split(":");

        LocalTime newCreationTime_LocalTime = LocalTime.of(Integer.parseInt(newTimes[0]),
                Integer.parseInt(newTimes[1]), Integer.parseInt(newTimes[2]));

        Instant newCreationTime_Instant = LocalDateTime.of(newCreationDate_LocalDate, newCreationTime_LocalTime)
                .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));

        FileTime newCreationTime_FileTime = FileTime.from(newCreationTime_Instant);
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        System.out.println(newCreationTime_FileTime.toString());

        posixFileAttributeView.setTimes(null, null, newCreationTime_FileTime);
        System.out.println("Новое время создания должно быть записано.");
    }


    private void applyNewLastModifiedTimeOnLinux() throws AccessDeniedException, IOException
    {
        LocalDate newLastModifiedDate_LocalDate = lastModifiedDate_Picker.getValue();

        String[] newTimes = lastModifiedTime_TextField.getText().split(":");

        LocalTime newLastModifiedTime_LocalTime = LocalTime.of(Integer.parseInt(newTimes[0]),
                Integer.parseInt(newTimes[1]), Integer.parseInt(newTimes[2]));

        Instant newLastModifiedTime_Instant = LocalDateTime.of(newLastModifiedDate_LocalDate, newLastModifiedTime_LocalTime)
                .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));

        FileTime newLastModified_FileTime = FileTime.from(newLastModifiedTime_Instant);
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        System.out.println(newLastModified_FileTime.toString());

        posixFileAttributeView.setTimes(newLastModified_FileTime, newLastModified_FileTime, newLastModified_FileTime);
        System.out.println("Новое время изменения должно быть записано.");
    }

    private void applyNewLastModifiedTime() throws AccessDeniedException, IOException
    {
        LocalDate newLastModifiedDate_LocalDate = lastModifiedDate_Picker.getValue();

        String[] newTimes = lastModifiedTime_TextField.getText().split(":");

        LocalTime newLastModifiedTime_LocalTime = LocalTime.of(Integer.parseInt(newTimes[0]),
                Integer.parseInt(newTimes[1]), Integer.parseInt(newTimes[2]));

        Instant newLastModifiedTime_Instant = LocalDateTime.of(newLastModifiedDate_LocalDate, newLastModifiedTime_LocalTime)
                .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));

        FileTime newLastModified_FileTime = FileTime.from(newLastModifiedTime_Instant);
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        System.out.println(newLastModified_FileTime.toString());

        posixFileAttributeView.setTimes(newLastModified_FileTime, null, null);
        System.out.println("Новое время изменения должно быть записано.");
    }

    private void applyNewOwner() throws AccessDeniedException, IOException
    {
        System.out.println("User: " + newUserPrincipal[0].getName());
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        posixFileAttributeView.setOwner(newUserPrincipal[0]);
    }

    private void applyNewGroup() throws AccessDeniedException, IOException
    {
        System.out.println("Group: " + newGroupPrincipal[0].getName());
        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        posixFileAttributeView.setGroup(newGroupPrincipal[0]);
    }

    private void applyNewExtendedAttribute() throws AccessDeniedException, IOException
    {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(targetFile_Path, UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

        String newAttributeName = attributesNames_ComboBox.getValue();
        String newAttributeValue = extendedAttributeValue_TextField.getText();

        System.out.println("Добавление нового аттрибута с именем " + newAttributeName
                + ", значением " + newAttributeValue);

        view.write(newAttributeName, Charset.defaultCharset().encode(newAttributeValue));
    }


    /**
     * Обрабатывает информацию с CheckBox"ов, и на их основе редактирует права доступа
     * к файлу.
     */
    private void applyNewPermissionsToFile() throws IOException
    {
        HashSet<PosixFilePermission> newPermissions = new HashSet<>();
        if (permissionsCheckBoxes[0][0].isSelected())//owner-read
        {
            newPermissions.add(PosixFilePermission.OWNER_READ);
        }
        if (permissionsCheckBoxes[0][1].isSelected())//owner-write
        {
            newPermissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if (permissionsCheckBoxes[0][2].isSelected())
        {
            newPermissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if (permissionsCheckBoxes[1][0].isSelected())
        {
            newPermissions.add(PosixFilePermission.GROUP_READ);
        }
        if (permissionsCheckBoxes[1][1].isSelected())
        {
            newPermissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if (permissionsCheckBoxes[1][2].isSelected())
        {
            newPermissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if (permissionsCheckBoxes[2][0].isSelected())
        {
            newPermissions.add(PosixFilePermission.OTHERS_READ);
        }
        if (permissionsCheckBoxes[2][1].isSelected())
        {
            newPermissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (permissionsCheckBoxes[2][2].isSelected())
        {
            newPermissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }


        PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        posixFileAttributeView.setPermissions(newPermissions);
        FM_GUI.showLittleNotification(stage, "Permissions have been successfully changed.", 3);
        return;
    }

    private void disableAllEditCheckBoxes()
    {
        editCreationTime_CheckBox.setSelected(false);
        editCreationTime_CheckBox.setDisable(true);
        editLastModifiedTime_CheckBox.setSelected(false);
        editLastModifiedTime_CheckBox.setDisable(true);
        editPermissions_CheckBox.setSelected(false);
        editPermissions_CheckBox.setDisable(true);
        editOwner_CheckBox.setSelected(false);
        editOwner_CheckBox.setDisable(true);
        editGroup_CheckBox.setSelected(false);
        editGroup_CheckBox.setDisable(true);
        editExtendedAttributes_CheckBox.setSelected(false);
        editExtendedAttributes_CheckBox.setDisable(true);
    }

    private void unselectAllEditCheckBoxes()
    {
        editCreationTime_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(creationTimeEditorPane_VBox);
        editLastModifiedTime_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(lastModifiedTimeEditorPane_VBox);
        editPermissions_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(permissionSelector_GridPane);
        editOwner_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(ownerEditorPane_VBox);
        editGroup_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(groupEditorPane_VBox);
        editExtendedAttributes_CheckBox.setSelected(false);
        edit_VBox.getChildren().remove(extendedAttributesEditorPane_VBox);
    }

    private void warning_MouseEntered()
    {
        if (warning_Popup == null)
        {
            Label warning_Label = new Label("This operation required Root privilege.");
            warning_Label.setTextFill(Color.FIREBRICK);
            warning_Label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));

            HBox warning_HBox = new HBox(warning_Label);
            warning_HBox.setAlignment(Pos.CENTER);
            warning_HBox.setPadding(new Insets(FM_GUI.rem * 0.65D));
            /// warning_HBox.setBackground(new Background(new BackgroundFill(Color.LIGHTPINK,
            // new CornerRadii(0.05),new Insets(FM_GUI.rem * 0.45D))));
            warning_HBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY, BorderStroke.MEDIUM, Insets.EMPTY)));

            warning_Popup = new Popup();
            warning_Popup.getScene().setFill(Color.LIGHTPINK);
            warning_Popup.getContent().add(warning_HBox);
        }
        warning_Popup.show(stage);
    }

    private void warning_MouseExited()
    {
        warning_Popup.hide();
    }

    public void show()
    {
        stage.show();
    }

    private void updateInfoTabContent()
    {
        try
        {
            BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(targetFile_Path, BasicFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            BasicFileAttributes basicFileAttributes = basicFileAttributeView.readAttributes();

            PosixFileAttributeView posixFileAttributeView = Files.getFileAttributeView(targetFile_Path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            PosixFileAttributes posixFileAttributes = posixFileAttributeView.readAttributes();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

            ownerAttributeValue_Label.setText(posixFileAttributes.owner().getName());
            groupAttributeValue_Label.setText(String.valueOf(posixFileAttributes.group().getName()));
            sizeBytesAttributeValue_Label.setText(NumberFormat.getNumberInstance().format(posixFileAttributes.size()));
            creationTimeAttributeValue_Label.setText(LocalDateTime.ofInstant(basicFileAttributes.creationTime().toInstant(), ZoneId.systemDefault()).format(formatter));
            lastModifiedTimeAttributeValue_Label.setText(LocalDateTime.ofInstant(basicFileAttributes.lastModifiedTime().toInstant(), ZoneId.systemDefault()).format(formatter));

            attributes_GridPane.getChildren().clear();
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), ownerAttribute_Label, ownerAttributeValue_Label);
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), groupAttribute_Label, groupAttributeValue_Label);
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), sizeBytesAttribute_Label, sizeBytesAttributeValue_Label);
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), creationTimeAttribute_Label, creationTimeAttributeValue_Label);
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), lastModifiedTimeAttribute_Label, lastModifiedTimeAttributeValue_Label);
            attributes_GridPane.add(firstSeparator, 0, attributes_GridPane.getRowCount(), 2, 1);

            //----------------------------------
            UserDefinedFileAttributeView view = Files.getFileAttributeView(targetFile_Path,
                    UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            List<String> extendedAttributes_List = view.list();

            if (extendedAttributesNames_Label != null)
            {
                for (int k = 0; k < extendedAttributesValues_Label.length; k++)
                {
                    extendedAttributesValues_Label[k] = null;
                    extendedAttributesNames_Label[k] = null;
                }
            }
            extendedAttributesValues_Label = null;
            extendedAttributesNames_Label = null;

            extendedAttributesNames_Label = new Label[extendedAttributes_List.size()];
            extendedAttributesValues_Label = new Label[extendedAttributes_List.size()];

            Iterator<String> extendedAttributes_Iterator = extendedAttributes_List.iterator();
            int index = 0;
            String temporaryName = null;
            String temporaryValue = null;
            ByteBuffer buffer;

            while (extendedAttributes_Iterator.hasNext())
            {
                temporaryName = extendedAttributes_Iterator.next();
                buffer = ByteBuffer.allocate(view.size(temporaryName));
                view.read(temporaryName, buffer);
                buffer.flip();
                temporaryValue = Charset.defaultCharset().decode(buffer).toString();

                extendedAttributesNames_Label[index] = new Label(temporaryName + " :");
                extendedAttributesValues_Label[index] = new Label(temporaryValue);
                attributes_GridPane.addRow(attributes_GridPane.getRowCount(),
                        extendedAttributesNames_Label[index], extendedAttributesValues_Label[index]);
                index++;
            }

            if (secondSeparator == null)
            {
                secondSeparator = new Separator(Orientation.HORIZONTAL);
            }

            if (extendedAttributes_List.size() > 0)
            {
                attributes_GridPane.add(secondSeparator, 0, attributes_GridPane.getRowCount(), 2, 1);
            }
            //=======================================

            attributes_GridPane.add(permissionTitle_HBox, 0, attributes_GridPane.getRowCount(), 2, 1);

            //Owner
            attributes_GridPane.add(readPermissionsIndicator_HBox[0], 1, attributes_GridPane.getRowCount());
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), ownerPermissionsBlock_VBox,
                    writePermissionsIndicator_HBox[0]);
            attributes_GridPane.add(executePermissionsIndicator_HBox[0], 1, attributes_GridPane.getRowCount());
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());

            //Group
            attributes_GridPane.add(readPermissionsIndicator_HBox[1], 1, attributes_GridPane.getRowCount());
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), groupPermissionsBlock_VBox,
                    writePermissionsIndicator_HBox[1]);
            attributes_GridPane.add(executePermissionsIndicator_HBox[1], 1, attributes_GridPane.getRowCount());
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), new Label());

            //Other
            attributes_GridPane.add(readPermissionsIndicator_HBox[2], 1, attributes_GridPane.getRowCount());
            attributes_GridPane.addRow(attributes_GridPane.getRowCount(), otherPermissionsBlock_VBox,
                    writePermissionsIndicator_HBox[2]);
            attributes_GridPane.add(executePermissionsIndicator_HBox[2], 1, attributes_GridPane.getRowCount());

            Image granted_Image = new Image(getClass().getResourceAsStream("/Images/granted.png"));
            Image hrenZnayet_Image = new Image(getClass().getResourceAsStream("/Images/cancel.png"));


            //Заполняем все ImageView изображениями с запретом
            for (int k = 0; k < readPermissionsIndicator_ImageView.length; k++)
            {
                setImageToImageView(hrenZnayet_Image, readPermissionsIndicator_ImageView[k]);
                setImageToImageView(hrenZnayet_Image, writePermissionsIndicator_ImageView[k]);
                setImageToImageView(hrenZnayet_Image, executePermissionsIndicator_ImageView[k]);
            }

            Iterator<PosixFilePermission> iterator = posixFileAttributes.permissions().iterator();
            while (iterator.hasNext())
            {
                PosixFilePermission temporaryPermission = iterator.next();
                if (temporaryPermission == PosixFilePermission.OWNER_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.OWNER_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[0]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.GROUP_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[1]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_READ)
                {
                    setImageToImageView(granted_Image, readPermissionsIndicator_ImageView[2]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_WRITE)
                {
                    setImageToImageView(granted_Image, writePermissionsIndicator_ImageView[2]);
                }
                else if (temporaryPermission == PosixFilePermission.OTHERS_EXECUTE)
                {
                    setImageToImageView(granted_Image, executePermissionsIndicator_ImageView[2]);
                }
            }

        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void deleteExtendedAttribute_Action(ActionEvent event)
    {
        String temporaryAttributeName = attributesNames_ComboBox.getValue();

        try
        {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(targetFile_Path,
                    UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            view.delete(temporaryAttributeName);
            System.out.println("Дополнительный аттрибут с именем " + temporaryAttributeName
                    + " был удален.");

            attributesNames_ComboBox.getItems().remove(attributesNames_ComboBox.getSelectionModel().getSelectedIndex());
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

    }

}
