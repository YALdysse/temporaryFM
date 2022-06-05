package org.yaldysse.fm;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.util.Callback;
import javafx.util.Duration;
import org.yaldysse.fm.dialogs.*;
import org.yaldysse.fm.dialogs.copy.CopyFilesDialog;
import org.yaldysse.fm.dialogs.delete.DeleteFileDialog;
import org.yaldysse.fm.dialogs.delete.DeleteOperationResult;
import org.yaldysse.tools.Shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Нужно сделать:
 * 3 [Функционал]: Редактор аттрибутов.
 * 4 [Функционал]: Авто обновление таблицы файлов.
 * 5 [Функционал]: Реализовать возврат предыдущего выделения при переходе в родительский каталог,
 * но только на один уровень.
 * 6 [Функционал]: Не работает вырезание файлов.
 */
public class FM_GUI extends Application
{
    private Stage stage;
    private Scene scene;
    private VBox root;
    private VBox content_VBox;
    private BorderPane menu_BorderPane;
    private TextField currentPath_TextField;
    private TreeTableCell<FileData, String> treeTableCell;
    private TreeItem<FileData> rootItem;
    private ContextMenu contextMenuForColums;
    private TabPane content_TabPane;
    private TreeTableView<FileData> activatedTreeTableView;
    /**
     * Будет хранить ссылку на столбец с типом для выбранной таблицы
     */
    private CustomTreeTableColumn<FileData, String> currentNameColumn;
    private CustomTreeTableColumn<FileData, String> currentSizeColumn;

    /**
     * Нужен для правильной сортировки по размеру. Пока что играет исключительно
     * "техническую" роль. По умолчанию, эта колонка будет скрыта, чтобы лишний
     * раз не использовать ресурсы. В случае включения, данные в таблице будут
     * обновлены.
     */
    private CustomTreeTableColumn<FileData, Long> currentSizeBytesColumn;
    private CustomTreeTableColumn<FileData, Short> currentIsDirectoryColumn;
    private CustomTreeTableColumn<FileData, String> currentTypeColumn;
    private CustomTreeTableColumn<FileData, String> currentCreationTimeColumn;
    private CustomTreeTableColumn<FileData, String> currentOwnerColumn;
    private CustomTreeTableColumn<FileData, String> currentLastModifiedTimeColumn;


    public static final double rem = new Text("").getBoundsInParent().getHeight();
    private double preferredWidth = rem * 27.0D;
    private double preferredHeight = rem * 22.0D;
    public final String FIlE_STORES_PATH = "file stores:///";
    //private Path currentPath;
    private ArrayList<Path> currentPath;
    private TreeTableColumn.SortType sortType;
    private CustomTreeTableColumn<FileData, ?> lastActiveSortColumn;
    private boolean filesToMoveFromClipboard = false;
    private int currentContentTabIndex;
    private int previousSelectedFileIndex;
    private DateTimeFormatter dateTimeIsoFormatter;
    private double fileIconHeight;
    /**
     * Предназначен для хранения всех объектов TreeItem, чтобы
     * за каждым разом не создавать новые.
     */
    private ObservableList<TreeItem<FileData>> treeItemsSaver;
    private ArrayList<FileData> fileDataSaver;

    private MenuBar menu_Bar;
    private Menu file_Menu;
    private Menu goTo_Menu;
    private Menu view_Menu;
    private Menu sortBy_Menu;
    private RadioMenuItem sortByName_MenuItem;
    private RadioMenuItem sortBySize_MenuItem;
    private RadioMenuItem sortByType_MenuItem;
    private RadioMenuItem sortByOwner_MenuItem;
    private RadioMenuItem sortByCreationTime_MenuItem;
    private RadioMenuItem sortByLastModifiedTime_MenuItem;
    private CheckMenuItem directoriesFirst_MenuItem;
    private Menu edit_Menu;
    private MenuItem delete_MenuItem;
    private MenuItem rename_MenuItem;

    private ToggleGroup sortBy_ToggleGroup;
    private RadioMenuItem descendingSortType_MenuItem;
    private RadioMenuItem ascendingSortType_MenuItem;
    private ToggleGroup sortType_ToggleGroup;
    private MenuItem goToParent_MenuItem;
    private MenuItem goToRootDirectories_MenuItem;
    private MenuItem goToHomeDirectory_MenuItem;
    private MenuItem goToUserDirectory_MenuItem;
    private MenuItem exit_MenuItem;
    private MenuItem copyFileName_MenuItem;
    private MenuItem copyAbsoluteNamePath_MenuItem;
    private MenuItem createFile_MenuItem;
    private MenuItem createDirectory_MenuItem;
    private MenuItem createSymbolicLink_MenuItem;
    private MenuItem copy_MenuItem;
    private MenuItem paste_MenuItem;
    private MenuItem move_MenuItem;
    private ContextMenu contextMenuForFiles;
    private MenuItem createNewTab_MenuItem;
    private MenuItem goToPreviousTab_MenuItem;
    private MenuItem goToNextTab_MenuItem;
    private MenuItem showOrEditAttributes_MenuItem;
    private MenuItem openTerminalHere_MenuItem;
    private MenuItem openInNewTab_MenuItem;

    private CheckMenuItem autoSizeColumn_MenuItem;

    private ConfirmOperationDialog confirmOperationDialog;

    public final String TYPE_COLUMN_ID = "TYPE";
    public final String NAME_COLUMN_ID = "NAME";
    public final String SIZE_COLUMN_ID = "NAME";
    public final String OWNER_COLUMN_ID = "OWNER";
    public final String CREATION_TIME_COLUMN_ID = "CREATION_TIME";
    public final String LAST_MODIFIED_TIME_COLUMN_ID = "LAST_MODIFIED_TIME";
    public final String SIZE_BYTES_COLUMN_ID = "SIZE_BYTES";

    private Font cellFont;
    private Image folderIcon_Image;
    private Image fileIcon_Image;
    private Image brokenLink_Image;

    private FlowPane fileStorages_FlowPane;
    //private VBox currentContainerWithFiles_VBox;
    private ScrollPane fileStores_ScrollPane;
    private Label fileSystemName_Label;
    private HBox pathAndFileSystemName_HBox;

    private FileStoreInfoPopup fileStore_Popup;

    private ArrayList<TreeTableView<FileData>> allTreeTableViewWithFiles_ArrayList;
    private ArrayList<VBox> allContainersWithFiles_ArrayList;
    private ArrayList<CustomTreeTableColumn<FileData, Short>> allIsDirectoryColumns_ArrayList;

    private Color[] textColorForFileSystemName_Color;
    private Color[] borderColorForFileSystemName_Color;

    private Popup quickSearchInCurrentFolder_Popup;
    private TextField quickSearchKey_TextField;
    private DeleteFileDialog deleteFileDialog;
    private FileAttributesEditor fileAttributesEditor;
    private CreateSymbolicLinkDialog createSymbolicLinkDialog;
    private CopyFilesDialog copyFilesDialog;


    @Override
    public void start(Stage primaryStage) throws Exception
    {
        stage = primaryStage;

        root = new VBox(rem * 0.45D);
        //root.setPadding(new Insets(rem * 0.55D));

        scene = new Scene(root);
        initializeComponents();

        stage.setScene(scene);
        stage.setMinHeight(preferredHeight - 2);
        stage.setMinWidth(preferredWidth - 2);
        stage.setWidth(preferredWidth);
        stage.setHeight(preferredHeight);
        stage.show();
    }

    private void initializeComponents()
    {
        dateTimeIsoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");
        loadResources();
        initializeGeneral();
        initializeMenu();
        initializeContextMenuForColumns();
        initializeContextMenuForFiles();

        confirmOperationDialog = new ConfirmOperationDialog(StageStyle.UTILITY,
                "");
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.initOwner(stage);
        confirmOperationDialog.initModality(Modality.APPLICATION_MODAL);

        root.getChildren().addAll(menu_BorderPane, content_VBox);
        activatedTreeTableView.requestFocus();

        //goToDriveList();
        //-----------------
        List<String> rawParametersList = getParameters().getRaw();
        if (rawParametersList.size() > 1)
        {
            processArguments(rawParametersList);
        }
        else
        {
            goToFileStoragesPane();
            content_TabPane.getSelectionModel().getSelectedItem().setText(FIlE_STORES_PATH);
        }

    }

