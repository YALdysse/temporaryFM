package org.yaldysse.fm;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.xml.stream.XMLOutputFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FM_GUI extends Application
{
    private Stage stage;
    private Scene scene;
    private VBox root;
    private VBox content_VBox;
    private BorderPane menu_BorderPane;
    private TextField currentPath_TextField;
    private TreeTableView<FileData> content_TreeTableView;
    private CustomTreeTableColumn<FileData, String> nameColumn;
    private CustomTreeTableColumn<FileData, Long> sizeColumn;
    private CustomTreeTableColumn<FileData, String> ownerColumn;
    private CustomTreeTableColumn<FileData, String> lastModifiedTimeColumn;
    private CustomTreeTableColumn<FileData, String> creationTimeColumn;
    private CustomTreeTableColumn<FileData, String> typeColumn;
    private TreeTableCell<FileData, String> treeTableCell;
    private TreeItem<FileData> rootItem;
    private ContextMenu contextMenuForColums;
    private ArrayList<TreeTableColumn> allColumns;


    public static final double rem = new Text("").getBoundsInParent().getHeight();
    private double preferredWidth = rem * 30.0D;
    private double preferredHeight = rem * 20.0D;
    public final String FIlE_SYSTEMS_PATH = "file systems:///";
    private Path currentPath;
    private TreeTableColumn.SortType sortType;
    private CustomTreeTableColumn<FileData, ?> lastActiveSortColumn;

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

    private CheckMenuItem autoSizeColumn_MenuItem;

    private ConfirmOperationDialog confirmOperationDialog;


    @Override
    public void start(Stage primaryStage) throws Exception
    {
        stage = primaryStage;

        root = new VBox(rem * 0.45D);
        //root.setPadding(new Insets(rem * 0.55D));

        scene = new Scene(root);

        initializeComponents();
        FileSystem curfs = FileSystems.getDefault();

        Iterable<Path> iterable = curfs.getRootDirectories();
        for (Path path : iterable)
        {
            addCustomToContentInTable(FIlE_SYSTEMS_PATH, path.toString());
        }

        stage.setScene(scene);
        stage.setMinHeight(preferredHeight);
        stage.setMinWidth(preferredWidth);
        stage.show();
    }

    private void initializeComponents()
    {
        initializeGeneral();
        initializeMenu();
        initializeContextMenuForColumns();

        confirmOperationDialog = new ConfirmOperationDialog(StageStyle.UTILITY,
                "");
        confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.initOwner(stage);
        confirmOperationDialog.initModality(Modality.APPLICATION_MODAL);

        root.getChildren().addAll(menu_BorderPane, content_VBox);
        content_TreeTableView.requestFocus();
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
        copyFileName_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHIFT_DOWN));
        copyFileName_MenuItem.setOnAction(event ->
        {
            copyFileName_MenuItem_Action(event);
        });

        copyAbsoluteNamePath_MenuItem = new MenuItem("Copy absolute path of file");
        copyAbsoluteNamePath_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN));
        copyAbsoluteNamePath_MenuItem.setOnAction(event ->
        {
            copyAbsoluteNamePath_MenuItem_Action(event);
        });

        file_Menu = new Menu("Main");
        file_Menu.getItems().addAll(copyFileName_MenuItem, copyAbsoluteNamePath_MenuItem, new SeparatorMenuItem(), exit_MenuItem);

        goToParent_MenuItem = new MenuItem("Parent directory");
        goToParent_MenuItem.setOnAction(event ->
        {
            goToParentPath(currentPath);
        });
        goToParent_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCodeCombination.CONTROL_DOWN));

        goToRootDirectories_MenuItem = new MenuItem("Root directories");
        goToRootDirectories_MenuItem.setOnAction(event ->
        {
            for (Path rootPath : FileSystems.getDefault().getRootDirectories())
            {
                addCustomToContentInTable(FIlE_SYSTEMS_PATH, rootPath.toString());
            }
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


        goTo_Menu = new Menu("Go to...");
        goTo_Menu.getItems().addAll(goToRootDirectories_MenuItem, goToHomeDirectory_MenuItem,
                goToUserDirectory_MenuItem, goToParent_MenuItem);

        sortBy_ToggleGroup = new ToggleGroup();
        sortType_ToggleGroup = new ToggleGroup();

        sortByName_MenuItem = new RadioMenuItem("by Name");
        sortByName_MenuItem.setSelected(true);
        sortByName_MenuItem.setOnAction(event ->
        {
            requestSort(nameColumn);
        });
        sortByName_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortBySize_MenuItem = new RadioMenuItem("by Size");
        sortBySize_MenuItem.setOnAction(event ->
        {
            requestSort(sizeColumn);
        });
        sortBySize_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortByType_MenuItem = new RadioMenuItem("by Type");
        sortByType_MenuItem.setOnAction(event ->
        {
            requestSort(typeColumn);
        });
        sortByType_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByType_MenuItem.setDisable(true);

        sortByOwner_MenuItem = new RadioMenuItem("by Owner");
        sortByOwner_MenuItem.setOnAction(event ->
        {
            requestSort(ownerColumn);
        });
        sortByOwner_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByOwner_MenuItem.setDisable(true);
        sortByOwner_MenuItem.disableProperty().bind(ownerColumn.visibleProperty().not());

        sortByCreationTime_MenuItem = new RadioMenuItem("by Creation Time");
        sortByCreationTime_MenuItem.setOnAction(event ->
        {
            requestSort(creationTimeColumn);
        });
        sortByCreationTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByCreationTime_MenuItem.setDisable(true);
        sortByCreationTime_MenuItem.disableProperty().bind(creationTimeColumn.visibleProperty().not());

        sortByLastModifiedTime_MenuItem = new RadioMenuItem("by Last modified Time");
        sortByLastModifiedTime_MenuItem.setOnAction(event ->
        {
            requestSort(lastModifiedTimeColumn);
        });
        sortByLastModifiedTime_MenuItem.setToggleGroup(sortBy_ToggleGroup);
        sortByLastModifiedTime_MenuItem.setDisable(true);
        sortByLastModifiedTime_MenuItem.disableProperty().bind(lastModifiedTimeColumn.visibleProperty().not());

        ascendingSortType_MenuItem = new RadioMenuItem("Ascending");
        ascendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        ascendingSortType_MenuItem.setSelected(true);
        ascendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.ASCENDING;
            requestSort(lastActiveSortColumn);
        });

        descendingSortType_MenuItem = new RadioMenuItem("Descending");
        descendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        descendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.DESCENDING;
            requestSort(lastActiveSortColumn);
        });

        directoriesFirst_MenuItem = new CheckMenuItem("Directories first");
        directoriesFirst_MenuItem.setSelected(true);
        directoriesFirst_MenuItem.setOnAction(event ->
        {
            requestSort(lastActiveSortColumn);
        });
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
        delete_MenuItem.setOnAction(event ->
        {
            delete_MenuItem_Action();
        });

        rename_MenuItem = new MenuItem("Rename");
        rename_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        rename_MenuItem.setOnAction(event ->
        {
            rename_MenuItem_Action();
        });

        createFile_MenuItem = new MenuItem("Create file");
        createFile_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        createFile_MenuItem.setOnAction(this::createFile_MenuItem_Action);

        createDirectory_MenuItem = new MenuItem("Create directory");
        createDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        createDirectory_MenuItem.setOnAction(this::createDirectory_MenuItem_Action);

        edit_Menu.getItems().addAll(createFile_MenuItem, createDirectory_MenuItem,
                new SeparatorMenuItem(), rename_MenuItem, delete_MenuItem);

        menu_Bar = new MenuBar(file_Menu, edit_Menu, goTo_Menu, sortBy_Menu);

        menu_BorderPane = new BorderPane();
        menu_BorderPane.setTop(menu_Bar);
    }

    private void initializeGeneral()
    {
        nameColumn = new CustomTreeTableColumn<>("Name");
        nameColumn.setMinWidth(preferredWidth * 0.1D);
        nameColumn.setSortable(false);
        //nameColumn.setMaxWidth(Integer.MAX_VALUE * 0.5);

        sizeColumn = new CustomTreeTableColumn<>("Size");
        sizeColumn.setMinWidth(preferredWidth * 0.05D);
        sizeColumn.setSortable(false);
        //sizeColumn.setMaxWidth(Integer.MAX_VALUE * 0.3);

        ownerColumn = new CustomTreeTableColumn<>("Owner");
        ownerColumn.setMinWidth(preferredWidth * 0.1D);
        ownerColumn.setSortable(false);
        ownerColumn.setVisible(false);
        ownerColumn.visibleProperty().addListener(event ->
        {
            if(ownerColumn.isVisible())
            {
                goToPath(currentPath);
            }
        });

        lastModifiedTimeColumn = new CustomTreeTableColumn<>("Last modified time");
        lastModifiedTimeColumn.setMinWidth(preferredWidth * 0.1D);
        lastModifiedTimeColumn.setSortable(false);
        lastModifiedTimeColumn.setVisible(false);
        lastModifiedTimeColumn.visibleProperty().addListener(event ->
        {
            if(lastModifiedTimeColumn.isVisible())
            {
                goToPath(currentPath);
            }
        });

        creationTimeColumn = new CustomTreeTableColumn<>("Creation time");
        creationTimeColumn.setMinWidth(preferredWidth * 0.1D);
        creationTimeColumn.setSortable(false);
        creationTimeColumn.setVisible(false);
        creationTimeColumn.visibleProperty().addListener(event ->
        {
            if(creationTimeColumn.isVisible())
            {
                goToPath(currentPath);
            }
        });

        typeColumn = new CustomTreeTableColumn<>("Type");
        typeColumn.setMinWidth(preferredWidth * 0.1D);
        typeColumn.setSortable(false);
        //typeColumn.setMaxWidth(Integer.MAX_VALUE * 0.2);
        typeColumn.visibleProperty().addListener(event ->
        {
            if(typeColumn.isVisible())
            {
                goToPath(currentPath);
            }
        });


        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
        sizeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Long> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().getSize()));
        ownerColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getOwner()));
        lastModifiedTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getLastModifiedTime(true)));
        creationTimeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getCreationTime(true)));
        typeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getType()));

        rootItem = new TreeItem<>(new FileData("root", 4096L));

        content_TreeTableView = new TreeTableView<>();
        content_TreeTableView.getColumns().addAll(nameColumn, typeColumn, sizeColumn, ownerColumn,
                creationTimeColumn, lastModifiedTimeColumn);
        content_TreeTableView.setRoot(rootItem);
        content_TreeTableView.setShowRoot(false);
        content_TreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Сомнительный подход
        content_TreeTableView.setOnKeyPressed(event ->
        {
            contentKeyPressed_Action(event);
        });
        content_TreeTableView.getSortOrder().addAll(nameColumn, typeColumn);
        sortType = TreeTableColumn.SortType.ASCENDING;
        lastActiveSortColumn = nameColumn;
        content_TreeTableView.setTableMenuButtonVisible(true);

        EventHandler<MouseEvent> mouseClickEvent = new EventHandler<>()
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
                //temporaryCell.setTextFill(Color.RED);
                temporaryCell.setOnMouseClicked(mouseClickEvent);
                return temporaryCell;
            }
        });
        //nameColumn.setAutoFitColumnWidthToData(true);

        currentPath_TextField = new TextField();
        currentPath_TextField.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                System.out.println("Enter нажата.");

                Path newPath = Paths.get(currentPath_TextField.getText());
                System.out.println("Новый путь: " + newPath);
                goToPath(newPath);
            }
        });

