package org.yaldysse.fm;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.commons.io.FilenameUtils;
import org.yaldysse.fm.dialogs.*;
import org.yaldysse.fm.dialogs.copy.CopyFilesDialog;
import org.yaldysse.fm.dialogs.delete.DeleteFileDialog;
import org.yaldysse.fm.dialogs.delete.DeleteOperationResult;
import org.yaldysse.fm.dialogs.favorites.FavoritesDialog;
import org.yaldysse.tools.Shell;
import org.yaldysse.tools.StorageCapacity;
import org.yaldysse.tools.os.windows.StorageSpaceFormatter;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;


/**
 * Нужно сделать:
 * 3 [Функционал]: Редактор аттрибутов.
 * 4 [Функционал]: Авто обновление таблицы файлов.
 * 5 [Функционал]: Реализовать возврат предыдущего выделения при переходе в родительский каталог,
 * но только на один уровень.
 * 6: Вспливающие подсказки
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
    private String languagePath;
    private ArrayList<Path> currentPath;
    private TreeTableColumn.SortType sortType;
    private CustomTreeTableColumn<FileData, ?> lastActiveSortColumn;
    private boolean filesToMoveFromClipboard = false;
    private int currentContentTabIndex;
    private DateTimeFormatter dateTimeIsoFormatter;
    private double fileIconHeight;
    /**
     * Предназначен для хранения всех объектов TreeItem, чтобы
     * за каждым разом не создавать новые.
     */
    private ObservableList<TreeItem<FileData>> treeItemsSaver;
    private ArrayList<FileData> fileDataSaver;
    private ArrayList<Path> favorites_List;

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
    private Menu language_Menu;
    private MenuItem favoritesDialog_MenuItem;

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
    private MenuItem openInCustomProgram_contextMenuForFilesItem;
    private Menu openWith_Menu;

    private MenuItem createFile_contextMenuForFilesItem;
    private MenuItem createDirectory_contextMenuForFilesItem;
    private MenuItem openInNewTab_contextMenuForFilesItem;
    private MenuItem openInDefaultProgram_contextMenuForFilesItem;
    private MenuItem createSymbolicLink_contextMenuForFilesItem;
    private MenuItem copyFileToClipboard_contextMenuForFilesItem;
    private MenuItem moveFilesToClipboard_contextMenuForFilesItem;
    private MenuItem renameFile_contextMenuForFilesItem;
    private MenuItem openInSystem_contextMenuForFilesItem;
    private MenuItem deleteFile_contextMenuForFilesItem;
    private MenuItem pasteFileFromClipboard_contextMenuForFilesItem;
    private MenuItem addToFavorites_contextMenuForFilesItem;


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
    public final Properties properties = new Properties();
    private Properties currentLanguage;

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

    private ToolBar info_ToolBar;
    private Label selectedItem_toolBarLabel;
    private Label selectedItemValue_toolBarLabel;
    private Label selectedSize_toolBarLabel;
    private Label selectedSizeValue_toolBarLabel;
    private Label selectedRegularFiles_toolBarLabel;
    private Label selectedRegularFilesValue_toolBarLabel;
    private Label selectedDirectories_toolBarLabel;
    private Label selectedDirectoriesValue_toolBarLabel;
    private FileNameTip fileNameTip;
    private Thread totalSizeCounterForToolBar_Thread;
    private SimpleFileSizeAndNumberCounter fileSizeCounterForToolBar;
    private ProgressIndicator selectedSize_ProgressIndicator;

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
        stage.setOnHidden(this::stageOnHidden);

        stage.setX(Double.parseDouble(properties.getProperty("locationX",
                "10")));
        stage.setY(Double.parseDouble(properties.getProperty("locationY", "10")));
        stage.setWidth(Double.parseDouble(properties.getProperty("stageWidth", "" + preferredWidth)));
        stage.setHeight(Double.parseDouble(properties.getProperty("stageHeight", "" + preferredHeight)));

        stage.show();
    }

    private void initializeComponents()
    {
        dateTimeIsoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");
        loadResources();
        loadPropertyFile();
        initializeGeneral();
        initializeMenu();
        initializeLanguageMenuItems();
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
        if (rawParametersList != null && rawParametersList.size() > 1)
        {
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
        else
        {
            int tabsNumber = Integer.parseInt(properties.getProperty("tabsNumber", "0"));
            for (int k = 0; k < tabsNumber; k++)
            {
                Path temporaryPath = Path.of(properties.getProperty("tabPath_" + k, FIlE_STORES_PATH));
                System.out.println("Путь из настроек:" + temporaryPath.toAbsolutePath().toString());
                if (Files.exists(temporaryPath, LinkOption.NOFOLLOW_LINKS))
                {
                    if (k + 1 > content_TabPane.getTabs().size())
                    {
                        createNewTab_Action(null);
                    }
                    if (currentPath.size() > 0)
                    {
                        currentPath.remove(currentPath.size() - 1);
                    }
                    currentPath.add(temporaryPath);
                    String fileName = temporaryPath.getFileName().toString();
                    if (fileName == null)
                    {
                        fileName = temporaryPath.toString();
                    }
                    content_TabPane.getTabs().get(content_TabPane.getTabs().size() - 1)
                            .setText(fileName);
                }
            }
            if (tabsNumber > 0 && content_TabPane.getTabs().size() > tabsNumber
                    && Files.exists(currentPath.get(tabsNumber - 1), LinkOption.NOFOLLOW_LINKS))
            {
                content_TabPane.getSelectionModel().select(
                        content_TabPane.getTabs().size() - 1);
                if (content_TabPane.getTabs().size() == 1)
                {
                    goToPath(currentPath.get(0));
                }
                return;
            }

            goToFileStoragesPane();
            content_TabPane.getSelectionModel().getSelectedItem().setText(FIlE_STORES_PATH);
        }

    }

    private void initializeMenu()
    {
        exit_MenuItem = new MenuItem(currentLanguage.getProperty("exit_menuItem",
                "Exit"));
        exit_MenuItem.setOnAction(event ->
        {
            System.exit(0);
        });
        exit_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        copyFileName_MenuItem = new MenuItem(currentLanguage.getProperty("copyFileName_menuItem",
                "Copy name of file"));
        copyFileName_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN));
        copyFileName_MenuItem.setOnAction(event ->
        {
            copyFileName_MenuItem_Action(event);
        });

        copyAbsoluteNamePath_MenuItem = new MenuItem(currentLanguage.getProperty("copyAbsolutePath_meuItem",
                "Copy Absolute path of file"));
        copyAbsoluteNamePath_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN,
                KeyCombination.ALT_DOWN));
        copyAbsoluteNamePath_MenuItem.setOnAction(event ->
        {
            copyAbsoluteNamePath_MenuItem_Action(event);
        });

        createNewTab_MenuItem = new MenuItem(currentLanguage.getProperty("createNewTab_menuItem",
                "Create new tab"));
        createNewTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        createNewTab_MenuItem.setOnAction(this::createNewTab_Action);
        createNewTab_MenuItem.setDisable(true);

        openTerminalHere_MenuItem = new MenuItem(currentLanguage.getProperty("openTerminalHere_menuItem",
                "Open terminal here"));
        openTerminalHere_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
        openTerminalHere_MenuItem.setOnAction(this::addActionToOpenTerminalMenuItem);

        openInNewTab_MenuItem = new MenuItem(currentLanguage.getProperty("openInNewTab_menuItem",
                "Open in new tab"));
        openInNewTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openInNewTab_MenuItem.setOnAction(this::openInNewTab_Action);

        language_Menu = new Menu(currentLanguage.getProperty("language_menu",
                "Language"));

        favoritesDialog_MenuItem = new MenuItem(currentLanguage.getProperty(
                "favorites_menuItem", "Favorites files"));
        favoritesDialog_MenuItem.setOnAction(this::favoritesMenuItem_Action);
        favoritesDialog_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.B,
                KeyCombination.CONTROL_DOWN));

        file_Menu = new Menu(currentLanguage.getProperty("file_menu",
                "Main"));
        file_Menu.getItems().addAll(createNewTab_MenuItem, openInNewTab_MenuItem, new SeparatorMenuItem(),
                copyFileName_MenuItem, copyAbsoluteNamePath_MenuItem, new SeparatorMenuItem(),
                favoritesDialog_MenuItem, openTerminalHere_MenuItem, new SeparatorMenuItem(),
                language_Menu, exit_MenuItem);

        goToParent_MenuItem = new MenuItem(currentLanguage.getProperty("goToParent_menuItem",
                "Parent directory"));
        goToParent_MenuItem.setOnAction(event -> goToParentPath(currentPath.get(currentContentTabIndex)));
        goToParent_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCodeCombination.CONTROL_DOWN));

        goToRootDirectories_MenuItem = new MenuItem(currentLanguage.getProperty("goToRootDirectories_menuItem",
                "Root directories"));
        goToRootDirectories_MenuItem.setOnAction(event ->
        {
            //goToDriveList();
            goToFileStoragesPane();
        });
        goToRootDirectories_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCodeCombination.ALT_DOWN));

        goToHomeDirectory_MenuItem = new MenuItem(currentLanguage.getProperty("goToHomeDirectory_menuItem",
                "Home directory"));
        goToHomeDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.home");
            goToPath(Paths.get(userDirectory));
        });
        goToHomeDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCodeCombination.ALT_DOWN));

        goToUserDirectory_MenuItem = new MenuItem(currentLanguage.getProperty("goToUserDirectory_menuItem",
                "User directory"));
        goToUserDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.dir");
            goToPath(Paths.get(userDirectory));
        });
        goToUserDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCodeCombination.ALT_DOWN));

        goToPreviousTab_MenuItem = new MenuItem(currentLanguage.getProperty("goToPreviousTab_menuItem",
                "Previous tab"));
        goToPreviousTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.PAGE_UP, KeyCombination.CONTROL_DOWN));
        goToPreviousTab_MenuItem.setOnAction(this::goToPreviousTab_Action);

        goToNextTab_MenuItem = new MenuItem(currentLanguage.getProperty("goToNextTab_menuItem",
                "Next tab"));
        goToNextTab_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.PAGE_DOWN, KeyCombination.CONTROL_DOWN));
        goToNextTab_MenuItem.setOnAction(this::goToNextTab_Action);

        goTo_Menu = new Menu(currentLanguage.getProperty("goTo_menu",
                "Go to..."));
        goTo_Menu.getItems().addAll(goToRootDirectories_MenuItem, goToHomeDirectory_MenuItem,
                goToUserDirectory_MenuItem, goToParent_MenuItem, new SeparatorMenuItem(),
                goToPreviousTab_MenuItem, goToNextTab_MenuItem);

        sortBy_ToggleGroup = new ToggleGroup();
        sortType_ToggleGroup = new ToggleGroup();

        sortByName_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortBy_menu",
                "Name"));
        sortByName_MenuItem.setSelected(true);
        sortByName_MenuItem.setOnAction(event -> requestSort(currentNameColumn, currentTypeColumn));
        sortByName_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortBySize_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortBySize_menuItem",
                "Size"));
        sortBySize_MenuItem.setOnAction(event -> requestSort(currentSizeBytesColumn, currentTypeColumn));
        sortBySize_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortByType_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortByType_menuItem",
                "Type"));
        sortByType_MenuItem.setOnAction(event -> requestSort(currentTypeColumn, currentTypeColumn));
        sortByType_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByType_MenuItem.setDisable(true);

        sortByOwner_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortByOwner_menuItem",
                "Owner"));
        sortByOwner_MenuItem.setOnAction(event -> requestSort(currentOwnerColumn, currentTypeColumn));
        sortByOwner_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByOwner_MenuItem.setDisable(true);
        sortByOwner_MenuItem.disableProperty().bind(currentOwnerColumn.visibleProperty().not());

        sortByCreationTime_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortByCreationTime_menuItem",
                "Creation time"));
        sortByCreationTime_MenuItem.setOnAction(event -> requestSort(currentCreationTimeColumn, currentTypeColumn));
        sortByCreationTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByCreationTime_MenuItem.setDisable(true);
        sortByCreationTime_MenuItem.disableProperty().bind(currentCreationTimeColumn.visibleProperty().not());

        sortByLastModifiedTime_MenuItem = new RadioMenuItem(currentLanguage.getProperty("sortByLastModifiedTime_menuItem",
                "Last modified time"));
        sortByLastModifiedTime_MenuItem.setOnAction(event -> requestSort(currentLastModifiedTimeColumn, currentTypeColumn));
        sortByLastModifiedTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByLastModifiedTime_MenuItem.setDisable(true);
        sortByLastModifiedTime_MenuItem.disableProperty().bind(currentLastModifiedTimeColumn.visibleProperty().not());

        ascendingSortType_MenuItem = new RadioMenuItem(currentLanguage.getProperty("ascending_menuItem",
                "Ascending"));
        ascendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        ascendingSortType_MenuItem.setSelected(true);
        ascendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.ASCENDING;
            requestSort(lastActiveSortColumn, currentTypeColumn);
        });

        descendingSortType_MenuItem = new RadioMenuItem(currentLanguage.getProperty("descending_menuItem",
                "Descending"));
        descendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        descendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.DESCENDING;
            requestSort(lastActiveSortColumn, currentTypeColumn);
        });

        directoriesFirst_MenuItem = new CheckMenuItem(currentLanguage.getProperty("directoriesFirst_menuItem",
                "Directories first"));
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

        sortBy_Menu = new Menu(currentLanguage.getProperty("sortBy_menu",
                "Sort by..."));
        sortBy_Menu.getItems().addAll(directoriesFirst_MenuItem, new SeparatorMenuItem(), sortByName_MenuItem,
                sortByType_MenuItem, sortBySize_MenuItem, sortByOwner_MenuItem,
                sortByCreationTime_MenuItem, sortByLastModifiedTime_MenuItem, new SeparatorMenuItem(),
                ascendingSortType_MenuItem, descendingSortType_MenuItem, new SeparatorMenuItem());


        edit_Menu = new Menu(currentLanguage.getProperty("edit_menu",
                "Edit"));

        delete_MenuItem = new MenuItem(currentLanguage.getProperty("delete_menuItem",
                "Delete"));
        delete_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        delete_MenuItem.setOnAction(this::delete_MenuItem_Action);

        rename_MenuItem = new MenuItem(currentLanguage.getProperty("rename_menuItem",
                "Rename"));
        rename_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        rename_MenuItem.setOnAction(this::rename_MenuItem_Action);

        createFile_MenuItem = new MenuItem(currentLanguage.getProperty("createFile_menuItem",
                "Create file"));
        createFile_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        createFile_MenuItem.setOnAction(this::createFile_MenuItem_Action);

        createDirectory_MenuItem = new MenuItem(currentLanguage.getProperty("createDirectory_menuItem",
                "Create directory"));
        createDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        createDirectory_MenuItem.setOnAction(this::createDirectory_MenuItem_Action);

        createSymbolicLink_MenuItem = new MenuItem(currentLanguage.getProperty("createSymbolicLink_menuItem",
                "Create Symbolic link"));
        createSymbolicLink_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        createSymbolicLink_MenuItem.setOnAction(this::createSymbolicLink_MenuItem_Action);

        copy_MenuItem = new MenuItem(currentLanguage.getProperty("copy_menuItem",
                "Copy"));
        copy_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copy_MenuItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = false;
        });

        paste_MenuItem = new MenuItem(currentLanguage.getProperty("paste_menuItem",
                "Paste"));
        paste_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        paste_MenuItem.setOnAction(this::pasteFiles_MenuItem_Action);

        move_MenuItem = new MenuItem(currentLanguage.getProperty("move_menuItem",
                "Move"));
        move_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        move_MenuItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = true;
        });
        move_MenuItem.setVisible(false);

        showOrEditAttributes_MenuItem = new MenuItem(currentLanguage.getProperty("attributes_menuItem",
                "Attributes"));
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
        favorites_List = new ArrayList<>();

        int favoritesNumber = Integer.parseInt(properties.getProperty(
                "favoritesNumber", "0"));

        for (int k = 0; k < favoritesNumber; k++)
        {
            favorites_List.add(Path.of(properties.getProperty(
                    "favoritePath_" + k)));
        }

        try
        {
            currentLanguage = new Properties();
            languagePath = properties.getProperty("languagePath", "/Languages/language_en.lang");
            System.out.println("Путь к языку: " + languagePath);
            currentLanguage.loadFromXML(this.getClass().getResourceAsStream(languagePath));
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
        catch (NullPointerException nullPointerException)
        {
            System.out.println("Файл перевода " + languagePath + " не был найден.");
        }

        CustomTreeTableColumn<FileData, String> nameColumn = new CustomTreeTableColumn<>(currentLanguage.getProperty(
                "name_column", "Name"));
        nameColumn.setMinWidth(preferredWidth * 0.1D);
        nameColumn.setPrefWidth(preferredWidth * 0.5D);
        nameColumn.setSortable(false);
        nameColumn.setId(NAME_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> sizeColumn = new CustomTreeTableColumn<>(
                currentLanguage.getProperty("size_column", "Size"));
        sizeColumn.setMinWidth(preferredWidth * 0.05D);
        sizeColumn.setPrefWidth(preferredWidth * 0.1D);
        sizeColumn.setSortable(false);
        sizeColumn.setId(SIZE_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> ownerColumn = new CustomTreeTableColumn<>(
                currentLanguage.getProperty("owner_column", "Owner"));
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

        CustomTreeTableColumn<FileData, String> lastModifiedTimeColumn = new CustomTreeTableColumn<>(
                currentLanguage.getProperty("lastModifiedTime_column", "Last modified Time"));
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

        CustomTreeTableColumn<FileData, String> creationTimeColumn = new CustomTreeTableColumn<>(
                currentLanguage.getProperty("creationTime_column", "Creation time"));
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

        CustomTreeTableColumn<FileData, String> typeColumn = new CustomTreeTableColumn<>(
                currentLanguage.getProperty("type_column", "Type"));
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
        content_TreeTableView.setOnKeyReleased(this::contentKeyReleased_Action);
        content_TreeTableView.getSortOrder().addAll(nameColumn, typeColumn);
        content_TreeTableView.getSelectionModel().selectedItemProperty().addListener(
                this::contentFilesTable_selectionChanged);

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

                temporaryCell.setTooltip(new FileNameTip(temporaryCell.getText()));
                temporaryCell.textProperty().addListener((eventT, oldText, newText) ->
                {
                    FileNameTip temporaryFileNameTip = (FileNameTip) temporaryCell.getTooltip();
                    temporaryFileNameTip.setLabelText(newText);
                });
                temporaryCell.setFont(cellFont);
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
                if (Files.isDirectory(newPath, LinkOption.NOFOLLOW_LINKS))
                {
                    goToPath(newPath);
                }
            }
        });
        currentPath_TextField.focusedProperty().addListener(this::currentPathTextField_Focus);

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
        content_TabPane.setTabMaxWidth(preferredWidth * 0.35D);
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

        info_ToolBar = new ToolBar();
        selectedItem_toolBarLabel = new Label(currentLanguage.getProperty("selectedItems_str",
                "Selected:"));
        selectedItem_toolBarLabel.setFont(Font.font(11.0D));

        selectedItemValue_toolBarLabel = new Label();
        selectedItemValue_toolBarLabel.setFont(Font.font(11.0D));

        selectedSize_toolBarLabel = new Label(currentLanguage.getProperty("size_str",
                "Size:"));
        selectedSize_toolBarLabel.setVisible(false);
        selectedSize_toolBarLabel.setFont(selectedItem_toolBarLabel.getFont());

        selectedSizeValue_toolBarLabel = new Label();
        selectedSizeValue_toolBarLabel.setVisible(false);
        selectedSizeValue_toolBarLabel.setFont(selectedItemValue_toolBarLabel.getFont());

        selectedSize_ProgressIndicator = new ProgressIndicator();
        selectedSize_ProgressIndicator.setPrefHeight(selectedSize_toolBarLabel
                .getBoundsInParent().getHeight());

        selectedRegularFiles_toolBarLabel = new Label("📄");
        selectedRegularFiles_toolBarLabel.setVisible(false);
        selectedRegularFiles_toolBarLabel.setFont(selectedSize_toolBarLabel.getFont());
        selectedRegularFiles_toolBarLabel.setTooltip(new FileNameTip("Files"));

        selectedRegularFilesValue_toolBarLabel = new Label();
        selectedRegularFilesValue_toolBarLabel.setVisible(false);
        selectedRegularFilesValue_toolBarLabel.setFont(selectedSizeValue_toolBarLabel.getFont());
        selectedRegularFilesValue_toolBarLabel.setTooltip(new FileNameTip("Files"));

        selectedDirectories_toolBarLabel = new Label("\uD83D\uDCC2");
        selectedDirectories_toolBarLabel.setVisible(false);
        selectedDirectories_toolBarLabel.setFont(selectedSize_toolBarLabel.getFont());
        selectedDirectories_toolBarLabel.setTooltip(new FileNameTip("Directories"));

        selectedDirectoriesValue_toolBarLabel = new Label();
        selectedDirectoriesValue_toolBarLabel.setVisible(false);
        selectedDirectoriesValue_toolBarLabel.setFont(selectedSizeValue_toolBarLabel.getFont());
        selectedDirectoriesValue_toolBarLabel.setTooltip(new FileNameTip("Directories"));


        info_ToolBar.getItems().addAll(selectedItem_toolBarLabel, selectedItemValue_toolBarLabel,
                new Separator(Orientation.VERTICAL), selectedSize_toolBarLabel,
                selectedSizeValue_toolBarLabel, new Separator(Orientation.VERTICAL),
                selectedRegularFiles_toolBarLabel,selectedRegularFilesValue_toolBarLabel,
                selectedDirectories_toolBarLabel,selectedDirectoriesValue_toolBarLabel,
                new Separator(Orientation.VERTICAL));

        content_VBox = new VBox(rem * 0.45D);
        content_VBox.setPadding(new Insets(rem * 0.15D, rem * 0.7D, rem * 0.7D, rem * 0.7D));
        content_VBox.getChildren().addAll(pathAndFileSystemName_HBox, content_TabPane,
                info_ToolBar);
        VBox.setVgrow(content_VBox, Priority.ALWAYS);
        VBox.setVgrow(content_TabPane, Priority.ALWAYS);
        VBox.setVgrow(files_VBox, Priority.ALWAYS);
        VBox.setVgrow(content_TreeTableView, Priority.ALWAYS);

    }

    /**
     * @deprecated Не надежный метод. Хорошо работает с Linux, но плохо с Win.
     */
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
            //System.out.println("First: '" + temporaryFileStorageFirstName + "'");
            //System.out.println("Second: '" + temporaryFileStorageSecondName + "'");

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
        contextMenuForFiles.setOnShowing(this::contextMenuForFiles_ShowingAction);

        /*Нужно создавать новые обьекты. Старые не отображаются.*/
        createFile_contextMenuForFilesItem = new MenuItem(createFile_MenuItem.getText());
        createFile_contextMenuForFilesItem.setOnAction(this::createFile_MenuItem_Action);

        createDirectory_contextMenuForFilesItem = new MenuItem(createDirectory_MenuItem.getText());
        createDirectory_contextMenuForFilesItem.setOnAction(this::createDirectory_MenuItem_Action);

        openInNewTab_contextMenuForFilesItem = new MenuItem(openInNewTab_MenuItem.getText());
        openInNewTab_contextMenuForFilesItem.setOnAction(this::openInNewTab_Action);


        openInSystem_contextMenuForFilesItem = new MenuItem(currentLanguage.getProperty(
                "openInSystem_contextMenuForFilesItem", "System"));
        openInSystem_contextMenuForFilesItem.setOnAction(this::openFileInSystem_Action);

        openInCustomProgram_contextMenuForFilesItem = new MenuItem(currentLanguage.getProperty("openInCustomProgram_contextMenuForFilesItem",
                "Select program..."));
        openInCustomProgram_contextMenuForFilesItem.setOnAction(this::openWithCustomProgram_Action);

        openWith_Menu = new Menu(currentLanguage.getProperty(
                "openWith_MenuContext", "Open with"));
        openWith_Menu.getItems().addAll(openInSystem_contextMenuForFilesItem,
                openInCustomProgram_contextMenuForFilesItem);

        openInDefaultProgram_contextMenuForFilesItem = new MenuItem(currentLanguage.getProperty(
                "open_contextMenuForFilesItem", "Open"));
        openInDefaultProgram_contextMenuForFilesItem.setOnAction(this::openInDefaultProgram_Action);

        createSymbolicLink_contextMenuForFilesItem = new MenuItem(createSymbolicLink_MenuItem.getText());
        createSymbolicLink_contextMenuForFilesItem.setOnAction(this::createSymbolicLink_MenuItem_Action);

        copyFileToClipboard_contextMenuForFilesItem = new MenuItem(copy_MenuItem.getText());
        copyFileToClipboard_contextMenuForFilesItem.setOnAction(this::copyFilesToClipboard_MenuItem_Action);

        moveFilesToClipboard_contextMenuForFilesItem = new MenuItem(move_MenuItem.getText());
        moveFilesToClipboard_contextMenuForFilesItem.setOnAction(event ->
        {
            copyFilesToClipboard_MenuItem_Action(event);
            filesToMoveFromClipboard = true;
        });

        pasteFileFromClipboard_contextMenuForFilesItem = new MenuItem(paste_MenuItem.getText());
        pasteFileFromClipboard_contextMenuForFilesItem.setOnAction(this::pasteFiles_MenuItem_Action);


        renameFile_contextMenuForFilesItem = new MenuItem(rename_MenuItem.getText());
        renameFile_contextMenuForFilesItem.setOnAction(this::rename_MenuItem_Action);

        deleteFile_contextMenuForFilesItem = new MenuItem(delete_MenuItem.getText());
        deleteFile_contextMenuForFilesItem.setOnAction(this::delete_MenuItem_Action);

        addToFavorites_contextMenuForFilesItem = new MenuItem(currentLanguage
                .getProperty("addToFavorites_contextMenuForFilesItem", "Add to favorites"));
        addToFavorites_contextMenuForFilesItem.setOnAction(this::addToFavorites_Action);

        contextMenuForFiles.getItems().addAll(openInDefaultProgram_contextMenuForFilesItem, openWith_Menu,
                openInNewTab_contextMenuForFilesItem, new SeparatorMenuItem(),
                createFile_contextMenuForFilesItem,
                createDirectory_contextMenuForFilesItem, createSymbolicLink_contextMenuForFilesItem,
                new SeparatorMenuItem(), addToFavorites_contextMenuForFilesItem,
                new SeparatorMenuItem(),
                copyFileToClipboard_contextMenuForFilesItem,
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
        startDirectoryWatching();

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

        /*Обнаружилась */
        activatedTreeTableView.refresh();
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
            temporaryTreeItem.setValue(null);
            temporaryTreeItem.setGraphic(null);
            temporaryTreeItem.setValue(temporaryFileData);
        }
        return temporaryTreeItem;
    }

    private FileData reuseFileDataObjects(final int fileDataIndex)
    {
        FileData temporaryFileData = null;
        if (fileDataIndex >= fileDataSaver.size())
        {
            temporaryFileData = new FileData("", 1L);
            fileDataSaver.add(temporaryFileData);
        }
        else
        {
            temporaryFileData = fileDataSaver.get(fileDataIndex);
        }
        return temporaryFileData;
    }

    /**
     * Возвращается к родительскому элементу пути и выделяет его.
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

            String fileName = path.getFileName().toString();
            int indexToSelect = -1;
            if (fileName != null)
            {
                ObservableList<TreeItem<FileData>> items = activatedTreeTableView.getRoot().getChildren();
                for (int k = 0; k < items.size(); k++)
                {
                    if (items.get(k).getValue().getName().equals(fileName))
                    {
                        indexToSelect = k;
                        break;
                    }
                }
            }
            if (indexToSelect != -1)
            {
                activatedTreeTableView.getSelectionModel().select(indexToSelect);
                activatedTreeTableView.scrollTo(indexToSelect - 2);
            }
            else
            {
                activatedTreeTableView.getSelectionModel().select(0);
                activatedTreeTableView.scrollTo(0);
            }
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


    private ImagePreviewer imagePreviewer;
    private FavoritesDialog favoritesDialog;
    private FolderWatcher folderWatcher;
    private Thread folderWatcherThread;

    /**
     * Обработчик событий нажатия клавиши для таблицы файлов.
     */
    private void contentKeyReleased_Action(KeyEvent event)
    {
        KeyCode keyRelease_KeyCode = event.getCode();

        if (keyRelease_KeyCode == KeyCode.ENTER)
        {
            if (quickSearchInCurrentFolder_Popup != null &&
                    quickSearchInCurrentFolder_Popup.isShowing())
            {
                quickSearchInCurrentFolder_Popup.hide();
            }

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
        else if (keyRelease_KeyCode == KeyCode.CONTEXT_MENU)
        {
            Point2D p = activatedTreeTableView.localToScreen(0, 0);
            //Bounds b = activatedTreeTableView.localToScreen(activatedTreeTableView.getBoundsInLocal());
            //System.out.println("X: " + p.getX()+ "\tY: " + p.getY());
            contextMenuForFiles.show(stage, p.getX(), p.getY());
        }
        else if (keyRelease_KeyCode == KeyCode.F11)
        {

            for (File f : File.listRoots())
            {
                System.out.println(f.getAbsolutePath());
//                try
//                {
//                    FileStoreAttributeView p = Files.getFileStore(tPath).getFileStoreAttributeView(FileStoreAttributeView.class);
//                    System.out.println(p.name());
//                }
//                catch(IOException ioException)
//                {
//                    ioException.printStackTrace();
//                }

            }
//            for(Path tPath : FileSystems.getDefault().getRootDirectories())
//            {
//                System.out.println(tPath.toAbsolutePath().toString());
//                try
//                {
//                    FileStoreAttributeView p = Files.getFileStore(tPath).getFileStoreAttributeView(FileStoreAttributeView.class);
//                    System.out.println(p.name());
//                }
//                catch(IOException ioException)
//                {
//                    ioException.printStackTrace();
//                }
//
//            }
//            for(FileStore tempStore : FileSystems.getDefault().getFileStores())
//            {
//                tempStore.
//            }
        }
        else if (keyRelease_KeyCode == KeyCode.SPACE)
        {
            System.out.println("Space");
            Path imagePath = (currentPath.get(currentContentTabIndex).resolve(
                    activatedTreeTableView.getSelectionModel()
                            .getSelectedItem().getValue().getName()));
            String extension = getExtension(imagePath);

            if (imagePreviewer == null)
            {
                imagePreviewer = new ImagePreviewer();
            }

            if (extension != null && (extension.toLowerCase().equals("jpg")
                    || extension.toLowerCase().equals("png")))
            {
                try
                {
                    Image image = new Image(Files.newInputStream(imagePath));
                    imagePreviewer.setImage(image);
                    imagePreviewer.setMaxSizePreview(25);
                }
                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }
            imagePreviewer.show(stage, true);
        }
        else if (keyRelease_KeyCode.isLetterKey() && !event.isControlDown()
                && !event.isAltDown() && !event.isMetaDown() && !event.isShortcutDown())
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
            deleteFileDialog = new DeleteFileDialog(this, currentLanguage);
        }
        deleteFileDialog.setTargetPaths(paths);
        deleteFileDialog.show();
    }

    /**
     * Нужна для обработки результатов удаления.
     */
    public void updateFilesListAfterDeleting(final DeleteOperationResult deleteOperationResult)
    {
        if (true)
        {
            return;
        }
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
            showLittleNotification(stage, currentLanguage.getProperty("nameOfFileHasBeenSuccessfullyCopied_str",
                    "Name of file has been successfully copied."), 3);
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
                        //addRowToTreeTable(resultPath);
                        //requestSort(lastActiveSortColumn, lastActiveSortColumn);
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
                        //addRowToTreeTable(resultPath);
                        //requestSort(lastActiveSortColumn, lastActiveSortColumn);
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
            copyFilesDialog = new CopyFilesDialog(this, currentLanguage);
        }
        copyFilesDialog.setPathsToCopy(sourceFilesPaths, destinationFilesPaths);
        copyFilesDialog.setMoveOperation(filesToMoveFromClipboard);
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
        filesToMoveFromClipboard = false;
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
        newTreeTableView.setOnKeyReleased(this::contentKeyReleased_Action);
        newTreeTableView.setContextMenu(contextMenuForFiles);
        newTreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        CustomTreeTableColumn<FileData, String> newNameColumn = new CustomTreeTableColumn<>(
                currentNameColumn.getText());
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
                temporaryCell.setFont(cellFont);
                temporaryCell.setPadding(new Insets(0.0D, 0.0D, 0.0, rem * 0.2D));
                temporaryCell.setOnMouseClicked(event ->
                {
                    cellMouseClicked_Action(event);
                });
                return temporaryCell;
            }
        });

        CustomTreeTableColumn<FileData, String> newSizeColumn = new CustomTreeTableColumn<>(
                currentSizeColumn.getText());
        newSizeColumn.setMinWidth(preferredWidth * 0.05D);
        newSizeColumn.setPrefWidth(preferredWidth * 0.1D);
        newSizeColumn.setSortable(false);
        newSizeColumn.setContextMenu(contextMenuForColums);
        newSizeColumn.setId(SIZE_COLUMN_ID);

        CustomTreeTableColumn<FileData, String> newOwnerColumn = new CustomTreeTableColumn<>(
                currentOwnerColumn.getText());
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

        CustomTreeTableColumn<FileData, String> newLastModifiedTimeColumn = new CustomTreeTableColumn<>(
                currentLastModifiedTimeColumn.getText());
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

        CustomTreeTableColumn<FileData, String> newCreationTimeColumn = new CustomTreeTableColumn<>(
                currentCreationTimeColumn.getText());
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

        CustomTreeTableColumn<FileData, String> newTypeColumn = new CustomTreeTableColumn<>(
                currentTypeColumn.getText());
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
        newTreeTableView.getSelectionModel().selectedItemProperty().addListener(
                this::contentFilesTable_selectionChanged);

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


        fileAttributesEditor = new FileAttributesEditor(temporaryPath,
                currentLanguage);
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
                    currentContentTabIndex), currentLanguage);
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

    private void openInNewTab_Action(ActionEvent event)
    {
        Path temporarySelectedFilePath = currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName());

        if (activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().isFile())
        {
            temporarySelectedFilePath = temporarySelectedFilePath.getParent();
        }

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
            OpenWithCustomProgramDialog openWithDialog = new OpenWithCustomProgramDialog(temporaryFilePath,
                    currentLanguage);
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

    private void contextMenuForFiles_ShowingAction(WindowEvent event)
    {
        if (activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().isDirectory())
        {
            //openInCustomProgram_contextMenuForFilesItem.setVisible(false);
            openWith_Menu.setVisible(false);
        }
        else
        {
            if (!openWith_Menu.isVisible())
            {
                //openInCustomProgram_contextMenuForFilesItem.setVisible(true);
                openWith_Menu.setVisible(true);
            }
        }
    }

    private void openFileInSystem_Action(ActionEvent event)
    {
        ProcessBuilder processBuilder = new ProcessBuilder(currentPath.get(currentContentTabIndex).resolve(
                activatedTreeTableView.getSelectionModel().getSelectedItem().getValue().getName()).toAbsolutePath().toString());
        try
        {
            Process process = processBuilder.start();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    /**
     * Читает файл с настройками и только.
     */
    private void loadPropertyFile()
    {
        Path pathToProgram = null;
        try
        {
            pathToProgram = Path.of(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (URISyntaxException ioException)
        {
            ioException.printStackTrace();
        }
        if (Files.isRegularFile(pathToProgram, LinkOption.NOFOLLOW_LINKS))
        {
            pathToProgram = pathToProgram.getParent();
        }

        Path pathToProperties = pathToProgram.resolve("general.properties");
        System.out.println("Property file: " + pathToProperties.toAbsolutePath().toString());

        try
        {
            properties.loadFromXML(Files.newInputStream(pathToProperties, StandardOpenOption.READ));
        }
        catch (NoSuchFileException noSuchFileException)
        {
            System.out.println("Файл с настройками не найден по пути: "
                    + pathToProperties.toAbsolutePath().toString());
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }


    /**
     * Записывает в объект настроек необходимые параметры и сохраняет файл рядом
     * с программой.
     */
    private void saveGeneralPropertiesBeforeClose()
    {
        long startTime = System.nanoTime();
        Path pathToProgram = null;
        try
        {
            pathToProgram = Path.of(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (URISyntaxException ioException)
        {
            ioException.printStackTrace();
        }
        if (Files.isRegularFile(pathToProgram, LinkOption.NOFOLLOW_LINKS))
        {
            pathToProgram = pathToProgram.getParent();
        }

        Path pathToProperties = pathToProgram.resolve("general.properties");

        properties.setProperty("locationX", "" + stage.getX());
        properties.setProperty("locationY", "" + stage.getY());
        properties.setProperty("stageWidth", "" + stage.getWidth());
        properties.setProperty("stageHeight", "" + stage.getHeight());
        properties.setProperty("tabsNumber", "" + content_TabPane.getTabs().size());
        properties.setProperty("languagePath", languagePath);
        //------------ Сохранение путей во всех вкладках
        for (int k = 0; k < currentPath.size(); k++)
        {
            properties.setProperty("tabPath_" + k, currentPath.get(k)
                    .toAbsolutePath().toString());
        }


        int favoritesNumber = -1;
        if (favoritesDialog != null)
        {
            favorites_List = favoritesDialog.getFavoritesList();
            favoritesNumber = favorites_List.size();
        }
        else
        {
            favoritesNumber = favorites_List.size();
        }

        properties.setProperty("favoritesNumber", "" + favoritesNumber);
        //------------ Сохранение избранных

        for (int k = 0; k < favorites_List.size(); k++)
        {
            properties.setProperty("favoritePath_" + k, favorites_List.get(k)
                    .toAbsolutePath().toString());
        }
//        properties.setProperty("lastPath", currentPath.get(currentContentTabIndex)
//                .toAbsolutePath().toString());

        try
        {
            properties.storeToXML(Files.newOutputStream(pathToProperties, StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE),
                    "", StandardCharsets.UTF_8);
            System.out.println("Файл настроек сохранен: " + pathToProperties.toAbsolutePath().toString()
                    + "\nВремени на сохранение настроек после закрытия: " + (System.nanoTime() - startTime));

        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void stageOnHidden(WindowEvent event)
    {
        System.out.println("Stage спрятана.");
        saveGeneralPropertiesBeforeClose();
    }

    /**
     * Считывает с файла доступные языки и создает соответсвующие элементы меню.
     */
    private void initializeLanguageMenuItems()
    {
        try
        {
            Properties languageList = new Properties();
            Properties languagesIcons_Properties = new Properties();

            languageList.loadFromXML(this.getClass().getResourceAsStream("/Languages/language_list.properties"));
            languagesIcons_Properties.loadFromXML(this.getClass().getResourceAsStream(
                    "/Languages/language_icons.properties"));

            for (Object languageName : languageList.keySet())
            {
                MenuItem temporaryMenuItem = new MenuItem(languageName.toString());
                temporaryMenuItem.setOnAction(event ->
                {
                    currentLanguage = new Properties();
                    languagePath = "/Languages/" + languageList.getProperty(languageName.toString(),
                            "language_en.lang");

                    System.out.println("Смена языка на: " + languageName);
                    try
                    {
                        currentLanguage.loadFromXML(this.getClass().getResourceAsStream(languagePath));
                    }
                    catch (IOException ioException)
                    {
                        ioException.printStackTrace();
                    }

                    applyNewLanguage();
                });

                Image countryIcon = new Image(this.getClass().getResourceAsStream(
                        languagesIcons_Properties.getProperty(languageName.toString(), "default.png")));

                ImageView country_ImageView = new ImageView(countryIcon);
                country_ImageView.setPreserveRatio(true);
                country_ImageView.setSmooth(true);
                country_ImageView.setFitWidth(24.0D);

                temporaryMenuItem.setGraphic(country_ImageView);

                language_Menu.getItems().add(temporaryMenuItem);
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }

    }


    private void applyNewLanguage()
    {
        file_Menu.setText(currentLanguage.getProperty("file_menu", "Main"));
        createNewTab_MenuItem.setText(currentLanguage.getProperty("createNewTab_menuItem", "Create new tab"));
        copyFileName_MenuItem.setText(currentLanguage.getProperty("copyFileName_menuItem", "Copy name of file"));
        copyAbsoluteNamePath_MenuItem.setText(currentLanguage.getProperty("copyAbsolutePath_meuItem", "Copy absolute path of file"));
        openTerminalHere_MenuItem.setText(currentLanguage.getProperty("openTerminalHere_menuItem", "Open terminal here"));
        openInNewTab_MenuItem.setText(currentLanguage.getProperty("openInNewTab_menuItem", "Open in new tab"));
        favoritesDialog_MenuItem.setText(currentLanguage.getProperty("favorites_menuItem","Favorites files"));
        language_Menu.setText(currentLanguage.getProperty("language_menu", "Language"));
        exit_MenuItem.setText(currentLanguage.getProperty("exit_menuItem", "Exit"));

        edit_Menu.setText(currentLanguage.getProperty("edit_menu", "Edit"));
        createDirectory_MenuItem.setText(currentLanguage.getProperty("createDirectory_menuItem", "Create directory"));
        createFile_MenuItem.setText(currentLanguage.getProperty("createFile_menuItem", "Create file"));
        rename_MenuItem.setText(currentLanguage.getProperty("rename_menuItem", "Rename"));
        delete_MenuItem.setText(currentLanguage.getProperty("delete_menuItem", "Delete"));
        createSymbolicLink_MenuItem.setText(currentLanguage.getProperty("createSymbolicLink_menuItem", "Create Symbolic link"));
        copy_MenuItem.setText(currentLanguage.getProperty("copy_menuItem", "Copy"));
        paste_MenuItem.setText(currentLanguage.getProperty("paste_menuItem", "Paste"));
        move_MenuItem.setText(currentLanguage.getProperty("move_menuItem", "Move"));
        showOrEditAttributes_MenuItem.setText(currentLanguage.getProperty("attributes_menuItem", "Attributes"));

        goTo_Menu.setText(currentLanguage.getProperty("goTo_menu", "Go to..."));
        goToPreviousTab_MenuItem.setText(currentLanguage.getProperty("goToPreviousTab_menuItem", "Previous tab"));
        goToNextTab_MenuItem.setText(currentLanguage.getProperty("goToNextTab_menuItem", "Next tab"));
        goToHomeDirectory_MenuItem.setText(currentLanguage.getProperty("goToHomeDirectory_menuItem", "Home directory"));
        goToParent_MenuItem.setText(currentLanguage.getProperty("goToParent_menuItem", "Parent directory"));

        sortBy_Menu.setText(currentLanguage.getProperty("sortBy_menu", "Sort by..."));
        sortByName_MenuItem.setText(currentLanguage.getProperty("sortByName_menuItem", "Name"));
        sortBySize_MenuItem.setText(currentLanguage.getProperty("sortBySize_menuItem", "Size"));
        sortByOwner_MenuItem.setText(currentLanguage.getProperty("sortByOwner_menuItem", "Owner"));
        sortByType_MenuItem.setText(currentLanguage.getProperty("sortByType_menuItem", "Type"));
        sortByCreationTime_MenuItem.setText(currentLanguage.getProperty("sortByCreationTime_menuItem", "Creation time"));
        sortByLastModifiedTime_MenuItem.setText(currentLanguage.getProperty("sortByLastModifiedTime_menuItem", "Last modified time"));
        ascendingSortType_MenuItem.setText(currentLanguage.getProperty("ascending_menuItem", "Ascending"));
        descendingSortType_MenuItem.setText(currentLanguage.getProperty("descending_menuItem", "Descending"));
        directoriesFirst_MenuItem.setText(currentLanguage.getProperty("directoriesFirst_menuItem", "Directories first"));

        currentNameColumn.setText(currentLanguage.getProperty("name_column", "Name"));
        currentTypeColumn.setText(currentLanguage.getProperty("type_column", "Type"));
        currentSizeColumn.setText(currentLanguage.getProperty("size_column", "Size"));
        currentOwnerColumn.setText(currentLanguage.getProperty("owner_column", "Owner"));
        currentCreationTimeColumn.setText(currentLanguage.getProperty("creationTime_column", "Creation time"));
        currentLastModifiedTimeColumn.setText(currentLanguage.getProperty("lastModifiedTime_column", "Last modified time"));

        openInDefaultProgram_contextMenuForFilesItem.setText(currentLanguage.getProperty("open_contextMenuForFilesItem",
                "Open"));
        openInNewTab_contextMenuForFilesItem.setText(openInNewTab_MenuItem.getText());
        createFile_contextMenuForFilesItem.setText(createFile_MenuItem.getText());
        createDirectory_contextMenuForFilesItem.setText(createDirectory_MenuItem.getText());
        createSymbolicLink_contextMenuForFilesItem.setText(createSymbolicLink_MenuItem.getText());
        copyFileToClipboard_contextMenuForFilesItem.setText(copy_MenuItem.getText());
        renameFile_contextMenuForFilesItem.setText(rename_MenuItem.getText());
        deleteFile_contextMenuForFilesItem.setText(delete_MenuItem.getText());
        pasteFileFromClipboard_contextMenuForFilesItem.setText(paste_MenuItem.getText());
        moveFilesToClipboard_contextMenuForFilesItem.setText(move_MenuItem.getText());
        openInSystem_contextMenuForFilesItem.setText(currentLanguage.getProperty("openInSystem_contextMenuForFilesItem", "System"));
        openWith_Menu.setText(currentLanguage.getProperty("openWith_MenuContext", "Open with"));
        openInCustomProgram_contextMenuForFilesItem.setText(currentLanguage.getProperty("openInCustomProgram_contextMenuForFilesItem",
                "Custom program..."));
        addToFavorites_contextMenuForFilesItem.setText(currentLanguage.getProperty("addToFavorites_contextMenuForFilesItem",
                "Add to favorites"));

        selectedItem_toolBarLabel.setText(currentLanguage.getProperty("selectedItems_str",
                "Selected:"));
        selectedSize_toolBarLabel.setText(currentLanguage.getProperty("size_str",
                "Size:"));
    }

    private void addToFavorites_Action(ActionEvent event)
    {
        ObservableList<TreeItem<FileData>> selectedItems = activatedTreeTableView
                .getSelectionModel().getSelectedItems();

        if (favoritesDialog == null)
        {
            for (int k = 0; k < selectedItems.size(); k++)
            {
                favorites_List.add(currentPath
                        .get(currentContentTabIndex).resolve(selectedItems.get(k)
                                .getValue().getName()));
            }
        }
        else
        {
            for (int k = 0; k < selectedItems.size(); k++)
            {
                favoritesDialog.addFavoriteFile(currentPath
                        .get(currentContentTabIndex).resolve(selectedItems.get(k)
                                .getValue().getName()));
            }
        }
    }

    private void favoritesMenuItem_Action(ActionEvent event)
    {
        if (favoritesDialog == null)
        {
            if (favorites_List.size() > 0)
            {
                favoritesDialog = new FavoritesDialog(favorites_List,
                        currentLanguage, this);
            }
            else
            {
                favoritesDialog = new FavoritesDialog(currentLanguage, this);
            }
        }
        favoritesDialog.show();
    }

    public void goToFavoriteFile(final Path targetPath)
    {
        createNewTab_Action(null);
        currentPath.remove(currentPath.size() - 1);

        Path favoriteFilePath = targetPath;
        if (Files.isRegularFile(targetPath, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(targetPath))
        {
            favoriteFilePath = targetPath.getParent();
        }

        currentPath.add(favoriteFilePath);
        content_TabPane.getSelectionModel().select(content_TabPane
                .getTabs().size() - 1);

        ObservableList<TreeItem<FileData>> items = activatedTreeTableView.getRoot().getChildren();
        //Виделить нужный елемент
        for (int k = 0; k < items.size(); k++)
        {
            if (items.get(k).getValue().getName().equals(targetPath.getFileName().toString()))
            {
                activatedTreeTableView.getSelectionModel().clearSelection();
                activatedTreeTableView.getSelectionModel().select(k);
                activatedTreeTableView.scrollTo(k - 2);
                break;
            }
        }
    }

    private void startDirectoryWatching()
    {
        if (folderWatcher != null)
        {
            try
            {
                folderWatcher.stopWatchService();
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }

        folderWatcher = new FolderWatcher(currentPath.get(currentContentTabIndex), this);
        folderWatcherThread = new Thread(folderWatcher);

        System.out.println("Запуск слушателя каталогов.");
        folderWatcherThread.start();
    }

    /**
     * Удаляет елемент из таблицы по его названию. Работает только до
     * первого вхождения.
     */
    private void removeRowByName(final String fileName)
    {
        ObservableList<TreeItem<FileData>> items = activatedTreeTableView
                .getRoot().getChildren();
        for (int k = 0; k < activatedTreeTableView.getRoot().getChildren().size();
             k++)
        {
            if (items.get(k).getValue().getName().equals(fileName))
            {
                items.remove(k);
                break;
            }
        }
    }

    public void directoryWatchOccured(final Path eventPath, final WatchEvent.Kind<Path> kind)
    {
        System.out.println("File: " + eventPath.toString()
                + "\nKind: " + kind.name());

        if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind))
        {
            System.out.println("Добавляем файл ибо услышали.");
            addRowToTreeTable(currentPath.get(currentContentTabIndex).resolve(eventPath));
        }
        else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind))
        {
            removeRowByName(eventPath.toString());
        }
    }


    private void currentPathTextField_Focus(Observable observable)
    {
        if (!currentPath_TextField.isFocused() && currentPath != null
                && currentPath.size() > currentContentTabIndex &&
                currentPath.get(currentContentTabIndex) != null)
        {
            currentPath_TextField.setText(currentPath.get(currentContentTabIndex)
                    .toAbsolutePath().toString());
        }
    }

    private void contentFilesTable_selectionChanged(ObservableValue<?> oValue, TreeItem<FileData> value1,
                                                    TreeItem<FileData> value2)
    {
        if (selectedItemValue_toolBarLabel != null)
        {
            selectedItemValue_toolBarLabel.setText("" + activatedTreeTableView.getSelectionModel()
                    .getSelectedItems().size());
        }

        createAndStartSelectedFilesSizeThreadForInfoToolBar();
    }


    public void createAndStartSelectedFilesSizeThreadForInfoToolBar()
    {
        FilesNumberAndSizeCalculator totalSizeForToolBarChanger = new FilesNumberAndSizeCalculator()
        {
            @Override
            public void appearInNodes(int aFilesNumber, long aTotalSize)
            {

            }

            @Override
            public void appearInNodes(int aFilesNumber, int aRegularFilesNumber,
                                      int aDirectoriesNumber, long aTotalSize)
            {
                if (!selectedSize_toolBarLabel.isVisible())
                {
                    selectedSize_toolBarLabel.setVisible(true);
                    selectedSizeValue_toolBarLabel.setVisible(true);
                }

                selectedSizeValue_toolBarLabel.setText(StorageCapacity.ofBytes(aTotalSize)
                        .getSpace(StorageSpaceFormatter.ONLY_BIGGEST_VALUE_FORMAT));
                selectedRegularFilesValue_toolBarLabel.setText("" + NumberFormat.getNumberInstance()
                        .format(aRegularFilesNumber));
                selectedDirectoriesValue_toolBarLabel.setText("" + NumberFormat.getNumberInstance()
                        .format(aDirectoriesNumber));

                info_ToolBar.getItems().remove(selectedSize_ProgressIndicator);
                {
                    selectedSize_toolBarLabel.setVisible(true);
                    selectedSizeValue_toolBarLabel.setVisible(true);
                    selectedRegularFiles_toolBarLabel.setVisible(true);
                    selectedRegularFilesValue_toolBarLabel.setVisible(true);
                    selectedDirectories_toolBarLabel.setVisible(true);
                    selectedDirectoriesValue_toolBarLabel.setVisible(true);
                }
            }
        };


        if (fileSizeCounterForToolBar != null &&
                totalSizeCounterForToolBar_Thread.isAlive())
        {
            fileSizeCounterForToolBar.interrupt();
        }

        //Подготовить файлы
        ObservableList<TreeItem<FileData>> items = activatedTreeTableView.getSelectionModel()
                .getSelectedItems();

        Path[] targetPaths = new Path[items.size()];
        Path temporaryCurrentPath = currentPath.get(currentContentTabIndex);

        for (int k = 0; k < targetPaths.length; k++)
        {
            targetPaths[k] = temporaryCurrentPath.resolve(items.get(k).getValue()
                    .getName());
        }

        fileSizeCounterForToolBar = new SimpleFileSizeAndNumberCounter(targetPaths,
                totalSizeForToolBarChanger);

        totalSizeCounterForToolBar_Thread = new Thread(fileSizeCounterForToolBar);
        totalSizeCounterForToolBar_Thread.start();

        if (!info_ToolBar.getItems().contains(selectedSize_ProgressIndicator))
        {
            selectedSize_toolBarLabel.setVisible(false);
            selectedSizeValue_toolBarLabel.setVisible(false);
            selectedRegularFiles_toolBarLabel.setVisible(false);
            selectedRegularFilesValue_toolBarLabel.setVisible(false);
            selectedDirectories_toolBarLabel.setVisible(false);
            selectedDirectoriesValue_toolBarLabel.setVisible(false);
            info_ToolBar.getItems().add(info_ToolBar.getItems().indexOf(selectedSize_toolBarLabel),
                    selectedSize_ProgressIndicator);
        }

    }

}