    private void initializeMenu()
    {
        exit_MenuItem = new MenuItem("Exit");
        exit_MenuItem.setOnAction(event ->
        {
            System.exit(0);
        });
        exit_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        copyFileName_MenuItem = new MenuItem("Copy name of file");
        copyFileName_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN));
        copyFileName_MenuItem.setOnAction(event ->
        {
            copyFileName_MenuItem_Action(event);
        });

        copyAbsoluteNamePath_MenuItem = new MenuItem("Copy absolute path of file");
        copyAbsoluteNamePath_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN,
                KeyCombination.ALT_DOWN));
        copyAbsoluteNamePath_MenuItem.setOnAction(event ->
        {
            copyAbsoluteNamePath_MenuItem_Action(event);
        });

        createNewTab_MenuItem = new MenuItem("Create new tab");
        createNewTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        createNewTab_MenuItem.setOnAction(this::createNewTab_Action);
        createNewTab_MenuItem.setDisable(true);

        openTerminalHere_MenuItem = new MenuItem("Open Terminal here");
        openTerminalHere_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
        openTerminalHere_MenuItem.setOnAction(this::addActionToOpenTerminalMenuItem);

        openInNewTab_MenuItem = new MenuItem("Open in new tab");
        openInNewTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openInNewTab_MenuItem.setOnAction(this::openInNewTab_Action);


        file_Menu = new Menu("Main");
        file_Menu.getItems().addAll(createNewTab_MenuItem, openInNewTab_MenuItem, new SeparatorMenuItem(),
                copyFileName_MenuItem, copyAbsoluteNamePath_MenuItem, new SeparatorMenuItem(),
                openTerminalHere_MenuItem, new SeparatorMenuItem(), exit_MenuItem);

        goToParent_MenuItem = new MenuItem("Parent directory");
        goToParent_MenuItem.setOnAction(event -> goToParentPath(currentPath.get(currentContentTabIndex)));
        goToParent_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCodeCombination.CONTROL_DOWN));

        goToRootDirectories_MenuItem = new MenuItem("Root directories");
        goToRootDirectories_MenuItem.setOnAction(event ->
        {
            //goToDriveList();
            goToFileStoragesPane();
        });
        goToRootDirectories_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCodeCombination.ALT_DOWN));

        goToHomeDirectory_MenuItem = new MenuItem("Home directory");
        goToHomeDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.home");
            goToPath(Paths.get(userDirectory));
        });
        goToHomeDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCodeCombination.ALT_DOWN));

        goToUserDirectory_MenuItem = new MenuItem("User directory");
        goToUserDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.dir");
            goToPath(Paths.get(userDirectory));
        });
        goToUserDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCodeCombination.ALT_DOWN));

        goToPreviousTab_MenuItem = new MenuItem("Previous tab");
        goToPreviousTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.PAGE_UP, KeyCombination.CONTROL_DOWN));
        goToPreviousTab_MenuItem.setOnAction(this::goToPreviousTab_Action);

        goToNextTab_MenuItem = new MenuItem("Next tab");
        goToNextTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.PAGE_DOWN, KeyCombination.CONTROL_DOWN));
        goToNextTab_MenuItem.setOnAction(this::goToNextTab_Action);

        goTo_Menu = new Menu("Go to...");
        goTo_Menu.getItems().addAll(goToRootDirectories_MenuItem, goToHomeDirectory_MenuItem,
                goToUserDirectory_MenuItem, goToParent_MenuItem, new SeparatorMenuItem(),
                goToPreviousTab_MenuItem, goToNextTab_MenuItem);

        sortBy_ToggleGroup = new ToggleGroup();
        sortType_ToggleGroup = new ToggleGroup();

        sortByName_MenuItem = new RadioMenuItem("by Name");
        sortByName_MenuItem.setSelected(true);
        sortByName_MenuItem.setOnAction(event -> requestSort(currentNameColumn, currentTypeColumn));
        sortByName_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortBySize_MenuItem = new RadioMenuItem("by Size");
        sortBySize_MenuItem.setOnAction(event -> requestSort(currentSizeBytesColumn, currentTypeColumn));
        sortBySize_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortByType_MenuItem = new RadioMenuItem("by Type");
        sortByType_MenuItem.setOnAction(event -> requestSort(currentTypeColumn, currentTypeColumn));
        sortByType_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByType_MenuItem.setDisable(true);

        sortByOwner_MenuItem = new RadioMenuItem("by Owner");
        sortByOwner_MenuItem.setOnAction(event -> requestSort(currentOwnerColumn, currentTypeColumn));
        sortByOwner_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByOwner_MenuItem.setDisable(true);
        sortByOwner_MenuItem.disableProperty().bind(currentOwnerColumn.visibleProperty().not());

        sortByCreationTime_MenuItem = new RadioMenuItem("by Creation Time");
        sortByCreationTime_MenuItem.setOnAction(event -> requestSort(currentCreationTimeColumn, currentTypeColumn));
        sortByCreationTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByCreationTime_MenuItem.setDisable(true);
        sortByCreationTime_MenuItem.disableProperty().bind(currentCreationTimeColumn.visibleProperty().not());

        sortByLastModifiedTime_MenuItem = new RadioMenuItem("by Last modified Time");
        sortByLastModifiedTime_MenuItem.setOnAction(event -> requestSort(currentLastModifiedTimeColumn, currentTypeColumn));
        sortByLastModifiedTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByLastModifiedTime_MenuItem.setDisable(true);
        sortByLastModifiedTime_MenuItem.disableProperty().bind(currentLastModifiedTimeColumn.visibleProperty().not());

        ascendingSortType_MenuItem = new RadioMenuItem("Ascending");
        ascendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        ascendingSortType_MenuItem.setSelected(true);
        ascendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.ASCENDING;
            requestSort(lastActiveSortColumn, currentTypeColumn);
        });

        descendingSortType_MenuItem = new RadioMenuItem("Descending");
        descendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        descendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.DESCENDING;
            requestSort(lastActiveSortColumn, currentTypeColumn);
        });

        directoriesFirst_MenuItem = new CheckMenuItem("Directories first");
        directoriesFirst_MenuItem.setSelected(true);
        directoriesFirst_MenuItem.setOnAction(event -> requestSort(lastActiveSortColumn,
                currentTypeColumn));
        directoriesFirst_MenuItem.selectedProperty().addListener(event ->
        {
            if (directoriesFirst_MenuItem.isSelected())
            {
                sortByType_MenuItem.setDisable(true);
                sortByType_MenuItem.setSelected(false);
            }
            else
            {
                sortByType_MenuItem.setDisable(false);
                sortByType_MenuItem.setSelected(false);
            }
        });

        sortByType_MenuItem.selectedProperty().addListener(event ->
        {
            if (sortByType_MenuItem.isSelected())
            {
                directoriesFirst_MenuItem.setDisable(true);
                directoriesFirst_MenuItem.setSelected(false);
            }
            else
            {
                directoriesFirst_MenuItem.setDisable(false);
                directoriesFirst_MenuItem.setSelected(false);
            }
        });

        sortBy_Menu = new Menu("Sort by");
        sortBy_Menu.getItems().addAll(directoriesFirst_MenuItem, new SeparatorMenuItem(), sortByName_MenuItem,
                sortByType_MenuItem, sortBySize_MenuItem, sortByOwner_MenuItem,
                sortByCreationTime_MenuItem, sortByLastModifiedTime_MenuItem, new SeparatorMenuItem(),
                ascendingSortType_MenuItem, descendingSortType_MenuItem, new SeparatorMenuItem());


        edit_Menu = new Menu("Edit");

        delete_MenuItem = new MenuItem("Delete");
        delete_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        delete_MenuItem.setOnAction(this::delete_MenuItem_Action);

        rename_MenuItem = new MenuItem("Rename");
        rename_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        rename_MenuItem.setOnAction(this::rename_MenuItem_Action);

        createFile_MenuItem = new MenuItem("Create file");
        createFile_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        createFile_MenuItem.setOnAction(this::createFile_MenuItem_Action);

        createDirectory_MenuItem = new MenuItem("Create directory");
        createDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        createDirectory_MenuItem.setOnAction(this::createDirectory_MenuItem_Action);

        createSymbolicLink_MenuItem = new MenuItem("Create symbolic link");
        createSymbolicLink_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        createSymbolicLink_MenuItem.setOnAction(this::createSymbolicLink_MenuItem_Action);

        copy_MenuItem = new MenuItem("Copy");
        copy_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copy_MenuItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = false;
        });

        paste_MenuItem = new MenuItem("Paste");
        paste_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        paste_MenuItem.setOnAction(this::pasteFiles_MenuItem_Action);

        move_MenuItem = new MenuItem("Move");
        move_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        move_MenuItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = true;
        });
        move_MenuItem.setVisible(false);

        showOrEditAttributes_MenuItem = new MenuItem("Attributes");
        showOrEditAttributes_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F4));
        showOrEditAttributes_MenuItem.setOnAction(this::showOrEditAttributes_Action);

        edit_Menu.getItems().addAll(showOrEditAttributes_MenuItem, new SeparatorMenuItem(),
                createFile_MenuItem, createDirectory_MenuItem, createSymbolicLink_MenuItem,
                new SeparatorMenuItem(), copy_MenuItem, move_MenuItem, paste_MenuItem,
                new SeparatorMenuItem(), rename_MenuItem, delete_MenuItem);

        menu_Bar = new MenuBar(file_Menu, edit_Menu, goTo_Menu, sortBy_Menu);

        menu_BorderPane = new BorderPane();
        menu_BorderPane.setTop(menu_Bar);
    }

    private void initializeGeneral()
    {
        fileIconHeight = 22.0D;

        CustomTreeTableColumn<FileData, String> nameColumn = new CustomTreeTableColumn<>("Name");
        nameColumn.setMinWidth(preferredWidth * 0.1D);
        nameColumn.setPrefWidth(preferredWidth * 0.5D);
        nameColumn.setSortable(false);
        nameColumn.setId(NAME_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> sizeColumn = new CustomTreeTableColumn<>("Size");
        sizeColumn.setMinWidth(preferredWidth * 0.05D);
        sizeColumn.setPrefWidth(preferredWidth * 0.1D);
        sizeColumn.setSortable(false);
        sizeColumn.setId(SIZE_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> ownerColumn = new CustomTreeTableColumn<>("Owner");
        ownerColumn.setMinWidth(preferredWidth * 0.1D);
        ownerColumn.setSortable(false);
        ownerColumn.setVisible(false);
        ownerColumn.setId(OWNER_COLUMN_ID);
        ownerColumn.visibleProperty().addListener(event ->
        {
            if (ownerColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByOwner_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
                sortByOwner_MenuItem.setSelected(false);
            }
        });

        CustomTreeTableColumn<FileData, String> lastModifiedTimeColumn = new CustomTreeTableColumn<>("Last modified time");
        lastModifiedTimeColumn.setMinWidth(preferredWidth * 0.15D);
        lastModifiedTimeColumn.setSortable(false);
        lastModifiedTimeColumn.setVisible(false);
        lastModifiedTimeColumn.setId(LAST_MODIFIED_TIME_COLUMN_ID);
        lastModifiedTimeColumn.visibleProperty().addListener(event ->
        {
            if (lastModifiedTimeColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByLastModifiedTime_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
                sortByLastModifiedTime_MenuItem.setSelected(false);
            }
        });

        CustomTreeTableColumn<FileData, String> creationTimeColumn = new CustomTreeTableColumn<>("Creation time");
        creationTimeColumn.setMinWidth(preferredWidth * 0.15D);
        creationTimeColumn.setSortable(false);
        creationTimeColumn.setVisible(false);
        creationTimeColumn.setId(CREATION_TIME_COLUMN_ID);
        creationTimeColumn.visibleProperty().addListener(event ->
        {
            if (creationTimeColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByCreationTime_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
                sortByCreationTime_MenuItem.setSelected(false);
            }
        });

        CustomTreeTableColumn<FileData, String> typeColumn = new CustomTreeTableColumn<>("Type");
        typeColumn.setMinWidth(preferredWidth * 0.1D);
        typeColumn.setSortable(false);
        typeColumn.setPrefWidth(preferredWidth * 0.15D);
        typeColumn.setId(TYPE_COLUMN_ID);
        typeColumn.visibleProperty().addListener(event ->
        {
            if (typeColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
        });

        CustomTreeTableColumn<FileData, Long> sizeBytesColumn = new CustomTreeTableColumn<>("Size (bytes)");
        sizeBytesColumn.setMinWidth(preferredWidth * 0.1D);
        sizeBytesColumn.setSortable(false);
        sizeBytesColumn.setPrefWidth(preferredWidth * 0.15D);
        sizeBytesColumn.setId(SIZE_COLUMN_ID);
        sizeBytesColumn.setVisible(false);

        CustomTreeTableColumn<FileData, Short> isDirectoryColumn = new CustomTreeTableColumn<>("is Directory");
        isDirectoryColumn.setMinWidth(preferredWidth * 0.1D);
        isDirectoryColumn.setSortable(false);
        isDirectoryColumn.setPrefWidth(preferredWidth * 0.15D);
        isDirectoryColumn.setId("");
        isDirectoryColumn.setVisible(false);


        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
        sizeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getSize(true)));
        ownerColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getOwner()));
        lastModifiedTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getLastModifiedTime(dateTimeIsoFormatter)));
        creationTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getCreationTime(dateTimeIsoFormatter)));
        typeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getType()));
        sizeBytesColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Long> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().getSize()));
        isDirectoryColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Short> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().isDirectory(true)));

        rootItem = new TreeItem<>(new FileData("root", 4096L));

        TreeTableView<FileData> content_TreeTableView = new TreeTableView<>();

        /*Сделано, чтобы F2 захватывалась в таблице файлов. Потенциально опасно -
         * иногда приводит к ошибкам JRE!!!*/
//        content_TreeTableView.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>()
//        {
//            @Override
//            public void handle(KeyEvent keyEvent)
//            {
//                if(keyEvent.getCode() == KeyCode.F2)
//                {
//                    Runnable r = scene.getAccelerators().get(rename_MenuItem.getAccelerator());
//                    if (r != null)
//                    {
//                        r.run();
//                    }
//                }
//            }
//        });

        content_TreeTableView.getColumns().addAll(nameColumn, typeColumn, sizeColumn, ownerColumn,
                creationTimeColumn, lastModifiedTimeColumn);
        content_TreeTableView.setRoot(rootItem);
        content_TreeTableView.setShowRoot(false);
        content_TreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        content_TreeTableView.setOnKeyReleased(this::contentKeyPressed_Action);
        content_TreeTableView.getSortOrder().addAll(nameColumn, typeColumn);
        sortType = TreeTableColumn.SortType.ASCENDING;
        lastActiveSortColumn = nameColumn;
        content_TreeTableView.setTableMenuButtonVisible(true);

        EventHandler<MouseEvent> mouseClickEvent = new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent mouseEvent)
            {
                cellMouseClicked_Action(mouseEvent);
            }
        };


        nameColumn.setCellFactory(new Callback<TreeTableColumn<FileData, String>, TreeTableCell<FileData, String>>()
        {
            @Override
            public TreeTableCell<FileData, String> call(TreeTableColumn<FileData, String> fileDataStringTreeTableColumn)
            {
                CustomTreeTableCell<FileData, String> temporaryCell = new CustomTreeTableCell<>();
                //temporaryCell.setTextFill(Color.LIGHTSKYBLUE);
                //temporaryCell.setFont(cellFont);
                temporaryCell.setPadding(new Insets(0.0D, 0.0D, 0.0, rem * 0.2D));
                temporaryCell.setOnMouseClicked(mouseClickEvent);
                return temporaryCell;
            }
        });
        //nameColumn.setAutoFitColumnWidthToData(true);

        currentPath_TextField = new TextField();
        currentPath_TextField.setOnKeyReleased(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                System.out.println("Enter нажата.");

                Path newPath = Paths.get(currentPath_TextField.getText());
                System.out.println("Новый путь: " + newPath);
                goToPath(newPath);
            }
        });

        textColorForFileSystemName_Color = new Color[]{Color.MEDIUMSEAGREEN,
                Color.DARKVIOLET};
        borderColorForFileSystemName_Color = new Color[]{Color.MEDIUMAQUAMARINE,
                Color.MEDIUMORCHID};

        fileSystemName_Label = new Label("FS");
        fileSystemName_Label.setTextFill(Color.MEDIUMSEAGREEN);
        fileSystemName_Label.setBorder(new Border(new BorderStroke(Color.MEDIUMAQUAMARINE,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3.0D))));
        fileSystemName_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 14.0D));
        fileSystemName_Label.setAlignment(Pos.CENTER);
        fileSystemName_Label.setPadding(new Insets(0.0D, 4.0D, 0.0D, 4.0D));
        fileSystemName_Label.setOnMouseClicked(this::fileSystemName_MouseEnteredAction);

        pathAndFileSystemName_HBox = new HBox(rem * 0.35D, currentPath_TextField,
                fileSystemName_Label);
        HBox.setHgrow(currentPath_TextField, Priority.ALWAYS);

        Tab main_Tab = new Tab();
        main_Tab.setClosable(false);

        currentContentTabIndex = 0;

        content_TabPane = new TabPane(main_Tab);
        content_TabPane.setPadding(new Insets(rem * 0.15D));
        content_TabPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        content_TabPane.getSelectionModel().selectedItemProperty().addListener(event ->
        {
            System.out.println("Вкладка изменена. Текущий индекс: "
                    + content_TabPane.getSelectionModel().getSelectedIndex());
            currentContentTabIndex = content_TabPane.getSelectionModel().getSelectedIndex();

            /*Нужно затереть с предыдущей таблицы, иначе пропадут значки.
             * Внимание: актуально только для текущего поведения. В данном
             * варианте при переходе на каждую вкладку элементы таблиц удаляются
             * из нее, а объекты используются повторно. Это дает ощутимый выигрыш
             * в скорости работы.*/
            activatedTreeTableView.getRoot().getChildren().clear();

            activatedTreeTableView = allTreeTableViewWithFiles_ArrayList.get(currentContentTabIndex);
            currentNameColumn = findColumnByStringID(NAME_COLUMN_ID);
            currentTypeColumn = findColumnByStringID(TYPE_COLUMN_ID);
            currentOwnerColumn = findColumnByStringID(OWNER_COLUMN_ID);
            currentCreationTimeColumn = findColumnByStringID(CREATION_TIME_COLUMN_ID);
            currentLastModifiedTimeColumn = findColumnByStringID(LAST_MODIFIED_TIME_COLUMN_ID);
            //----------------

            configureSortPropertiesByMenuItems();

            goToPath(currentPath.get(currentContentTabIndex));

            Platform.runLater(() ->
            {
                activatedTreeTableView.requestFocus();
            });
        });

        currentPath = new ArrayList<>();
        allContainersWithFiles_ArrayList = new ArrayList<>();
        allIsDirectoryColumns_ArrayList = new ArrayList<>();
        allTreeTableViewWithFiles_ArrayList = new ArrayList<>();
        allTreeTableViewWithFiles_ArrayList.add(content_TreeTableView);
        treeItemsSaver = FXCollections.observableArrayList();
        fileDataSaver = new ArrayList<>();

        activatedTreeTableView = content_TreeTableView;
        currentNameColumn = nameColumn;
        currentSizeColumn = sizeColumn;
        currentOwnerColumn = ownerColumn;
        currentCreationTimeColumn = creationTimeColumn;
        currentLastModifiedTimeColumn = lastModifiedTimeColumn;
        currentTypeColumn = typeColumn;
        currentSizeBytesColumn = sizeBytesColumn;
        currentIsDirectoryColumn = isDirectoryColumn;

        fileStorages_FlowPane = new FlowPane(rem * 1.5D, rem * 0.7D);
        fileStorages_FlowPane.setPadding(new Insets(rem * 1.2D, rem * 0.5D, rem * 0.80D,
                rem * 0.5D));
        fileStorages_FlowPane.setAlignment(Pos.CENTER);

        fileStores_ScrollPane = new ScrollPane();
        fileStores_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileStores_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fileStores_ScrollPane.setFitToWidth(true);
        fileStores_ScrollPane.setFitToHeight(true);
        fileStores_ScrollPane.setContent(fileStorages_FlowPane);

        VBox files_VBox = new VBox(rem * 0.45D);
        files_VBox.getChildren().add(fileStores_ScrollPane);