//        content_TreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
//        nameColumn.setMaxWidth(1f * Integer.MAX_VALUE * 70); // 50% width
//        sizeColumn.setMaxWidth(1f * Integer.MAX_VALUE * 30); // 30% width
        //ageCol.setMaxWidth( 1f * Integer.MAX_VALUE * 20 ); // 20% width

        content_VBox = new VBox(rem * 0.45D);
        content_VBox.setPadding(new Insets(rem * 0.15D, rem * 0.7D, rem * 0.7D, rem * 0.7D));
        content_VBox.getChildren().addAll(currentPath_TextField, content_TreeTableView);
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

            if (temporaryColumn.isAutoFitSizeColumn())
            {
                autoSizeColumn_MenuItem.setSelected(true);
            }
            else
            {
                autoSizeColumn_MenuItem.setSelected(false);
            }
        });

        autoSizeColumn_MenuItem = new CheckMenuItem("Auto size");
        autoSizeColumn_MenuItem.setOnAction(this::contextMenuForColumns_Action);
        contextMenuForColums.getItems().addAll(autoSizeColumn_MenuItem);

        nameColumn.setContextMenu(contextMenuForColums);
        sizeColumn.setContextMenu(contextMenuForColums);
        ownerColumn.setContextMenu(contextMenuForColums);
        creationTimeColumn.setContextMenu(contextMenuForColums);
        lastModifiedTimeColumn.setContextMenu(contextMenuForColums);
        typeColumn.setContextMenu(contextMenuForColums);
    }

    private void contextMenuForColumns_Action(ActionEvent event)
    {
        TableColumnHeader tableColumnHeader = (TableColumnHeader) contextMenuForColums.getOwnerNode();
        CustomTreeTableColumn<FileData, String> temporaryColumn = (CustomTreeTableColumn<FileData, String>) tableColumnHeader.getTableColumn();
        System.out.println(temporaryColumn.getText());
        CheckMenuItem source = (CheckMenuItem) event.getSource();

        //Устанавливает равенство колонок
        //content_TreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
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
    public boolean goToPath(final Path destinationPath)
    {
        if (destinationPath == null)
        {
            System.out.println("Пустой путь.");
            return false;
        }
        if (!Files.exists(destinationPath))
        {
            System.out.println("Данного пути не существует.");
            if (currentPath == null)
            {
                currentPath_TextField.setText(FIlE_SYSTEMS_PATH);
            }
            else
            {
                currentPath_TextField.setText(currentPath.toAbsolutePath().toString());
            }
            return false;
        }

        System.out.println("destination: " + destinationPath);

        currentPath_TextField.setText(destinationPath.toAbsolutePath().toString());
        currentPath = destinationPath;

        try
        {
            updateFilesContent(currentPath);
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
        return true;
    }

    /**
     * Обновляет всю таблицу файлов исходя из текущего пути создавая новые объекты.
     * Также устанавливает выделение на первый элемент в таблице, если он есть.
     */
    private boolean updateFilesContent(final Path destinationPath) throws IOException
    {
        long startTime = System.currentTimeMillis();
        System.out.println("updatePath: " + destinationPath);
        rootItem.getChildren().clear();

        try (Stream<Path> filesStream = Files.list(destinationPath))
        {
            Iterator<Path> iterator = filesStream.iterator();
            FileData temporaryFileData = null;
            while (iterator.hasNext())
            {
                Path temporaryPath = iterator.next();

                try
                {
                    temporaryFileData = new FileData(temporaryPath.getFileName().toString(), Files.size(temporaryPath));
                    if (ownerColumn.isVisible())
                    {
                        temporaryFileData.setOwner(Files.getOwner(temporaryPath).getName());
                    }
                    if (lastModifiedTimeColumn.isVisible())
                    {
                        temporaryFileData.setLastModifiedTime(Files.getLastModifiedTime(temporaryPath));
                    }
                    if (creationTimeColumn.isVisible())
                    {
                        temporaryFileData.setCreationTime(Files.readAttributes(temporaryPath, BasicFileAttributes.class).creationTime());
                    }
                    temporaryFileData.setDirectory(Files.isDirectory(temporaryPath));
                    temporaryFileData.setFile(Files.isRegularFile(temporaryPath));
                    temporaryFileData.setSymbolicLink(Files.isSymbolicLink(temporaryPath));

                    rootItem.getChildren().add(new TreeItem<>(temporaryFileData));
                }
                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }

        }
        System.out.println("Прошло времени: " + (System.currentTimeMillis() - startTime));
        if (content_TreeTableView.getExpandedItemCount() != 0)
        {
            content_TreeTableView.getSelectionModel().clearSelection();
            content_TreeTableView.getSelectionModel().select(0);
        }

        requestSort(lastActiveSortColumn);

        if (content_TreeTableView.getExpandedItemCount() != 0)
        {
            content_TreeTableView.getSelectionModel().clearSelection();
            content_TreeTableView.getSelectionModel().select(0);
        }
        return true;
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

        return goToPath(path.getParent());
    }

    /**
     * Позволяет задать свой список элементов в таблице содержимого, при этом
     * все содержимое очищается.
     */
    private boolean addCustomToContentInTable(String pathName, String... names)
    {
        rootItem.getChildren().clear();
        for (String currentName : names)
        {
            rootItem.getChildren().add(new TreeItem<>(new FileData(currentName, 4L)));
        }
        currentPath_TextField.setText(pathName);
        return true;
    }

    /**
     * Обработчик событий нажатия клавиши для таблицы файлов.
     */
    private void contentKeyPressed_Action(KeyEvent event)
    {
        if (event.getCode() == KeyCode.ENTER)
        {
            if (currentPath != null)
            {
                System.out.println("Новый путь: " + currentPath);
                goToPath(currentPath.resolve(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
            }
            else
            {
                goToPath(Paths.get(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
            }
        }
//        if (event.getCode() == KeyCode.F2)
//        {
//            System.out.println("pizdes");
//            FileTime time = FileTime.fromMillis(35883295833L);
//            try
//            {
//                Path path = currentPath.resolve(content_TreeTableView.getSelectionModel().getSelectedItem()
//                        .getValue().getName());
//                FileTime timeFile = Files.readAttributes(currentPath.resolve(".dir_colors"), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).creationTime();
//                System.out.println(timeFile);
//                Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(time, time, time);
//                System.out.println("Атрибуты должны быть установлеы.");
//            }
//            catch (IOException ioException)
//            {
//                ioException.printStackTrace();
//            }
//
//        }
//        else if(event.getCode() == KeyCode.HOME)
//        {
//            System.out.println("Запрос на авторазмер....");
//            nameColumn.setAutoFitColumnWidthToData(false);
//        }
    }

    /**
     * Обработчик событий нажатия клавиш мыши для таблицы файлов.
     */
    private void cellMouseClicked_Action(MouseEvent event)
    {
        if (event.getClickCount() == 2)
        {
            if (currentPath != null)
            {
                goToPath(currentPath.resolve(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
            }
            else
            {
                System.out.println(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
                goToPath(Paths.get(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName()));
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
    private void requestSort(CustomTreeTableColumn<FileData, ?> targetColumn)
    {
        lastActiveSortColumn = targetColumn;
        targetColumn.setSortable(true);
        typeColumn.setSortable(true);
        nameColumn.setSortable(true);
        sizeColumn.setSortable(true);
        ownerColumn.setSortable(true);
        creationTimeColumn.setSortable(true);
        lastModifiedTimeColumn.setSortable(true);

        if (content_TreeTableView.getExpandedItemCount() < 2)
        {
            return;
        }

        targetColumn.setSortType(sortType);
        content_TreeTableView.getSortOrder().clear();

        if (directoriesFirst_MenuItem.isSelected())
        {
            typeColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
            content_TreeTableView.getSortOrder().add(typeColumn);
        }

        content_TreeTableView.getSortOrder().add(targetColumn);
        content_TreeTableView.sort();

        //================ Проверка на то, что каталоги действительно идут первыми
        if (directoriesFirst_MenuItem.isSelected())
        {
            int temporaryLength = 3;
            if (temporaryLength > content_TreeTableView.getExpandedItemCount())
            {
                temporaryLength = content_TreeTableView.getExpandedItemCount();
            }

            for (int k = 0; k < temporaryLength; k++)
            {
                if (content_TreeTableView.getTreeItem(k).getValue().isDirectory())
                {
                    System.out.println("Директория");
                }
                else
                {
                    System.out.println("Скорее всего нужно поменять тип сортировки.");
                    if (typeColumn.getSortType() == TreeTableColumn.SortType.ASCENDING)
                    {
                        typeColumn.setSortType(TreeTableColumn.SortType.DESCENDING);
                    }
                    else
                    {
                        typeColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
                    }
                }
            }
        }
        //=============================================================

        targetColumn.setSortable(false);
        typeColumn.setSortable(false);
        nameColumn.setSortable(false);
        sizeColumn.setSortable(false);
        ownerColumn.setSortable(false);
        creationTimeColumn.setSortable(false);
        lastModifiedTimeColumn.setSortable(false);
    }

    /**
     * @deprecated Не оптимизирован.
     */
    private void delete_MenuItem_Action()
    {
        System.out.println("Запрос на удаление");

        VBox filesToDeleting_VBox = new VBox(rem * 0.15D);
        Label[] files_Labels = null;

        ObservableList<TreeItem<FileData>> files_ObservableList = content_TreeTableView.getSelectionModel().getSelectedItems();
        files_Labels = new Label[files_ObservableList.size()];
        for (int k = 0; k < files_ObservableList.size(); k++)
        {
            files_Labels[k] = new Label(files_ObservableList.get(k).getValue().getName());
            filesToDeleting_VBox.getChildren().add(files_Labels[k]);
        }


        confirmOperationDialog.setHeaderText("Delete");
        confirmOperationDialog.setHeaderColor(Color.INDIANRED);
        confirmOperationDialog.setMessageText("Are you sure You want to delete this file ?");
        //confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
        confirmOperationDialog.setContent(filesToDeleting_VBox);
        confirmOperationDialog.setBackgroundToRootNode(new Background(
                new BackgroundFill(Color.DARKSALMON, CornerRadii.EMPTY, Insets.EMPTY)));
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 13.0D));
        confirmOperationDialog.showAndWait();
        System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

        if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
        {
            try
            {
                String temporaryName = null;
                Path temporaryPath = null;
                for (int k = 0; k < files_ObservableList.size(); k++)
                {
                    temporaryName = files_ObservableList.get(k).getValue().getName();
                    temporaryPath = currentPath.resolve(Paths.get(temporaryName));
                    System.out.println("На удаление: " + temporaryPath.toAbsolutePath().toString());
                    if (deleteFileRecursively(temporaryPath))
                    {
                        System.out.println("Успешно удалено.");
                        System.out.println("Элемент: " + files_ObservableList.get(k).getValue().getName());
                    }

                }

                //Обновляем плохим образом, ибо хорошим нету больше сил.
                goToPath(currentPath);
                //removeRowsFromTreeTableView(files_ObservableList);

            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }

    private boolean deleteFileRecursively(Path targetPath) throws IOException
    {
        if (!Files.exists(targetPath))
        {
            return false;
        }

        if (Files.isRegularFile(targetPath))
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
     * @deprecated Работает не корректно.
     */
    private void removeRowsFromTreeTableView(ObservableList<TreeItem<FileData>> targetRows)
    {
        Iterator<TreeItem<FileData>> iterator = targetRows.iterator();
        while (iterator.hasNext())
        {
            FileData temporaryItem = iterator.next().getValue();
            String name = temporaryItem.getName();
            System.out.println("Item: " + name);
            System.out.println("size: " + root.getChildren().size());
            root.getChildren().remove(temporaryItem);
        }
    }

    /**
     * Пока, только для одиночного переименования
     */
    private void rename_MenuItem_Action()
    {
        System.out.println("Запрос на переименования");

        VBox fileName_VBox = new VBox(rem * 0.15D);
        fileName_VBox.setAlignment(Pos.CENTER);
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
        fileName_TextField.setPromptText("Enter new name here");
        fileName_TextField.setOnKeyPressed(event ->
        {
            if (fileAlreadyExists_Label != null)
            {
                fileAlreadyExists_Label.setVisible(false);
            }
        });


        FileData temporaryFileData = content_TreeTableView.getSelectionModel().getSelectedItem().getValue();
        Path temporaryPath = currentPath.resolve(temporaryFileData.getName());

        fileName_TextField.setText(temporaryFileData.getName());


        fileName_VBox.getChildren().addAll(fileName_TextField, fileAlreadyExists_Label);

        confirmOperationDialog.setTitle("Rename");
        confirmOperationDialog.setHeaderText("Rename");
        confirmOperationDialog.setHeaderColor(Color.INDIGO);
        confirmOperationDialog.setMessageText("Enter a new name below.");
        confirmOperationDialog.setMessageTextColor(Color.BLACK);
        confirmOperationDialog.setMessageTextFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 13.0D));
        //confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
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
        confirmOperationDialog.showAndWait();
        System.out.println("Выбрана кнопка: " + confirmOperationDialog.getActivatedOperationButton().name());

        if (confirmOperationDialog.getActivatedOperationButton() == ConfirmDialogButtonType.OK)
        {
            Path targetPath = null;
            while (true)
            {
                try
                {
                    targetPath = currentPath.resolve(fileName_TextField.getText());
                    Path resultPath = Files.move(temporaryPath, targetPath);

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
                    confirmOperationDialog.showAndWait();
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

            FileData temporaryData = content_TreeTableView.getSelectionModel().getSelectedItem().getValue();
            temporaryData = temporaryData.clone();
            temporaryData.setName(targetPath.getFileName().toString());
            content_TreeTableView.getSelectionModel().getSelectedItem().setValue(temporaryData);
        }
    }

    private void copyFileName_MenuItem_Action(ActionEvent event)
    {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content_TreeTableView.getSelectionModel().getSelectedItem().getValue().getName());
        if (Clipboard.getSystemClipboard().setContent(clipboardContent))
        {
            Tooltip tooltip = new Tooltip("Name of file has been copied.");
            tooltip.setWrapText(true);
            tooltip.setHideOnEscape(true);
            tooltip.setAutoHide(true);
            tooltip.setFont(Font.font(tooltip.getFont().getName(), FontWeight.BOLD, 12.0D));
            tooltip.setOnShown(eventToolTip ->
            {
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2.0D), eventHide ->
                {
                    tooltip.hide();
                }));
                timeline.play();
            });
            Text temporaryText = new Text(tooltip.getText());
            tooltip.show(stage, (stage.getX() + stage.getWidth() / 2.0D) -
                    temporaryText.getBoundsInParent().getWidth() / 2.0D, stage.getY() + stage.getHeight() / 2);
        }
    }

    private void copyAbsoluteNamePath_MenuItem_Action(ActionEvent event)
    {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(currentPath.resolve(content_TreeTableView.getSelectionModel().
                getSelectedItem().getValue().getName()).toAbsolutePath().toString());
        if (Clipboard.getSystemClipboard().setContent(clipboardContent))
        {
            Tooltip tooltip = new Tooltip("Absolute path of selected file has been copied.");
            tooltip.setWrapText(true);
            tooltip.setHideOnEscape(true);
            tooltip.setAutoHide(true);
            tooltip.setFont(Font.font(tooltip.getFont().getName(), FontWeight.BOLD, 12.0D));
            tooltip.setOnShown(eventToolTip ->
            {
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3.0D), eventHide ->
                {
                    tooltip.hide();
                }));
                timeline.play();
            });
            Text temporaryText = new Text(tooltip.getText());
            tooltip.show(stage, (stage.getX() + stage.getWidth() / 2.0D) -
                    temporaryText.getBoundsInParent().getWidth() / 2.0D, stage.getY() + stage.getHeight() / 2);
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
        fileName_TextField.setOnKeyPressed(textFieldEvent ->
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
        //confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
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
                    Path targetPath = currentPath.resolve(fileName_TextField.getText());
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
//                                System.out.println("Дата редактирования не может быть ранешь даты создания!");
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
        fileName_TextField.setOnKeyPressed(textFieldEvent ->
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
        //confirmOperationDialog.setOperationButtons(ConfirmDialogButtonType.CANCEL, ConfirmDialogButtonType.OK);
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
                    Path targetPath = currentPath.resolve(fileName_TextField.getText());
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
     * @deprecated Сомнительная реализация и польза.
     */
    private EditableBasicFileAttributes createCustomFileAttributes(boolean hasFile, boolean hasDirectory,
                                                                   boolean hasOther, boolean hasSymbolicLink,
                                                                   DatePicker creationData_datePicker)
    {
        EditableBasicFileAttributes editableBasicFileAttributes = new EditableBasicFileAttributes();
        editableBasicFileAttributes.setHasFile(hasFile);
        editableBasicFileAttributes.setHasDirectory(hasDirectory);
        editableBasicFileAttributes.setHasOther(hasOther);
        editableBasicFileAttributes.setHasSymbolicLink(hasSymbolicLink);

        LocalDate localDate = creationData_datePicker.getValue();
        LocalDateTime creationDateTime = LocalDateTime.of(localDate, LocalTime.now());

        editableBasicFileAttributes.setCreationTime(FileTime.from(creationDateTime.toInstant(ZoneOffset.UTC)));
        System.out.println("CrationTime: " + editableBasicFileAttributes.creationTime());
        return editableBasicFileAttributes;
    }


    /**
     * Добавляет новую строку в таблицу файлов. При этом в объект модели
     * данных записываются только те данные, для которых отображены соответсвующие
     * колонки. Сортирование при этом не происходит.
     */
    private void addRowToTreeTable(Path targetPath)
    {

        FileData newFileDate = new FileData(targetPath.getFileName().toString(),
                -1);

        try
        {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(targetPath, BasicFileAttributes.class);
            newFileDate.setSize(basicFileAttributes.size());
            newFileDate.setFile(basicFileAttributes.isRegularFile());
            newFileDate.setSymbolicLink(basicFileAttributes.isSymbolicLink());
            newFileDate.setDirectory(basicFileAttributes.isDirectory());

            if (creationTimeColumn.isVisible())
            {
                newFileDate.setCreationTime(basicFileAttributes.creationTime());
            }
            if (lastModifiedTimeColumn.isVisible())
            {
                newFileDate.setLastModifiedTime(basicFileAttributes.lastModifiedTime());
            }
            if (ownerColumn.isVisible())
            {
                newFileDate.setOwner(Files.getOwner(targetPath, LinkOption.NOFOLLOW_LINKS).getName());
            }
            rootItem.getChildren().add(new TreeItem<>(newFileDate));
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }
}