//        files_VBox.setBackground(new Background(new BackgroundFill(Color.BLACK,
//                CornerRadii.EMPTY, Insets.EMPTY)));


        main_Tab.setContent(files_VBox);

        allContainersWithFiles_ArrayList.add(files_VBox);
        allIsDirectoryColumns_ArrayList.add(currentIsDirectoryColumn);

        content_VBox = new VBox(rem * 0.45D);
        content_VBox.setPadding(new Insets(rem * 0.15D, rem * 0.7D, rem * 0.7D, rem * 0.7D));
        content_VBox.getChildren().addAll(pathAndFileSystemName_HBox, content_TabPane);
        VBox.setVgrow(content_VBox, Priority.ALWAYS);
        VBox.setVgrow(content_TabPane, Priority.ALWAYS);
        VBox.setVgrow(files_VBox, Priority.ALWAYS);
        VBox.setVgrow(content_TreeTableView, Priority.ALWAYS);

    }

    private void goToFileStoragesPane()
    {
        fileStorages_FlowPane.getChildren().clear();

        VBox temporaryContainerWithFiles_VBox = allContainersWithFiles_ArrayList.get(currentContentTabIndex);
        temporaryContainerWithFiles_VBox.getChildren().clear();
        temporaryContainerWithFiles_VBox.getChildren().add(fileStores_ScrollPane);

        FileSystem defaultFileSystem = FileSystems.getDefault();

        String temporaryFileStorageName = null;
        String temporaryFileStorageFirstName = null;
        String temporaryFileStorageSecondName = null;
        String buttonName = null;

        boolean defineMethod = true;
        boolean useFirstName = false;
        boolean useSecondName = false;
        boolean useSecondAndSeparatorName = false;
        Path resultPath = null;

        for (FileStore fileStore : FileSystems.getDefault().getFileStores())
        {
            temporaryFileStorageName = fileStore.toString();

            if (temporaryFileStorageName.indexOf("(") == 0)
            {
                temporaryFileStorageFirstName = "";
            }
            else
            {
                temporaryFileStorageFirstName = temporaryFileStorageName.substring(0,
                        temporaryFileStorageName.indexOf("(") - 1);
            }
            temporaryFileStorageSecondName = temporaryFileStorageName.substring(
                    temporaryFileStorageName.indexOf("(") + 1, temporaryFileStorageName.indexOf(")"));
            System.out.println("First: '" + temporaryFileStorageFirstName + "'");
            System.out.println("Second: '" + temporaryFileStorageSecondName + "'");

            //----------------------------
            if (defineMethod)
            {
                File temporaryFile = new File(temporaryFileStorageFirstName);
                if (temporaryFile.exists())
                {
                    useFirstName = true;
                    buttonName = temporaryFileStorageFirstName;
                    System.out.println("Существует");
                }
                else
                {
                    useFirstName = false;
                    System.out.println("Не существует");
                }

                //Скорее всего Windows гафно!
                if (!useFirstName)
                {
                    temporaryFile = new File(temporaryFileStorageSecondName + defaultFileSystem.getSeparator());
                    if (temporaryFile.exists())
                    {
                        useSecondName = true;
                        buttonName = temporaryFileStorageName;
                        System.out.println("Существует");
                    }
                    else
                    {
                        useSecondName = false;
                        System.out.println("Не существует");
                    }
                }
                //Скорее всего Windows, но нужно добавить разделитель
                if (!useSecondName && !useFirstName)
                {
                    temporaryFile = new File(temporaryFileStorageSecondName
                            + defaultFileSystem.getSeparator());
                    if (temporaryFile.exists())
                    {
                        useSecondAndSeparatorName = true;
                        buttonName = temporaryFileStorageName;
                        System.out.println("Существует");
                    }
                    else
                    {
                        useSecondAndSeparatorName = false;
                        System.out.println("Несуществууут");
                    }
                }
                Path temporaryPath = temporaryFile.toPath();
                System.out.println("temporaryPath: " + temporaryPath);
                System.out.println("temporaryPathAbsolute: " + temporaryPath.toAbsolutePath());

                resultPath = temporaryPath.toAbsolutePath();
                System.out.println("Result: " + resultPath);
                defineMethod = false;
            }
            else
            {
                Path temporaryPath = null;

                if (useFirstName)
                {
                    temporaryPath = new File(temporaryFileStorageFirstName).toPath();
                    buttonName = temporaryFileStorageFirstName;
                }
                else if (useSecondName)
                {
                    temporaryPath = new File(temporaryFileStorageSecondName).toPath();
                    buttonName = temporaryFileStorageName;
                }
                else if (useSecondAndSeparatorName)
                {
                    temporaryPath = new File(temporaryFileStorageSecondName + defaultFileSystem.getSeparator()).toPath();
                    buttonName = temporaryFileStorageName;
                }
//                System.out.println("ResultPath: " + temporaryPath.toString());
//                System.out.println("ResultPath: " + temporaryPath.toAbsolutePath().toString());
                resultPath = temporaryPath.toAbsolutePath();
                System.out.println("Result: " + resultPath);
            }


            FileStorageButton temporaryFileStore_Button = new FileStorageButton(buttonName,
                    resultPath);
            temporaryFileStore_Button.setOnAction(event ->
            {
                FileStorageButton temporaryButton = (FileStorageButton) event.getSource();
                goToPath(temporaryButton.getFileStoragePath());
                temporaryContainerWithFiles_VBox.getChildren().clear();
                temporaryContainerWithFiles_VBox.getChildren().add(activatedTreeTableView);
            });
            fileStorages_FlowPane.getChildren().add(temporaryFileStore_Button);
            //==================================================== Удалить!!!!
            //break;
        }


//        for (FileStore fileStore : FileSystems.getDefault().getFileStores())
//        {
//            temporaryFileStorageName = fileStore.toString();
//            System.out.println("======"
//                    + "\nFileStore.toString(): " + fileStore.toString()
//                    + "\nFileStore.name(): " + fileStore.name());
//
//
//
//
//        }

//        Path targetPath = null;
//
//
//
//        FileStorageButton temporaryFileStore_Button = new FileStorageButton(temporaryFileStorageFirstName,
//                targetPath);
//        temporaryFileStore_Button.setOnAction(event ->
//        {
//            FileStorageButton temporaryButton = (FileStorageButton) event.getSource();
//            goToPath(temporaryButton.getFileStoragePath());
//            temporaryContainerWithFiles_VBox.getChildren().clear();
//            temporaryContainerWithFiles_VBox.getChildren().add(activatedTreeTableView);
//        });
//        fileStorages_FlowPane.getChildren().add(temporaryFileStore_Button);

        fileSystemName_Label.setText("?");
        currentPath_TextField.setText(FIlE_STORES_PATH);
        content_TabPane.getTabs().get(currentContentTabIndex).setText(FIlE_STORES_PATH);

        openTerminalHere_MenuItem.setDisable(true);
    }

    private ArrayList<Path> defineDrives()
    {
        FileSystem defaultFileSystem = FileSystems.getDefault();

        String temporaryFileStorageName = null;
        String temporaryFileStorageFirstName = null;
        String temporaryFileStorageSecondName = null;

        boolean defineMethod = true;
        boolean useFirstName = false;
        boolean useSecondName = false;
        boolean useSecondAndSeparatorName = false;

        ArrayList<Path> resultPaths = new ArrayList<>();

        for (FileStore fileStore : FileSystems.getDefault().getFileStores())
        {
            temporaryFileStorageName = fileStore.toString();

            if (temporaryFileStorageName.indexOf("(") == 0)
            {
                temporaryFileStorageFirstName = "";
            }
            else
            {
                temporaryFileStorageFirstName = temporaryFileStorageName.substring(0,
                        temporaryFileStorageName.indexOf("(") - 1);
            }
            temporaryFileStorageSecondName = temporaryFileStorageName.substring(
                    temporaryFileStorageName.indexOf("(") + 1, temporaryFileStorageName.indexOf(")"));
            System.out.println("First: '" + temporaryFileStorageFirstName + "'");
            System.out.println("Second: '" + temporaryFileStorageSecondName + "'");

            //----------------------------
            if (defineMethod)
            {
                File temporaryFile = new File(temporaryFileStorageFirstName);
                if (temporaryFile.exists())
                {
                    useFirstName = true;
                    System.out.println("Существует");
                }
                else
                {
                    useFirstName = false;
                    System.out.println("Несуществууут");
                }

                //Скорее всего Windows
                if (!useFirstName)
                {
                    temporaryFile = new File(temporaryFileStorageSecondName);
                    if (temporaryFile.exists())
                    {
                        useSecondName = true;
                        System.out.println("Существует");
                    }
                    else
                    {
                        useSecondName = false;
                        System.out.println("Несуществууут");
                    }
                }
                //Скорее всего Windows, но нужно добавить разделитель
                if (!useSecondName)
                {
                    temporaryFile = new File(temporaryFileStorageFirstName
                            + defaultFileSystem.getSeparator());
                    if (temporaryFile.exists())
                    {
                        useSecondAndSeparatorName = true;
                        System.out.println("Существует");
                    }
                    else
                    {
                        useSecondAndSeparatorName = false;
                        System.out.println("Несуществууут");
                    }
                }
                defineMethod = false;
            }
            else
            {
                Path temporaryPath = null;

                if (useFirstName)
                {
                    temporaryPath = new File(temporaryFileStorageFirstName).toPath();
                }
                else if (useSecondName)
                {
                    temporaryPath = new File(temporaryFileStorageSecondName).toPath();
                }
                else if (useSecondAndSeparatorName)
                {
                    temporaryPath = new File(temporaryFileStorageSecondName + defaultFileSystem.getSeparator()).toPath();
                }
//                System.out.println("ResultPath: " + temporaryPath.toString());
//                System.out.println("ResultPath: " + temporaryPath.toAbsolutePath().toString());
                temporaryPath = temporaryPath.toAbsolutePath();
                System.out.println("Result: " + temporaryPath);
                resultPaths.add(temporaryPath);
            }

        }
        return resultPaths;
    }

    private void initializeContextMenuForColumns()
    {
        contextMenuForColums = new ContextMenu();
        contextMenuForColums.setOnShowing(event ->
        {
            System.out.println("Отображается Контекстное меню.");
            TableColumnHeader tableColumnHeader = (TableColumnHeader) contextMenuForColums.getOwnerNode();
            TableColumnBase tableColumnBase = tableColumnHeader.getTableColumn();
            CustomTreeTableColumn temporaryColumn = (CustomTreeTableColumn) tableColumnBase;
            System.out.println(temporaryColumn.toString());

            autoSizeColumn_MenuItem.setSelected(temporaryColumn.isAutoFitSizeColumn());
        });

        autoSizeColumn_MenuItem = new CheckMenuItem("Auto size");
        autoSizeColumn_MenuItem.setOnAction(this::contextMenuForColumns_Action);
        contextMenuForColums.getItems().addAll(autoSizeColumn_MenuItem);

        currentNameColumn.setContextMenu(contextMenuForColums);
        currentSizeColumn.setContextMenu(contextMenuForColums);
        currentOwnerColumn.setContextMenu(contextMenuForColums);
        currentCreationTimeColumn.setContextMenu(contextMenuForColums);
        currentLastModifiedTimeColumn.setContextMenu(contextMenuForColums);
        currentTypeColumn.setContextMenu(contextMenuForColums);
    }

    private void initializeContextMenuForFiles()
    {
        contextMenuForFiles = new ContextMenu();

        /*Нужно создавать новые обьекты. Старые не отображаются.*/
        MenuItem createFile_contextMenuForFilesItem = new MenuItem("Create File");
        createFile_contextMenuForFilesItem.setOnAction(this::createFile_MenuItem_Action);

        MenuItem createDirectory_contextMenuForFilesItem = new MenuItem("Create Directory");
        createDirectory_contextMenuForFilesItem.setOnAction(this::createDirectory_MenuItem_Action);

        MenuItem openInNewTab_contextMenuForFilesItem = new MenuItem("Open in new tab");
        openInNewTab_contextMenuForFilesItem.setOnAction(this::openInNewTab_Action);

        MenuItem openInCustomProgram_contextMenuForFilesItem = new MenuItem("Open with ...");
        openInCustomProgram_contextMenuForFilesItem.setOnAction(this::openWithCustomProgram_Action);

        MenuItem openInDefaultProgram_contextMenuForFilesItem = new MenuItem("Open");
        openInDefaultProgram_contextMenuForFilesItem.setOnAction(this::openInDefaultProgram_Action);

        MenuItem createSymbolicLink_contextMenuForFilesItem = new MenuItem("Create Symbolic link");
        createSymbolicLink_contextMenuForFilesItem.setOnAction(this::createSymbolicLink_MenuItem_Action);

        MenuItem copyFileToClipboard_contextMenuForFilesItem = new MenuItem("Copy");
        copyFileToClipboard_contextMenuForFilesItem.setOnAction(this::copyFilesToClipboard_MenuItem_Action);

        MenuItem moveFilesToClipboard_contextMenuForFilesItem = new MenuItem("Move");
        moveFilesToClipboard_contextMenuForFilesItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = true;
        });
        moveFilesToClipboard_contextMenuForFilesItem.setDisable(true);

        MenuItem pasteFileFromClipboard_contextMenuForFilesItem = new MenuItem("Paste");
        pasteFileFromClipboard_contextMenuForFilesItem.setOnAction(this::pasteFiles_MenuItem_Action);


        MenuItem renameFile_contextMenuForFilesItem = new MenuItem("Rename");
        renameFile_contextMenuForFilesItem.setOnAction(this::rename_MenuItem_Action);
        MenuItem deleteFile_contextMenuForFilesItem = new MenuItem("Delete");
        deleteFile_contextMenuForFilesItem.setOnAction(this::delete_MenuItem_Action);

        contextMenuForFiles.getItems().addAll(openInDefaultProgram_contextMenuForFilesItem, openInCustomProgram_contextMenuForFilesItem,
                openInNewTab_contextMenuForFilesItem, new SeparatorMenuItem(),
                createFile_contextMenuForFilesItem,
                createDirectory_contextMenuForFilesItem, createSymbolicLink_contextMenuForFilesItem,
                new SeparatorMenuItem(), copyFileToClipboard_contextMenuForFilesItem,
                moveFilesToClipboard_contextMenuForFilesItem, pasteFileFromClipboard_contextMenuForFilesItem,
                renameFile_contextMenuForFilesItem,
                deleteFile_contextMenuForFilesItem);

        activatedTreeTableView.setContextMenu(contextMenuForFiles);
    }

    private void loadResources()
    {
        long startTime = System.currentTimeMillis();
        cellFont = Font.loadFont(FM_GUI.class.getResourceAsStream("/Fonts/Leto Text Sans Defect.otf"), 14.0D);

        folderIcon_Image = new Image(FM_GUI.class.getResourceAsStream("/Images/folder.png"));
        fileIcon_Image = new Image(FM_GUI.class.getResourceAsStream("/Images/file.png"));
        brokenLink_Image = new Image(FM_GUI.class.getResourceAsStream("/Images/brokenLink_2.png"));


        System.out.println("На загрузку ресурсов потрачено: " + (System.currentTimeMillis() - startTime));
    }

    private void contextMenuForColumns_Action(ActionEvent event)
    {
        TableColumnHeader tableColumnHeader = (TableColumnHeader) contextMenuForColums.getOwnerNode();
        CustomTreeTableColumn<FileData, String> temporaryColumn = (CustomTreeTableColumn<FileData, String>) tableColumnHeader.getTableColumn();
        System.out.println(temporaryColumn.getText());
        CheckMenuItem source = (CheckMenuItem) event.getSource();

        if (source.isSelected())
        {
            temporaryColumn.setAutoFitColumnWidthToData(true);
        }
        else
        {
            temporaryColumn.setAutoFitColumnWidthToData(false);
        }
    }

    /**
     * Позволяет перейти к определенному пути. Обновляет содержимое таблицы файлов.
     */
    public boolean goToPath(Path destinationPath)
    {
        if (destinationPath == null)
        {
            System.out.println("Пустой путь.");
            return false;
        }
        if (!Files.exists(destinationPath))
        {
            System.out.println("Данного пути не существует.");
            return false;
        }
        if (Files.isRegularFile(destinationPath))
        {
            System.out.println("Переход отменен. Это файл!");
            return false;
        }
        if (Files.isSymbolicLink(destinationPath))
        {
            try
            {
                //destinationPath = Files.readSymbolicLink(destinationPath).toAbsolutePath();
                destinationPath = currentPath.get(currentContentTabIndex).resolve(Files.readSymbolicLink(destinationPath));
                destinationPath = destinationPath.toAbsolutePath();
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }

        if (!activatedTreeTableView.isTableMenuButtonVisible())
        {
            activatedTreeTableView.setTableMenuButtonVisible(true);
        }
        System.out.println("destination: " + destinationPath);

        currentPath_TextField.setText(destinationPath.toAbsolutePath().toString());
        currentPath_TextField.end();

        if (currentPath.size() != 0)
        {
            currentPath.remove(currentContentTabIndex);
        }
        currentPath.add(currentContentTabIndex, destinationPath);

        if (createNewTab_MenuItem.isDisable())
        {
            createNewTab_MenuItem.setDisable(false);
        }
        if (openTerminalHere_MenuItem.isDisable())
        {
            openTerminalHere_MenuItem.setDisable(false);
        }

        //Скорее всего из-за обращения к корневому каталогу /
        if (destinationPath.getFileName() == null)
        {
            content_TabPane.getSelectionModel().getSelectedItem().setText(destinationPath.toAbsolutePath().toString());
        }
        else
        {
            content_TabPane.getSelectionModel().getSelectedItem().setText(destinationPath.getFileName().toString());
        }

        try
        {
            updateFilesContent(destinationPath, activatedTreeTableView.getRoot());
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

        VBox temporaryContainerWithFiles_VBox = allContainersWithFiles_ArrayList.get(currentContentTabIndex);

        if (!temporaryContainerWithFiles_VBox.getChildren().contains(activatedTreeTableView))
        {
            System.out.println("Добавляем таблицу");
            temporaryContainerWithFiles_VBox.getChildren().clear();
            temporaryContainerWithFiles_VBox.getChildren().add(activatedTreeTableView);
        }

        changeFileSystemLabelTextAndColor();
        //registerWatchService(destinationPath);

        return true;
    }

    /**
     * Обновляет всю таблицу файлов исходя из текущего пути создавая новые объекты.
     * Также устанавливает выделение на первый элемент в таблице, если он есть.
     */
    private boolean updateFilesContent(final Path destinationPath, TreeItem<FileData> currentRootItem) throws IOException
    {
        long startTime = System.currentTimeMillis();

        currentRootItem.getChildren().clear();

        String[] directoryItems = destinationPath.toFile().list();

        FileData temporaryFileData = null;
        int treeItemIndex = 0;
        int fileDataIndex = 0;

        for (String temporaryItem : directoryItems)
        {
            Path temporaryPath = destinationPath.resolve(temporaryItem);

            try
            {
                //temporaryFileData = new FileData(temporaryPath.getFileName().toString(), Files.size(temporaryPath));
                temporaryFileData = reuseFileDataObjects(fileDataIndex);
                temporaryFileData.setName(temporaryPath.getFileName().toString());
                temporaryFileData.setSize(Files.size(temporaryPath));
                if (currentOwnerColumn.isVisible())
                {
                    temporaryFileData.setOwner(Files.getOwner(temporaryPath).getName());
                }
                if (currentLastModifiedTimeColumn.isVisible())
                {
                    temporaryFileData.setLastModifiedTime(Files.getLastModifiedTime(temporaryPath));
                }
                if (currentCreationTimeColumn.isVisible())
                {
                    temporaryFileData.setCreationTime(Files.readAttributes(temporaryPath, BasicFileAttributes.class).creationTime());
                }
                temporaryFileData.setDirectory(Files.isDirectory(temporaryPath));
                temporaryFileData.setFile(Files.isRegularFile(temporaryPath));
                temporaryFileData.setSymbolicLink(Files.isSymbolicLink(temporaryPath));

                if (temporaryFileData.isFile())
                {
                    temporaryFileData.setExtension(getExtension(temporaryPath));
                }
                if (temporaryFileData.isSymbolicLink())
                {
                    Path temporaryCurrentPath = currentPath.get(currentContentTabIndex);
                    Path symbolicLinkPath = Files.readSymbolicLink(temporaryCurrentPath.resolve(temporaryPath.getFileName()));
                    temporaryFileData.setSymbolicLinkPath(temporaryCurrentPath.resolve(symbolicLinkPath));
                }

                TreeItem<FileData> temporaryTreeItem = reuseTreeItemObjects(treeItemIndex, temporaryFileData);
                determineIconToTreeItem(temporaryFileData, temporaryTreeItem);
                treeItemIndex++;
                fileDataIndex++;
                currentRootItem.getChildren().add(temporaryTreeItem);
            }
            catch (NoSuchFileException noSuchFileException)
            {
                System.out.println("Скорее всего битая символическая ссылка " + temporaryPath.
                        getFileName().toString());
                temporaryFileData = new FileData(temporaryPath.getFileName().toString(), -1);
                temporaryFileData.setSymbolicLink(true);
                temporaryFileData.setWastedSymbolicLink(true);
                TreeItem<FileData> temporaryTreeItem = new TreeItem<>(temporaryFileData);
                applyIconForTreeItem(temporaryTreeItem, brokenLink_Image, fileIconHeight);
                currentRootItem.getChildren().add(temporaryTreeItem);
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }

        if (activatedTreeTableView.getExpandedItemCount() != 0)
        {
            activatedTreeTableView.getSelectionModel().clearSelection();
            activatedTreeTableView.getSelectionModel().select(0);
        }

        System.out.println("updateFilesContent duration: " + (System.currentTimeMillis() - startTime));

        requestSort(lastActiveSortColumn, currentTypeColumn);

        if (activatedTreeTableView.getExpandedItemCount() != 0)
        {
            activatedTreeTableView.getSelectionModel().clearSelection();
            activatedTreeTableView.getSelectionModel().select(0);
        }
        return true;
    }

    /**
     * Предназначен для повторного использования уже существующих объектов
     * типа TreeItem.
     */
    private TreeItem<FileData> reuseTreeItemObjects(final int treeItemIndex, FileData temporaryFileData)
    {
        TreeItem<FileData> temporaryTreeItem = null;

        if (treeItemIndex >= treeItemsSaver.size())
        {
            temporaryTreeItem = new TreeItem<>(temporaryFileData);
            treeItemsSaver.add(temporaryTreeItem);
            //System.out.println("TreeItem создан");
        }
        else
        {
            temporaryTreeItem = treeItemsSaver.get(treeItemIndex);
            temporaryTreeItem.setValue(temporaryFileData);
        }
        return temporaryTreeItem;
    }

    private FileData reuseFileDataObjects(final int fileDataIndex)
    {
        FileData temporaryFileData = null;
        if (fileDataIndex >= fileDataSaver.size())
        {
            temporaryFileData = new FileData("", 1);
            fileDataSaver.add(temporaryFileData);
        }
        else
        {
            temporaryFileData = fileDataSaver.get(fileDataIndex);
        }
        return temporaryFileData;
    }

    /**
     * Возвращается к родительскому элементу пути.
     */
    public boolean goToParentPath(Path path)
    {
        if (path == null)
        {
            return false;
        }

        if (goToPath(path.getParent()))
        {
            activatedTreeTableView.getSelectionModel().clearSelection();
            activatedTreeTableView.getSelectionModel().select(previousSelectedFileIndex);
            activatedTreeTableView.scrollTo(previousSelectedFileIndex - 2);
            previousSelectedFileIndex = 0;
            return true;
        }
        return false;
        //return goToPath(path.getParent());
    }

    /**
     * Позволяет задать свой список элементов в таблице содержимого. Содержимое
     * при это не очищается.
     */
    private boolean addCustomToContentInTable(TreeItem<FileData> targetRootItem, String pathName,
                                              String... names)
    {
        for (String currentName : names)
        {
            targetRootItem.getChildren().add(new TreeItem<>(new FileData(currentName, 4L)));
        }
        currentPath_TextField.setText(pathName);
        return true;
    }

    /**
     * Обработчик событий нажатия клавиши для таблицы файлов.
     */
    private void contentKeyPressed_Action(KeyEvent event)
    {
        KeyCode keyRelease_KeyCode = event.getCode();

        if (keyRelease_KeyCode == KeyCode.ENTER)
        {
            if (quickSearchInCurrentFolder_Popup != null &&
                    quickSearchInCurrentFolder_Popup.isShowing())
            {
                quickSearchInCurrentFolder_Popup.hide();
            }

            previousSelectedFileIndex = activatedTreeTableView.getSelectionModel().getSelectedIndex();

            if (currentPath.size() != 0 &&
                    currentPath.get(currentContentTabIndex) != null)
            {
                Path temporaryFilePath = currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
                if (Files.isRegularFile(temporaryFilePath))
                {
                    openInDefaultProgram(temporaryFilePath);
                }
                else
                {
                    System.out.println("Новый путь: " + currentPath);
                    goToPath(currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
                }
            }
            else
            {
                goToPath(Paths.get(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
            }

        }
        else if (keyRelease_KeyCode == KeyCode.F2)
        {
            rename_MenuItem.fire();
        }
        else if (keyRelease_KeyCode == KeyCode.PAGE_UP && event.isControlDown())
        {
            goToPreviousTab_MenuItem.fire();
        }
        else if (keyRelease_KeyCode == KeyCode.PAGE_DOWN && event.isControlDown())
        {
            goToNextTab_MenuItem.fire();
        }
        else if (keyRelease_KeyCode == KeyCode.SLASH)
        {
            System.out.println("Переходим к вводу пути...");
            Platform.runLater(() ->
            {
                currentPath_TextField.requestFocus();
            });
        }
        else if (keyRelease_KeyCode == KeyCode.F11)
        {

        }
        else if (keyRelease_KeyCode == KeyCode.F12)
        {
            long startTime = System.nanoTime();
            walkPathsThroughListMethod(currentPath.get(currentContentTabIndex).toFile().getPath());

            System.out.println("Времени потрачено: " + (System.nanoTime() - startTime));
            System.out.println("Файлов: " + countFiles);
            countFiles = 0;
        }
        else if (keyRelease_KeyCode.getCode() >= 48 &&
                keyRelease_KeyCode.getCode() <= 90 && !event.isControlDown())
        {
            System.out.println("Text: '" + event.getText() + "'");

            if (quickSearchInCurrentFolder_Popup == null)
            {
                initializeQuickSearchInCurrentDirectory();
            }


            if (!quickSearchInCurrentFolder_Popup.isShowing())
            {
                System.out.println("отображаем");
                quickSearchKey_TextField.setText(event.getText());
                quickSearchInCurrentFolder_Popup.show(stage, stage.getX(), stage.getY() + stage.getHeight()
                        + stage.getHeight() * 0.01D);
                quickSearchKey_TextField.requestFocus();
                quickSearchKey_TextField.end();
            }
        }
    }

    /**
     * Обработчик событий нажатия клавиш мыши для таблицы файлов.
     */
    private void cellMouseClicked_Action(MouseEvent event)
    {
        if (event.getButton() == MouseButton.PRIMARY &&
                event.getClickCount() == 2)
        {
            previousSelectedFileIndex = activatedTreeTableView.getSelectionModel().getSelectedIndex();

            if (currentPath.size() != 0 &&
                    currentPath.get(currentContentTabIndex) != null)
            {
                Path temporaryFilePath = currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
                if (Files.isRegularFile(temporaryFilePath))
                {
                    openInDefaultProgram(temporaryFilePath);
                }
                else
                {
                    goToPath(currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
                }
            }
            else
            {
                System.out.println(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
                goToPath(Paths.get(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
            }
        }
    }

    /**
     * Запрашивает сортировку по указанному столбцу. Возвращает управление, если
     * количество элементов меньше 2. В начале выполнения метода разрешается
     * сортировка столбцов, а в конце - запрещается. Это сделано для того, чтобы
     * во время срабатывания контекстного меню компонент не запрашивал сортировку
     * самостоятельно.
     */
    private void requestSort(CustomTreeTableColumn<FileData, ?> targetColumn, CustomTreeTableColumn<FileData, ?>
            targetTypeColumn)
    {
        long startTime = System.currentTimeMillis();

        if (activatedTreeTableView.getExpandedItemCount() < 2)
        {
            return;
        }

        lastActiveSortColumn = targetColumn;
        activatedTreeTableView.getSortOrder().clear();

        Iterator<TreeTableColumn<FileData, ?>> iterator = activatedTreeTableView.getColumns().iterator();
        while (iterator.hasNext())
        {
            iterator.next().setSortable(true);
        }

        if (targetColumn.equals(currentSizeBytesColumn))
        {
            activatedTreeTableView.getColumns().add(currentSizeBytesColumn);
            currentSizeBytesColumn.setSortable(true);
            activatedTreeTableView.getSortOrder().add(currentSizeBytesColumn);
        }


        targetColumn.setSortable(true);
        targetColumn.setSortType(sortType);

        if (directoriesFirst_MenuItem.isSelected())
        {
            activatedTreeTableView.getColumns().add(currentIsDirectoryColumn);
            currentIsDirectoryColumn.setSortable(true);
            //Потому, что каталоги отображаются единичкой
            currentIsDirectoryColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
            activatedTreeTableView.getSortOrder().add(currentIsDirectoryColumn);
        }

        activatedTreeTableView.getSortOrder().add(targetColumn);
        activatedTreeTableView.sort();


        iterator = activatedTreeTableView.getColumns().iterator();
        while (iterator.hasNext())
        {
            iterator.next().setSortable(false);
        }


        if (targetColumn.equals(currentSizeBytesColumn))
        {
            currentSizeBytesColumn.setSortable(false);
            currentSizeBytesColumn.setVisible(false);
            activatedTreeTableView.getColumns().remove(currentSizeBytesColumn);
        }

        if (directoriesFirst_MenuItem.isSelected())
        {
            currentIsDirectoryColumn.setSortable(false);
            currentIsDirectoryColumn.setVisible(false);
            activatedTreeTableView.getColumns().remove(currentIsDirectoryColumn);
        }

        System.out.println("requestSort() duration: " + (System.currentTimeMillis() - startTime));
    }


    private void delete_MenuItem_Action(ActionEvent eventDelete)
    {
        ObservableList<TreeItem<FileData>> selectionModelItems = activatedTreeTableView.getSelectionModel().getSelectedItems();
        Path temporaryCurrentPath = currentPath.get(currentContentTabIndex);
        Path[] paths = new Path[selectionModelItems.size()];

        for (int k = 0; k < paths.length; k++)
        {
            paths[k] = temporaryCurrentPath.resolve(selectionModelItems.get(k).getValue().getName());
        }

        if (deleteFileDialog == null)
        {
            deleteFileDialog = new DeleteFileDialog(this);
        }
        deleteFileDialog.setTargetPaths(paths);
        deleteFileDialog.show();
    }

    /**
     * Нужна для обработки результатов удаления.
     */
    public void updateFilesListAfterDeleting(final DeleteOperationResult deleteOperationResult)
    {
        if (deleteOperationResult == DeleteOperationResult.CANCELED
                || deleteOperationResult == DeleteOperationResult.COMPLETED_WITH_ERRORS)
        {
            goToPath(currentPath.get(currentContentTabIndex));
        }
        else
        {
            if (currentPath.get(currentContentTabIndex).equals(deleteFileDialog.getFilesToDeleting()[0].getParent()))
            {
                Path[] deletedFiles = deleteFileDialog.getFilesToDeleting();
                if (deletedFiles != null)
                {
                    ObservableList<TreeItem<FileData>> children = activatedTreeTableView.getRoot().getChildren();

                    for (Path temporaryDeletedPath : deletedFiles)
                    {
                        for (int k = 0; k < children.size(); k++)
                        {
                            if (children.get(k).getValue().getName().equals(temporaryDeletedPath.getFileName().toString()))
                            {
                                children.remove(k);
                                System.out.println("Удалено из списка: " + temporaryDeletedPath.getFileName());
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
        }

    }


    public void updateFilesListAfterCopying(final Path[] targetPaths)
    {
        if (currentPath.get(currentContentTabIndex).equals(targetPaths[0].getParent()))
        {
            System.out.println("Копирование происходило в этот каталог, добавляем.");

            for (Path temporaryFilePath : targetPaths)
            {
                ObservableList<TreeItem<FileData>> children = activatedTreeTableView.getRoot().getChildren();
                for (int k = 0; k < activatedTreeTableView.getRoot().getChildren().size(); k++)
                {
                    if (children.get(k).getValue().getName().equals(temporaryFilePath.getFileName().toString()))
                    {
                        TreeItem<FileData> temporaryFileData = children.get(k);
                        try
                        {
                            temporaryFileData.getValue().setSize(Files.size(temporaryFilePath));
                        }
                        catch (IOException ioException)
                        {
                            ioException.printStackTrace();
                        }
                        break;
                    }
                }
                //addRowToTreeTable(temporaryFilePath);
            }
            requestSort(lastActiveSortColumn, lastActiveSortColumn);
        }

    }


    private boolean deleteFileRecursively(Path targetPath) throws IOException
    {
        if (!Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS))
        {
            return false;
        }

        if (Files.isRegularFile(targetPath, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(targetPath))
        {
            Files.delete(targetPath);
        }
        else if (Files.isDirectory(targetPath))
        {
            Stream<Path> pathsStream = Files.list(targetPath);
            List<Path> pathsList = pathsStream.collect(Collectors.toList());
            if (pathsList.size() == 0)
            {
                Files.delete(targetPath);
            }
            else
            {
                for (Path temporaryPath : pathsList)
                {
                    System.out.println(deleteFileRecursively(temporaryPath));
                }
                deleteFileRecursively(targetPath);
            }

        }

        return true;
    }

    /**
     * Пока, только для одиночного переименования
     */
    private void rename_MenuItem_Action(ActionEvent event)
    {
        VBox fileName_VBox = new VBox(rem * 0.15D);
        fileName_VBox.setAlignment(Pos.CENTER);
//        fileName_VBox.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN,
//                CornerRadii.EMPTY, Insets.EMPTY)));

        Label fileAlreadyExists_Label = new Label("File with the same name already exists.");
        fileAlreadyExists_Label.setWrapText(true);
        fileAlreadyExists_Label.setVisible(false);
        fileAlreadyExists_Label.setTextFill(Color.LIGHTCORAL);
        //fileAlreadyExists_Label.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY)));
        fileAlreadyExists_Label.setFont(Font.font(fileAlreadyExists_Label.getFont().getName(),
                FontWeight.BOLD, 12.0D));

        TextField fileName_TextField = new TextField();
        fileName_TextField.setPromptText("Enter new name here");
        fileName_TextField.setOnKeyReleased(eventFileName ->
        {
            System.out.println("released");
            if (fileAlreadyExists_Label != null && fileAlreadyExists_Label.isVisible())
            {
                fileAlreadyExists_Label.setVisible(false);
            }
        });


        FileData temporaryFileData = activatedTreeTableView.getSelectionModel().getSelectedItem().getValue();
        Path temporaryPath = currentPath.get(currentContentTabIndex).resolve(temporaryFileData.getName());

        fileName_TextField.setText(temporaryFileData.getName());


        fileName_VBox.getChildren().addAll(fileName_TextField, fileAlreadyExists_Label);

        confirmOperationDialog.setTitle("Rename");
        confirmOperationDialog.setHeaderText("Rename");
        confirmOperationDialog.setHeaderColor(Color.INDIGO);
        confirmOperationDialog.setMessageText("Enter a new name below.");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(fileName_VBox);
        confirmOperationDialog.setConfirmOperationOnEnterKey(true);
        confirmOperationDialog.setBackgroundToRootNode(new Background(new BackgroundFill(Color.DARKSEAGREEN, CornerRadii.EMPTY, Insets.EMPTY)));

        ObservableList<Node> nodes = confirmOperationDialog.getOperationButtons();
        for (int k = 0; k < nodes.size(); k++)
        {
            ConfirmOperationButton temporaryConfirmOperationButton = (ConfirmOperationButton) nodes.get(k);
            temporaryConfirmOperationButton.setBackground(Background.EMPTY);
            temporaryConfirmOperationButton.setBorder(new Border(new BorderStroke(Color.BLACK,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.MEDIUM, Insets.EMPTY)));
        }

        while (true)
        {
            confirmOperationDialog.showAndWait();
            System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

            if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
            {
                Path targetPath = null;

                try
                {
                    targetPath = currentPath.get(currentContentTabIndex).resolve(fileName_TextField.getText());
                    Path resultPath = Files.move(temporaryPath, targetPath);

                    try
                    {
                        FileData temporaryData = activatedTreeTableView.getSelectionModel().getSelectedItem().getValue();
                        temporaryData = temporaryData.cloneFileData();
                        temporaryData.setName(targetPath.getFileName().toString());
                        activatedTreeTableView.getSelectionModel().getSelectedItem().setValue(temporaryData);
                    }
                    catch (CloneNotSupportedException cloneNotSupportedException)
                    {
                        cloneNotSupportedException.printStackTrace();
                    }
                    if (resultPath != null)
                    {
                        break;
                    }
                }
                catch (FileAlreadyExistsException fileAlreadyExistsException)
                {
                    //fileAlreadyExistsException.printStackTrace();
                    System.out.println("Файл с таким именем уже существует.");
                    fileAlreadyExists_Label.setVisible(true);
                    confirmOperationDialog.setContent(fileName_VBox);
                }
                catch (NoSuchFileException noSuchFileException)
                {
                    //fileAlreadyExistsException.printStackTrace();
                    System.out.println("Обычно эта ошибка означает, что имя недопустимое.");
                    break;
                }
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

    }

    private void copyFileName_MenuItem_Action(ActionEvent event)
    {

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
        if (Clipboard.getSystemClipboard().setContent(clipboardContent))
        {
            showLittleNotification(stage, "Name of file has been successfully copied.", 3);
        }
    }

    private void copyAbsoluteNamePath_MenuItem_Action(ActionEvent event)
    {
        ClipboardContent clipboardContent = new ClipboardContent();

        if (currentPath != null && currentPath.size() > currentContentTabIndex &&
                currentPath.get(currentContentTabIndex) != null)
        {
            clipboardContent.putString(currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().
                    getSelectedItem().getValue().getName()).toAbsolutePath().toString());
            if (Clipboard.getSystemClipboard().setContent(clipboardContent))
            {
                showLittleNotification(stage, "Absolute path of selected file has been copied.", 3);
            }
        }
        else
        {
            System.out.println("Невозможно скопировать абсолютный путь, т.к. он пустой.");
        }
    }

    /**
     * @deprecated Не дописано.
     */
    private void createFile_MenuItem_Action(ActionEvent event)
    {
        System.out.println("Запрос на создание файла.");

        VBox fileProperties_VBox = new VBox(rem * 0.15D);
        fileProperties_VBox.setAlignment(Pos.CENTER);
//        fileName_VBox.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN,
//                CornerRadii.EMPTY, Insets.EMPTY)));

        final Label fileAlreadyExists_Label = new Label("File with the same name already exists.");
        fileAlreadyExists_Label.setWrapText(true);
        fileAlreadyExists_Label.setVisible(false);
        fileAlreadyExists_Label.setTextFill(Color.LIGHTCORAL);
        //fileAlreadyExists_Label.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY)));
        fileAlreadyExists_Label.setFont(Font.font(fileAlreadyExists_Label.getFont().getName(),
                FontWeight.BOLD, 12.0D));

        TextField fileName_TextField = new TextField();
        fileName_TextField.setPromptText("Enter name of file here");
        fileName_TextField.setOnKeyReleased(textFieldEvent ->
        {
            if (fileAlreadyExists_Label != null)
            {
                fileAlreadyExists_Label.setVisible(false);
            }
        });

        VBox accordionContent_VBox = new VBox(rem * 0.35D);

        Accordion accordion = new Accordion();
        TitledPane advanced_TitledPane = new TitledPane("Advanced", accordionContent_VBox);
        accordion.getPanes().add(advanced_TitledPane);

        HBox creationTime_HBox = new HBox(rem * 0.35D);
        HBox lastModifiedDate_HBox = new HBox(rem * 0.35D);

        DatePicker creationTime_DataPicker = new DatePicker();
        creationTime_DataPicker.setShowWeekNumbers(true);
        creationTime_DataPicker.setPromptText("Creation Date (DD.MM.YY)");

        DatePicker lastModified_DataPicker = new DatePicker();
        lastModified_DataPicker.setShowWeekNumbers(true);
        lastModified_DataPicker.setPromptText("Last modified date (DD.MM.YY)");

        CheckBox creationDateTime_CheckBox = new CheckBox("Creation Date-time");
        creationDateTime_CheckBox.setOnAction(eventCheckBox ->
        {
            if (creationDateTime_CheckBox.isSelected())
            {
                //creationTime_CheckBox.setText("");
                creationTime_HBox.getChildren().add(creationTime_DataPicker);
            }
            else
            {
                //creationTime_CheckBox.setText("Creation time");
                creationTime_HBox.getChildren().remove(creationTime_DataPicker);
            }
        });

        CheckBox lastModifiedData_CheckBox = new CheckBox("Last modified Date-time");
        lastModifiedData_CheckBox.setOnAction(eventCheckBox ->
        {
            if (lastModifiedData_CheckBox.isSelected())
            {
                //creationTime_CheckBox.setText("");
                lastModifiedDate_HBox.getChildren().add(lastModified_DataPicker);
            }
            else
            {
                //creationTime_CheckBox.setText("Creation time");
                lastModifiedDate_HBox.getChildren().remove(lastModified_DataPicker);
            }
        });


        creationTime_HBox.getChildren().add(creationDateTime_CheckBox);
        lastModifiedDate_HBox.getChildren().add(lastModifiedData_CheckBox);

        accordionContent_VBox.getChildren().addAll(creationTime_HBox, lastModifiedDate_HBox);

        fileProperties_VBox.getChildren().addAll(fileName_TextField, fileAlreadyExists_Label,
                accordion);

        confirmOperationDialog.setTitle("Create File");
        confirmOperationDialog.setHeaderText("Create File");
        confirmOperationDialog.setHeaderColor(Color.web("#006c84"));
        confirmOperationDialog.setMessageText("Enter a name of file below.");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(fileProperties_VBox);
        confirmOperationDialog.setConfirmOperationOnEnterKey(true);
        confirmOperationDialog.setBackgroundToRootNode(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMinHeight(confirmOperationDialog.rem * 18);
        confirmOperationDialog.setMaxHeight(confirmOperationDialog.rem * 25);

        ObservableList<Node> nodes = confirmOperationDialog.getOperationButtons();
        for (int k = 0; k < nodes.size(); k++)
        {
            ConfirmOperationButton temporaryConfirmOperationButton = (ConfirmOperationButton) nodes.get(k);
            temporaryConfirmOperationButton.setBackground(Background.EMPTY);
            temporaryConfirmOperationButton.setBorder(new Border(new BorderStroke(Color.BLACK,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.MEDIUM, Insets.EMPTY)));
        }

        Path resultPath = null;
        while (true)
        {
            confirmOperationDialog.showAndWait();
            System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

            if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
            {
                try
                {
                    Path targetPath = currentPath.get(currentContentTabIndex).resolve(fileName_TextField.getText());
                    resultPath = Files.createFile(targetPath);

                    if (resultPath != null && Files.exists(resultPath))
                    {
                        //-------------------- Редактирование аттрибутов времени
                        BasicFileAttributes basicFileAttributes = Files.readAttributes(resultPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                        FileTime creationTime = basicFileAttributes.creationTime();
                        FileTime lastModifiedTime = basicFileAttributes.lastModifiedTime();
                        FileTime lastAccessTime = basicFileAttributes.lastAccessTime();
                        if (creationDateTime_CheckBox.isSelected())
                        {
                            LocalDateTime temporary = LocalDateTime.of(creationTime_DataPicker.getValue(),
                                    LocalTime.now());
                            Instant temporary_Instant = temporary.toInstant(ZoneOffset.UTC);
                            creationTime = FileTime.from(temporary_Instant);

                            if (temporary_Instant.isAfter(lastModifiedTime.toInstant()))
                            {
                                lastModifiedTime = FileTime.from(temporary_Instant);
                            }

                            if (System.getProperty("os.name").contains("Linux"))
                            {
                                lastModifiedTime = creationTime;
                                lastAccessTime = creationTime;
                            }
                        }
                        if (lastModifiedData_CheckBox.isSelected())
                        {
                            Instant lastModifiedTime_Instant = LocalDateTime.of(lastModified_DataPicker.getValue(),
                                    LocalTime.now()).toInstant(ZoneOffset.UTC);
                            lastModifiedTime = FileTime.from(lastModifiedTime_Instant);

                            if (lastModifiedTime_Instant.isBefore(creationTime.toInstant()))
                            {
                                creationTime = FileTime.from(lastModifiedTime_Instant);
                            }

                            if (System.getProperty("os.name").contains("Linux"))
                            {
                                creationTime = lastModifiedTime;
                                lastAccessTime = lastModifiedTime;
                            }
                        }
//                        if(creationDateTime_CheckBox.isSelected() &&
//                        lastModifiedData_CheckBox.isSelected())
//                        {
//                            //Дата редактирования не может быть раньше даты создания
//                            if(creationTime.compareTo(lastModifiedTime) < 0)
//                            {
//                                System.out.println("Правильно!");
//                            }
//                            else
//                            {
//                                System.out.println("Дата редактирования не может быть раньше даты создания!");
//                                lastModifiedTime=creationTime;
//                            }
//                        }

                        /*С атрибутами под Linux Manjaro ext4 обнаружена такая
                         * хрень: изменения атрибутов не применяются, если
                         * вызывать метод setTime() с разными параметрами времени.
                         * Другими словами, чтобы метод записал изменения нужно
                         * чтобы даты создания, редактирования и последнего доступа
                         * были одинаковыми.*/
                        Files.getFileAttributeView(resultPath, BasicFileAttributeView.class).setTimes(lastModifiedTime, lastAccessTime, creationTime);
                        System.out.println("Аттрибуты должны быть записаны записаны.");
                        addRowToTreeTable(resultPath);
                        requestSort(lastActiveSortColumn, lastActiveSortColumn);
                        break;
                    }
                }
                catch (FileAlreadyExistsException fileAlreadyExistsException)
                {
                    //fileAlreadyExistsException.printStackTrace();
                    System.out.println("Файл с таким именем уже существует.");
                    fileAlreadyExists_Label.setVisible(true);
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
    }


    /**
     * @deprecated Не дописано.
     */
    private void createDirectory_MenuItem_Action(ActionEvent event)
    {
        System.out.println("Запрос на создание каталога.");

        VBox fileProperties_VBox = new VBox(rem * 0.15D);
        fileProperties_VBox.setAlignment(Pos.CENTER);
//        fileName_VBox.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN,
//                CornerRadii.EMPTY, Insets.EMPTY)));

        final Label fileAlreadyExists_Label = new Label("File with the same name already exists.");
        fileAlreadyExists_Label.setWrapText(true);
        fileAlreadyExists_Label.setVisible(false);
        fileAlreadyExists_Label.setTextFill(Color.LIGHTCORAL);
        //fileAlreadyExists_Label.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY)));
        fileAlreadyExists_Label.setFont(Font.font(fileAlreadyExists_Label.getFont().getName(),
                FontWeight.BOLD, 12.0D));

        TextField fileName_TextField = new TextField();
        fileName_TextField.setPromptText("Enter name of directory here");
        fileName_TextField.setOnKeyReleased(textFieldEvent ->
        {
            if (fileAlreadyExists_Label != null)
            {
                fileAlreadyExists_Label.setVisible(false);
            }
        });

        VBox accordionContent_VBox = new VBox(rem * 0.35D);

        Accordion accordion = new Accordion();
        TitledPane advanced_TitledPane = new TitledPane("Advanced", accordionContent_VBox);
        accordion.getPanes().add(advanced_TitledPane);

        HBox creationTime_HBox = new HBox(rem * 0.35D);
        HBox lastModifiedDate_HBox = new HBox(rem * 0.35D);

        DatePicker creationTime_DataPicker = new DatePicker();
        creationTime_DataPicker.setShowWeekNumbers(true);
        creationTime_DataPicker.setPromptText("Creation Date (DD.MM.YY)");

        DatePicker lastModified_DataPicker = new DatePicker();
        lastModified_DataPicker.setShowWeekNumbers(true);
        lastModified_DataPicker.setPromptText("Last modified date (DD.MM.YY)");

        CheckBox creationDateTime_CheckBox = new CheckBox("Creation Date-time");
        creationDateTime_CheckBox.setOnAction(eventCheckBox ->
        {
            if (creationDateTime_CheckBox.isSelected())
            {
                //creationTime_CheckBox.setText("");
                creationTime_HBox.getChildren().add(creationTime_DataPicker);
            }
            else
            {
                //creationTime_CheckBox.setText("Creation time");
                creationTime_HBox.getChildren().remove(creationTime_DataPicker);
            }
        });

        CheckBox lastModifiedData_CheckBox = new CheckBox("Last modified Date-time");
        lastModifiedData_CheckBox.setOnAction(eventCheckBox ->
        {
            if (lastModifiedData_CheckBox.isSelected())
            {
                //creationTime_CheckBox.setText("");
                lastModifiedDate_HBox.getChildren().add(lastModified_DataPicker);
            }
            else
            {
                //creationTime_CheckBox.setText("Creation time");
                lastModifiedDate_HBox.getChildren().remove(lastModified_DataPicker);
            }
        });


        creationTime_HBox.getChildren().add(creationDateTime_CheckBox);
        lastModifiedDate_HBox.getChildren().add(lastModifiedData_CheckBox);

        accordionContent_VBox.getChildren().addAll(creationTime_HBox, lastModifiedDate_HBox);

        fileProperties_VBox.getChildren().addAll(fileName_TextField, fileAlreadyExists_Label,
                accordion);

        confirmOperationDialog.setTitle("Create Directory");
        confirmOperationDialog.setHeaderText("Create Directory");
        confirmOperationDialog.setHeaderColor(Color.GOLDENROD);
        confirmOperationDialog.setMessageText("Enter a name of directory below.");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(fileProperties_VBox);
        confirmOperationDialog.setConfirmOperationOnEnterKey(true);
        confirmOperationDialog.setBackgroundToRootNode(new Background(new BackgroundFill(Color.LIGHTGOLDENRODYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMinHeight(confirmOperationDialog.rem * 18);
        confirmOperationDialog.setMaxHeight(confirmOperationDialog.rem * 25);

        ObservableList<Node> nodes = confirmOperationDialog.getOperationButtons();
        for (int k = 0; k < nodes.size(); k++)
        {
            ConfirmOperationButton temporaryConfirmOperationButton = (ConfirmOperationButton) nodes.get(k);
            temporaryConfirmOperationButton.setBackground(Background.EMPTY);
            temporaryConfirmOperationButton.setBorder(new Border(new BorderStroke(Color.BLACK,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.MEDIUM, Insets.EMPTY)));
        }


        Path resultPath = null;
        while (true)
        {
            confirmOperationDialog.showAndWait();
            System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

            if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
            {
                try
                {
                    Path targetPath = currentPath.get(currentContentTabIndex).resolve(fileName_TextField.getText());
                    resultPath = Files.createDirectory(targetPath);

                    if (resultPath != null && Files.exists(resultPath))
                    {
                        //-------------------- Редактирование аттрибутов времени
                        BasicFileAttributes basicFileAttributes = Files.readAttributes(resultPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                        FileTime creationTime = basicFileAttributes.creationTime();
                        FileTime lastModifiedTime = basicFileAttributes.lastModifiedTime();
                        FileTime lastAccessTime = basicFileAttributes.lastAccessTime();
                        if (creationDateTime_CheckBox.isSelected())
                        {
                            LocalDateTime temporary = LocalDateTime.of(creationTime_DataPicker.getValue(),
                                    LocalTime.now());
                            Instant temporary_Instant = temporary.toInstant(ZoneOffset.UTC);
                            creationTime = FileTime.from(temporary_Instant);

                            if (temporary_Instant.isAfter(lastModifiedTime.toInstant()))
                            {
                                lastModifiedTime = FileTime.from(temporary_Instant);
                            }

                            if (System.getProperty("os.name").contains("Linux"))
                            {
                                lastModifiedTime = creationTime;
                                lastAccessTime = creationTime;
                            }
                        }
                        if (lastModifiedData_CheckBox.isSelected())
                        {
                            Instant lastModifiedTime_Instant = LocalDateTime.of(lastModified_DataPicker.getValue(),
                                    LocalTime.now()).toInstant(ZoneOffset.UTC);
                            lastModifiedTime = FileTime.from(lastModifiedTime_Instant);

                            if (lastModifiedTime_Instant.isBefore(creationTime.toInstant()))
                            {
                                creationTime = FileTime.from(lastModifiedTime_Instant);
                            }

                            if (System.getProperty("os.name").contains("Linux"))
                            {
                                creationTime = lastModifiedTime;
                                lastAccessTime = lastModifiedTime;
                            }
                        }

                        /*С атрибутами под Linux Manjaro ext4 обнаружена такая
                         * хрень: изменения атрибутов не применяются, если
                         * вызывать метод setTime() с разными параметрами времени.
                         * Другими словами, чтобы метод записал изменения нужно
                         * чтобы даты создания, редактирования и последнего доступа
                         * были одинаковыми.*/
                        Files.getFileAttributeView(resultPath, BasicFileAttributeView.class).setTimes(lastModifiedTime, lastAccessTime, creationTime);
                        System.out.println("Аттрибуты должны быть записаны записаны.");
                        addRowToTreeTable(resultPath);
                        requestSort(lastActiveSortColumn, lastActiveSortColumn);
                        break;
                    }
                }
                catch (FileAlreadyExistsException fileAlreadyExistsException)
                {
                    //fileAlreadyExistsException.printStackTrace();
                    System.out.println("Файл с таким именем уже существует.");
                    fileAlreadyExists_Label.setVisible(true);
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
    }


    private void pasteFiles_MenuItem_Action(ActionEvent event)
    {
        List<File> filesToPaste_List = Clipboard.getSystemClipboard().getFiles();
        Path[] sourceFilesPaths = new Path[filesToPaste_List.size()];
        Path[] destinationFilesPaths = new Path[filesToPaste_List.size()];
        Path temporaryCurrentDirectoryPath = currentPath.get(currentContentTabIndex);

        for (int k = 0; k < filesToPaste_List.size(); k++)
        {
            sourceFilesPaths[k] = filesToPaste_List.get(k).toPath();
            destinationFilesPaths[k] = temporaryCurrentDirectoryPath.resolve(sourceFilesPaths[k].getFileName());
        }

        if (copyFilesDialog == null)
        {
            copyFilesDialog = new CopyFilesDialog(this);
        }
        copyFilesDialog.setPathsToCopy(sourceFilesPaths, destinationFilesPaths);
        copyFilesDialog.show();
    }


    /**
     * Копирует файлы из одного места в другое. При обнаружении каталогов с одинаковым
     * именем происходит слияние каталогов. Файлы, имеющие одинаковые имена будут заменены.
     */
    private boolean copyFileRecursively(Path sourceFilePath, Path targetFilePath,
                                        boolean replaceExisting) throws IOException
    {

        if (Files.isDirectory(sourceFilePath, LinkOption.NOFOLLOW_LINKS))
        {
            if (!Files.exists(targetFilePath, LinkOption.NOFOLLOW_LINKS))
            {
                Files.createDirectory(targetFilePath);
            }
            if (Files.exists(targetFilePath, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(targetFilePath, LinkOption.NOFOLLOW_LINKS))
            {
                Files.delete(targetFilePath);
                Files.createDirectory(targetFilePath);
            }


            Stream<Path> PathsStream = Files.list(sourceFilePath);

            Iterator<Path> filesInDirectory_Iterator = PathsStream.iterator();
            while (filesInDirectory_Iterator.hasNext())
            {
                Path temporaryFilePath = filesInDirectory_Iterator.next();
                copyFileRecursively(temporaryFilePath, targetFilePath.resolve(temporaryFilePath.getFileName()),
                        replaceExisting);
            }
        }
        else
        {
            Path resultPath = null;
            if (replaceExisting)
            {
                resultPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.COPY_ATTRIBUTES,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            else
            {
                resultPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.COPY_ATTRIBUTES);
            }

            if (resultPath != null)
            {
                System.out.println("Скопировано успешно.");
            }
        }

        return true;
    }


    /**
     * Добавляет новую строку в таблицу файлов. При этом в объект модели
     * данных записываются только те данные, для которых отображены соответствующие
     * колонки. Не запрашивает сортироку.
     */
    private void addRowToTreeTable(Path targetPath)
    {
        FileData newFileData = new FileData(targetPath.getFileName().toString(),
                -1);

        try
        {
            PosixFileAttributes basicFileAttributes = Files.readAttributes(targetPath, PosixFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            newFileData.setSize(basicFileAttributes.size());
            newFileData.setFile(basicFileAttributes.isRegularFile());
            newFileData.setSymbolicLink(basicFileAttributes.isSymbolicLink());
            newFileData.setDirectory(basicFileAttributes.isDirectory());

            //Определяем ссылка ли это на каталог или файл
            if (newFileData.isSymbolicLink())
            {
                Path symbolicLinkTargetPath = Files.readSymbolicLink(targetPath);
                PosixFileAttributes temporaryAttributes = Files.readAttributes(symbolicLinkTargetPath,
                        PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                newFileData.setFile(temporaryAttributes.isRegularFile());
                newFileData.setDirectory(temporaryAttributes.isDirectory());
                newFileData.setSymbolicLinkPath(symbolicLinkTargetPath);
            }

            if (currentCreationTimeColumn.isVisible())
            {
                newFileData.setCreationTime(basicFileAttributes.creationTime());
            }
            if (currentLastModifiedTimeColumn.isVisible())
            {
                newFileData.setLastModifiedTime(basicFileAttributes.lastModifiedTime());
            }
            if (currentOwnerColumn.isVisible())
            {
                newFileData.setOwner(Files.getOwner(targetPath, LinkOption.NOFOLLOW_LINKS).getName());
            }

            TreeItem<FileData> temporaryTreeItem = new TreeItem<>(newFileData);
            determineIconToTreeItem(newFileData, temporaryTreeItem);

            activatedTreeTableView.getRoot().getChildren().add(temporaryTreeItem);
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
        //requestSort(lastActiveSortColumn, currentTypeColumn);
    }

    private void copyFilesToClipboard_MenuItem_Action(ActionEvent event)
    {
        ClipboardContent clipboardContent = new ClipboardContent();

        ObservableList<TreeItem<FileData>> selectedItems = activatedTreeTableView
                .getSelectionModel().getSelectedItems();

        List<File> filesToCopy_List = new ArrayList<File>();
        for (int k = 0; k < selectedItems.size(); k++)
        {
            filesToCopy_List.add(currentPath.get(currentContentTabIndex).resolve(selectedItems.get(k).
                    getValue().getName()).toFile());
        }
        if (clipboardContent.putFiles(filesToCopy_List))
        {
            Clipboard.getSystemClipboard().setContent(clipboardContent);

            showLittleNotification(stage, "Files have been successfully copied to clipboard.", 3);
        }
    }

    /**
     * Отображает в таблице файлов разделы жесткого диска (и не только),
     * при этом предыдущие данные в таблице очищаются.
     */
    private void goToDriveList()
    {
        currentOwnerColumn.setVisible(false);
        currentCreationTimeColumn.setVisible(false);
        currentLastModifiedTimeColumn.setVisible(false);
        activatedTreeTableView.setTableMenuButtonVisible(false);

        activatedTreeTableView.getRoot().getChildren().clear();
        Iterable<FileStore> fileStores_Iterable = FileSystems.getDefault().getFileStores();
        String temporaryFileStorePath = null;
        for (FileStore temporaryFileStore : fileStores_Iterable)
        {
            System.out.println("name: " + temporaryFileStore.name());
            temporaryFileStorePath = temporaryFileStore.toString();
            //System.out.println(temporaryFileStorePath);
//            temporaryFileStorePath = temporaryFileStorePath.substring(0,
//                    temporaryFileStorePath.indexOf('(') - 1);
            addCustomToContentInTable(activatedTreeTableView.getRoot(), FIlE_STORES_PATH, temporaryFileStorePath);
        }
        currentPath_TextField.setText(FIlE_STORES_PATH);
        content_TabPane.getSelectionModel().getSelectedItem().setText(FIlE_STORES_PATH);
    }


    /**
     * Отображает всплывающую подсказку - небольшое уведомление.
     */
    public static void showLittleNotification(Window window, String message, final int DurationInSeconds)
    {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setWrapText(true);
        tooltip.setHideOnEscape(true);
        tooltip.setAutoHide(true);
        tooltip.setFont(Font.font(tooltip.getFont().getName(), FontWeight.BOLD, 12.0D));
        tooltip.setOnShown(eventToolTip ->
        {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(DurationInSeconds), eventHide ->
            {
                tooltip.hide();
                tooltip.setOnShown(null);
            }));
            timeline.play();
        });
        Text temporaryText = new Text(tooltip.getText());
        tooltip.show(window, (window.getX() + window.getWidth() / 2.0D) -
                temporaryText.getBoundsInParent().getWidth() / 2.0D, window.getY() + window.getHeight() / 2);
    }

    private void createNewTab_Action(ActionEvent event)
    {
        Tab newTab = new Tab();
        newTab.setClosable(true);

        //---------------------------- TreeTableView
        TreeTableView<FileData> newTreeTableView = new TreeTableView<>();
        newTreeTableView.setOnKeyReleased(this::contentKeyPressed_Action);
        newTreeTableView.setContextMenu(contextMenuForFiles);
        newTreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        CustomTreeTableColumn<FileData, String> newNameColumn = new CustomTreeTableColumn<>("Name");
        newNameColumn.setMinWidth(preferredWidth * 0.1D);
        newNameColumn.setPrefWidth(preferredWidth * 0.5D);
        newNameColumn.setSortable(false);
        newNameColumn.setId(NAME_COLUMN_ID);
        newNameColumn.setContextMenu(contextMenuForColums);
        newNameColumn.setCellFactory(new Callback<TreeTableColumn<FileData, String>, TreeTableCell<FileData, String>>()
        {
            @Override
            public TreeTableCell<FileData, String> call(TreeTableColumn<FileData, String> fileDataStringTreeTableColumn)
            {
                CustomTreeTableCell<FileData, String> temporaryCell = new CustomTreeTableCell<>();
                //temporaryCell.setTextFill(Color.LIGHTSKYBLUE);
                temporaryCell.setPadding(new Insets(0.0D, 0.0D, 0.0, rem * 0.2D));
                temporaryCell.setOnMouseClicked(event ->
                {
                    cellMouseClicked_Action(event);
                });
                return temporaryCell;
            }
        });

        CustomTreeTableColumn<FileData, String> newSizeColumn = new CustomTreeTableColumn<>("Size");
        newSizeColumn.setMinWidth(preferredWidth * 0.05D);
        newSizeColumn.setPrefWidth(preferredWidth * 0.1D);
        newSizeColumn.setSortable(false);
        newSizeColumn.setContextMenu(contextMenuForColums);
        newSizeColumn.setId(SIZE_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> newOwnerColumn = new CustomTreeTableColumn<>("Owner");
        newOwnerColumn.setMinWidth(preferredWidth * 0.1D);
        newOwnerColumn.setSortable(false);
        newOwnerColumn.setVisible(false);
        newOwnerColumn.setId(OWNER_COLUMN_ID);
        newOwnerColumn.setContextMenu(contextMenuForColums);
        newOwnerColumn.visibleProperty().addListener(eventOwner ->
        {
            if (newOwnerColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByOwner_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
            }
        });

        CustomTreeTableColumn<FileData, String> newLastModifiedTimeColumn = new CustomTreeTableColumn<>("Last modified time");
        newLastModifiedTimeColumn.setMinWidth(preferredWidth * 0.15D);
        newLastModifiedTimeColumn.setSortable(false);
        newLastModifiedTimeColumn.setVisible(false);
        newLastModifiedTimeColumn.setContextMenu(contextMenuForColums);
        newLastModifiedTimeColumn.setId(LAST_MODIFIED_TIME_COLUMN_ID);
        newLastModifiedTimeColumn.visibleProperty().addListener(eventLast ->
        {
            if (newLastModifiedTimeColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByLastModifiedTime_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
            }
        });

        CustomTreeTableColumn<FileData, String> newCreationTimeColumn = new CustomTreeTableColumn<>("Creation time");
        newCreationTimeColumn.setMinWidth(preferredWidth * 0.15D);
        newCreationTimeColumn.setSortable(false);
        newCreationTimeColumn.setVisible(false);
        newCreationTimeColumn.setId(CREATION_TIME_COLUMN_ID);
        newCreationTimeColumn.setContextMenu(contextMenuForColums);
        newCreationTimeColumn.visibleProperty().addListener(eventCreation ->
        {
            if (newCreationTimeColumn.isVisible())
            {
                System.out.println("Переход по пути");
                goToPath(currentPath.get(currentContentTabIndex));
            }
            else
            {
                if (sortByCreationTime_MenuItem.isSelected())
                {
                    sortByName_MenuItem.setSelected(true);
                    lastActiveSortColumn = currentNameColumn;
                }
            }
        });

        CustomTreeTableColumn<FileData, String> newTypeColumn = new CustomTreeTableColumn<>("Type");
        newTypeColumn.setMinWidth(preferredWidth * 0.1D);
        newTypeColumn.setSortable(false);
        newTypeColumn.setPrefWidth(preferredWidth * 0.15D);
        newTypeColumn.setContextMenu(contextMenuForColums);
        newTypeColumn.setId(TYPE_COLUMN_ID);
        newTypeColumn.visibleProperty().addListener(eventTypeColumn ->
        {
            if (newTypeColumn.isVisible())
            {
                goToPath(currentPath.get(currentContentTabIndex));
            }

        });

        CustomTreeTableColumn<FileData, Long> sizeBytesColumn = new CustomTreeTableColumn<>("Size (bytes)");
        sizeBytesColumn.setMinWidth(preferredWidth * 0.1D);
        sizeBytesColumn.setSortable(false);
        sizeBytesColumn.setPrefWidth(preferredWidth * 0.15D);
        sizeBytesColumn.setId(SIZE_BYTES_COLUMN_ID);
        sizeBytesColumn.setVisible(false);

        CustomTreeTableColumn<FileData, Short> isDirectoryColumn = new CustomTreeTableColumn<>("is Directory");
        isDirectoryColumn.setMinWidth(preferredWidth * 0.1D);
        isDirectoryColumn.setSortable(false);
        isDirectoryColumn.setPrefWidth(preferredWidth * 0.15D);
        isDirectoryColumn.setId("hren");
        isDirectoryColumn.setVisible(false);


        newNameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
        newSizeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().getSize(true)));
        newOwnerColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getOwner()));
        newLastModifiedTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getLastModifiedTime(dateTimeIsoFormatter)));
        newCreationTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getCreationTime(dateTimeIsoFormatter)));
        newTypeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getType()));
        sizeBytesColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Long> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().getSize()));
        isDirectoryColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Short> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().isDirectory(true)));

        newTreeTableView.getColumns().addAll(newNameColumn, newTypeColumn, newSizeColumn,
                newOwnerColumn, newCreationTimeColumn, newLastModifiedTimeColumn);


        sortByOwner_MenuItem.disableProperty().bind(newOwnerColumn.visibleProperty().not());
        sortByCreationTime_MenuItem.disableProperty().bind(newCreationTimeColumn.visibleProperty().not());
        sortByLastModifiedTime_MenuItem.disableProperty().bind(newLastModifiedTimeColumn.visibleProperty().not());

        newTreeTableView.setTableMenuButtonVisible(true);
        newTreeTableView.setRoot(new TreeItem<>(new FileData("Root", 8192)));
        newTreeTableView.setShowRoot(false);
        newTreeTableView.getSortOrder().addAll(newNameColumn, newTypeColumn);

        VBox files_VBox = new VBox(rem * 0.45D);
        files_VBox.getChildren().add(newTreeTableView);

        ScrollPane newFiles_ScrollPane = new ScrollPane();
        newFiles_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        newFiles_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        newFiles_ScrollPane.setContent(files_VBox);
        newFiles_ScrollPane.setFitToWidth(true);
        newFiles_ScrollPane.setFitToHeight(true);

        newTab.setContent(newFiles_ScrollPane);
        newTab.setOnCloseRequest(eventCloseRequest ->
        {
            currentPath.remove(currentContentTabIndex);
            allTreeTableViewWithFiles_ArrayList.remove(currentContentTabIndex);
            allContainersWithFiles_ArrayList.remove(currentContentTabIndex);
            allIsDirectoryColumns_ArrayList.remove(currentContentTabIndex);
        });

        content_TabPane.getTabs().add(newTab);

        allTreeTableViewWithFiles_ArrayList.add(newTreeTableView);
        allContainersWithFiles_ArrayList.add(files_VBox);
        allIsDirectoryColumns_ArrayList.add(isDirectoryColumn);


        //Каждая открытая вкладка наследует путь от текущей
        if (currentPath.size() > currentContentTabIndex &&
                currentPath.get(currentContentTabIndex) != null)
        {
            currentPath.add(currentPath.get(currentContentTabIndex));

            if (currentPath.get(currentContentTabIndex + 1).getFileName() != null)
            {
                newTab.setText(currentPath.get(currentContentTabIndex).getFileName().toString());
            }
            else
            {
                newTab.setText(currentPath.get(currentContentTabIndex).toAbsolutePath().toString());
            }
        }

    }

    /**
     * Находит и возвращает столбец по имени параметра ID. Поиск производится в активной
     * таблице за счет переменной activatedTreeTableView.
     */
    private CustomTreeTableColumn<FileData, String> findColumnByStringID(final String id)
    {
        Iterator<TreeTableColumn<FileData, ?>> iterator = activatedTreeTableView.getColumns().iterator();
        CustomTreeTableColumn<FileData, String> temporaryCustomColumn = null;

        while (iterator.hasNext())
        {
            temporaryCustomColumn = (CustomTreeTableColumn<FileData, String>) iterator.next();
            if (temporaryCustomColumn.getId().contains(id))
            {
                return temporaryCustomColumn;
            }
        }
        return null;
    }


    private void goToPreviousTab_Action(ActionEvent event)
    {
        if (content_TabPane.getTabs().size() <= 1
                || currentContentTabIndex < 1)
        {
            return;
        }
        content_TabPane.getSelectionModel().select(--currentContentTabIndex);
    }

    private void goToNextTab_Action(ActionEvent event)
    {
        if (content_TabPane.getTabs().size() <= 1
                || currentContentTabIndex >= content_TabPane.getTabs().size() - 1)
        {
            return;
        }
        content_TabPane.getSelectionModel().select(++currentContentTabIndex);
    }

    private void applyIconForTreeItem(TreeItem<?> treeItem, final Image icon, final double size)
    {
        ImageView temporaryIcon = new ImageView(icon);
        temporaryIcon.setPreserveRatio(true);
        temporaryIcon.setFitHeight(size);
        temporaryIcon.setSmooth(true);
        treeItem.setGraphic(temporaryIcon);

    }

    private void removeSelectedRowsFromTreeTableView()
    {
        ObservableList<Integer> indices = activatedTreeTableView.getSelectionModel().getSelectedIndices();
        Object[] indicesArray = indices.toArray();
        TreeItem<FileData> temporaryRoot = activatedTreeTableView.getRoot();
        int indexToRemoving = -1;
        for (int k = 0, offset = 0; k < indicesArray.length; k++)
        {
            temporaryRoot.getChildren().remove((int) indicesArray[k] - offset);
            offset++;
        }
    }

    private void showOrEditAttributes_Action(ActionEvent event)
    {
        Path temporaryPath = currentPath.get(currentContentTabIndex).resolve(activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());


        fileAttributesEditor = new FileAttributesEditor(temporaryPath);
        fileAttributesEditor.show();
    }

    private void addActionToOpenTerminalMenuItem(ActionEvent event)
    {
        String osName = System.getProperty("os.name");
        ProcessBuilder terminalProcessBuilder = null;

        try
        {
            if (osName.contains("Linux"))
            {
                terminalProcessBuilder = new ProcessBuilder(Shell.getTerminalEmulatorName());

            }
            else if (osName.contains("Windows"))
            {

                terminalProcessBuilder = new ProcessBuilder(Shell.getTerminalEmulatorName(), "/K", "start");
            }
        }
        catch (InterruptedException interruptedException)
        {
            interruptedException.printStackTrace();
        }


        try
        {
            terminalProcessBuilder.directory(currentPath.get(currentContentTabIndex).toFile());
            Process process = terminalProcessBuilder.start();
            //process.waitFor();
            //process.destroy();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
//        catch (InterruptedException InterruptedException)
//        {
//            InterruptedException.printStackTrace();
//        }
    }

    private void fileSystemName_MouseEnteredAction(MouseEvent event)
    {
        if (fileStore_Popup == null)
        {
            fileStore_Popup = new FileStoreInfoPopup(currentPath.get(
                    currentContentTabIndex));
        }


        if (fileStore_Popup.isShowing())
        {
            fileStore_Popup.hide();
        }
        else
        {
            fileStore_Popup.updateData(currentPath.get(currentContentTabIndex));
            fileStore_Popup.show(stage);
            double labelWidth = fileSystemName_Label.getBoundsInParent().getWidth();
            double labelHeight = fileSystemName_Label.getBoundsInParent().getHeight();
            fileStore_Popup.setX(fileSystemName_Label.localToScreen(
                    fileSystemName_Label.getBoundsInLocal()).getMinX());
            fileStore_Popup.setY(fileSystemName_Label.localToScreen(
                    fileSystemName_Label.getBoundsInLocal()).getMinY() +
                    fileSystemName_Label.getBoundsInParent().getHeight() + labelHeight * 0.2D);
        }
    }


    /**
     * Изменяет цвет рамки и текста текущей файловой системы на один из
     * заранее подготовленых цветов. Смена происходит в случае смены файловой
     * системы.
     */
    private void changeFileSystemLabelTextAndColor()
    {
        try
        {
            String newFileSystemName = Files.getFileStore(currentPath.
                    get(currentContentTabIndex)).type();

            if (!fileSystemName_Label.getText().equals(newFileSystemName))
            {
                System.out.println("Нужно сменить цвет.");
                int newColorIndex = new Random().nextInt(textColorForFileSystemName_Color.length);
                fileSystemName_Label.setTextFill(textColorForFileSystemName_Color[newColorIndex]);
                fileSystemName_Label.setBorder(new Border(new BorderStroke(
                        borderColorForFileSystemName_Color[newColorIndex], BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, fileSystemName_Label.getBorder().getStrokes()
                        .get(0).getWidths())));

            }


            fileSystemName_Label.setText(newFileSystemName);
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

    }

    /**
     * Корректирует параметры сортировки исходя из выбранных параметров
     * сортировки в меню.
     */
    private void configureSortPropertiesByMenuItems()
    {
        if (sortByName_MenuItem.isSelected())
        {
            lastActiveSortColumn = currentNameColumn;
        }
        if (sortByType_MenuItem.isSelected())
        {
            lastActiveSortColumn = currentTypeColumn;
        }
        if (sortByOwner_MenuItem.isSelected())
        {
            lastActiveSortColumn = currentOwnerColumn;
        }
        if (sortByCreationTime_MenuItem.isSelected())
        {
            lastActiveSortColumn = currentCreationTimeColumn;
        }
        if (sortByLastModifiedTime_MenuItem.isSelected())
        {
            lastActiveSortColumn = currentLastModifiedTimeColumn;
        }
    }


    /**
     * Определяет какую иконку нужно использовать для элемента таблицы и устанавливает её.
     */
    private void determineIconToTreeItem(FileData fileData, final TreeItem<FileData> targetItem)
    {
        if (fileData.isSymbolicLink())
        {
            //applyIconForTreeItem(targetItem, folderIcon_Image, fileIconHeight);
        }
        if (fileData.isDirectory())
        {
            applyIconForTreeItem(targetItem, folderIcon_Image, fileIconHeight);
        }
        if (fileData.isFile())
        {
            applyIconForTreeItem(targetItem, fileIcon_Image, fileIconHeight);
        }
        //if(temporaryFileData.isSymbolicLink())
        //{
        //applyIconForTreeItem(temporaryTreeItem, fileIcon_Image, fileIconHeight);
        //}
    }

    private void processArguments(List<String> rawParametersList)
    {
        if (rawParametersList.get(0).equals("--path"))
        {
            goToPath(Paths.get(rawParametersList.get(1)));
        }
    }

    public static void main(String[] args)
    {
        for (int k = 0; k < args.length; k++)
        {
            System.out.println("args[" + k + "]: " + args[k]);
        }
        if (args != null && args.length > 0 && args[0] != null)
        {
            System.out.println(args[0]);
            Path temporaryPath = Paths.get(args[0]);

            try
            {
                if (Files.exists(temporaryPath))
                {
                    System.out.println("Файл существует. Запускаем программу.");
                    Application.launch("--path", temporaryPath.toAbsolutePath().toString());
                }
                else if (Files.exists(temporaryPath.getParent()))
                {
                    System.out.println("Parent: " + temporaryPath.getParent().toAbsolutePath().toString());
                    Application.launch("--path", temporaryPath.getParent().toAbsolutePath().toString());
                }
                else
                {
                    System.out.println("Распознать путь по аргументу не удалось.");
                    Application.launch();
                }
            }
            catch (NullPointerException nullPointerException)
            {
                System.out.println("Скорее всего такого пути не существует.");
                Application.launch();
            }

        }
        else
        {
            System.out.println("Запускаем без параметров.");
            Application.launch();
        }
    }


    private void createSymbolicLink_MenuItem_Action(ActionEvent event)
    {
        Path temporaryTargetLinkPath = currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());

        if (createSymbolicLinkDialog == null)
        {
            createSymbolicLinkDialog = new CreateSymbolicLinkDialog(
                    temporaryTargetLinkPath, null);
        }
        Path resultPath = createSymbolicLinkDialog.showAndWait(temporaryTargetLinkPath,
                null);

        if (resultPath != null && resultPath.getParent().equals(currentPath
                .get(currentContentTabIndex)))
        {
            addRowToTreeTable(resultPath);
            requestSort(lastActiveSortColumn, lastActiveSortColumn);
        }
        else
        {
            System.out.println("Символическая ссылка создана в другом месте. Добавлять " +
                    "файл в таблицу не нужно.");
        }
    }

    private void initializeQuickSearchInCurrentDirectory()
    {
        quickSearchInCurrentFolder_Popup = new Popup();
        quickSearchInCurrentFolder_Popup.setAutoHide(true);

        quickSearchKey_TextField = new TextField();
        quickSearchKey_TextField.textProperty().addListener((event, oldText, newText) ->
        {
            quickSearchKey_TextField.deselect();
            quickSearchKey_TextField.end();
            quickSearchInCurrentFolder();
        });

        VBox quickSearchContent_VBox = new VBox();
        quickSearchContent_VBox.setPadding(new Insets(rem * 0.45D));
        quickSearchContent_VBox.getChildren().add(quickSearchKey_TextField);


        quickSearchInCurrentFolder_Popup.getContent().add(quickSearchContent_VBox);
    }

    /**
     * Производит поиск совпадений в названиях файлов в текущем каталоге и выделяет
     * тот, что больше подходит. Поиск не чувствителен к регистру.
     */
    private void quickSearchInCurrentFolder()
    {
        ObservableList<TreeItem<FileData>> elements = activatedTreeTableView.getRoot().getChildren();
        TreeTableView.TreeTableViewSelectionModel<FileData> selectionModel = activatedTreeTableView.getSelectionModel();

        for (int k = 0; k < elements.size(); k++)
        {
            if (elements.get(k).getValue().getName().toLowerCase().contains(quickSearchKey_TextField.getText()))
            {
                selectionModel.clearSelection();
                selectionModel.select(k);

                if (k > 2)
                {
                    activatedTreeTableView.scrollTo(k - 2);
                }
                else
                {
                    activatedTreeTableView.scrollTo(k);
                }
//                System.out.println("Совпадение найдено: " + activatedTreeTableView.getSelectionModel()
//                        .getSelectedItem().getValue().getName());
                return;
            }
        }
        selectionModel.clearSelection();
        selectionModel.select(0);
        activatedTreeTableView.scrollTo(0);
    }

//    private void registerWatchService(final Path targetPath)
//    {
//        Thread watchThread = new Thread(()->
//        {
//            try
//            {
//                WatchService watchService = targetPath.getFileSystem().newWatchService();
//
//                WatchKey watchKey = targetPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
//                        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
//
//                WatchKey temporaryKey;
//
//                while ((temporaryKey = watchService.take()) != null)
//                {
//                    for (WatchEvent<?> event : temporaryKey.pollEvents())
//                    {
//                        System.out.println("Тип события: " + event.kind());
//                        System.out.println("Зайл затронут: " + event.context());
//                        Platform.runLater(() ->
//                        {
//                            goToPath(currentPath.get(currentContentTabIndex));
//                        });
//                    }
//                    temporaryKey.reset();
//                }
//            }
//            catch (IOException ioException)
//            {
//                ioException.printStackTrace();
//            }
//            catch(InterruptedException interruptedException)
//            {
//                interruptedException.printStackTrace();
//            }
//        },"watch Thread");
//        watchThread.start();
//
//    }

    private int countFiles = 0;

    private void walkPathsThroughListMethod(String dirPath)
    {
        File walkDir = new File(dirPath);
        String[] dirList = walkDir.list();

        for (int k = 0; k < dirList.length; k++)
        {
            File f = new File(dirPath + "/" + dirList[k]);
            //System.out.println(f.toString());
            countFiles++;
            if (f.isDirectory())
            {
                walkPathsThroughListMethod(f.getPath());
            }
        }

    }

    private void openInNewTab_Action(ActionEvent event)
    {
        Path temporarySelectedFilePath = currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());

        if (Files.isDirectory(temporarySelectedFilePath, LinkOption.NOFOLLOW_LINKS))
        {
            createNewTab_Action(event);
            currentPath.remove(currentPath.size() - 1);
            currentPath.add(temporarySelectedFilePath);

            if (temporarySelectedFilePath.getFileName() != null)
            {
                content_TabPane.getTabs().get(content_TabPane.getTabs().size() - 1)
                        .setText(temporarySelectedFilePath.getFileName().toString());
            }
            else
            {
                content_TabPane.getTabs().get(content_TabPane.getTabs().size() - 1)
                        .setText(temporarySelectedFilePath.toAbsolutePath().toString());
            }
        }
        else
        {
            System.out.println("Нельзя открыть файл в новой вкладке!");
            return;
        }

    }

    private void openInDefaultProgram_Action(ActionEvent event)
    {
        Path temporarySelectedFilePath = currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());

        if (Files.isRegularFile(temporarySelectedFilePath))
        {
            getHostServices().showDocument(temporarySelectedFilePath.toAbsolutePath().toUri().toString());
        }
        else
        {
            System.out.println("Нельзя открыть каталог в системе.");
            return;
        }
    }

    /**
     * Открывает указанный файл в программе, что ассоциируется с этим типом
     * в системе. Проверка на файл отсувствует.
     */
    private void openInDefaultProgram(final Path targetPath)
    {
        getHostServices().showDocument(targetPath.toAbsolutePath().toUri().toString());
    }

    private void openWithCustomProgram_Action(ActionEvent event)
    {
        Path temporaryFilePath = currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()
        );

        if (activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().isFile())
        {
            OpenWithCustomProgramDialog openWithDialog = new OpenWithCustomProgramDialog(temporaryFilePath);
            openWithDialog.show();
        }
    }

    private String getExtension(final Path targetPath)
    {
        String extension = "";
        if (targetPath.getFileName() != null)
        {
            int extensionStartIndex = targetPath.getFileName().toString().lastIndexOf('.');

            if (extensionStartIndex > 0)
            {
                extension = targetPath.getFileName().toString().substring(extensionStartIndex + 1);
                extension = extension.toUpperCase();
            }
        }
        else
        {
            System.out.println("Нельзя определить расширения, т.к. не удалось" +
                    "получить имя.");
            return "";
        }
        return extension;
    }

}

