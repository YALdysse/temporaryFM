package org.yaldysse.fm;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private TreeTableColumn<FileData, String> nameColumn;
    private TreeTableColumn<FileData, Long> sizeColumn;
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
    private String maxLengthName;
    private String maxLengthSize_str;
    private boolean fitNameColumnToData;
    private boolean fitSizeColumnToData;
    private boolean isFitNameColumnActive;
    private boolean isFitSizeColumnActive;

    private MenuBar menu_Bar;
    private Menu file_Menu;
    private Menu goTo_Menu;
    private Menu view_Menu;
    private Menu sortBy_Menu;
    private RadioMenuItem sortByName_MenuItem;
    private RadioMenuItem sortBySize_MenuItem;
    private ToggleGroup sortBy_ToggleGroup;
    private RadioMenuItem descendingSortType_MenuItem;
    private RadioMenuItem ascendingSortType_MenuItem;
    private ToggleGroup sortType_ToggleGroup;
    private MenuItem goToParent_MenuItem;
    private MenuItem goToRootDirectories_MenuItem;
    private MenuItem goToHomeDirectory_MenuItem;
    private MenuItem goToUserDirectory_MenuItem;
    private MenuItem exit_MenuItem;

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
        initializeMenu();
        initializeGeneral();
        initializeContextMenuForColumns();

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

        file_Menu = new Menu("File");
        file_Menu.getItems().addAll(exit_MenuItem);

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
        goToRootDirectories_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCodeCombination.CONTROL_DOWN));

        goToHomeDirectory_MenuItem = new MenuItem("Home directory");
        goToHomeDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.home");
            goToPath(Paths.get(userDirectory));
        });
        goToHomeDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCodeCombination.CONTROL_DOWN));

        goToUserDirectory_MenuItem = new MenuItem("User directory");
        goToUserDirectory_MenuItem.setOnAction(event ->
        {
            String userDirectory = System.getProperty("user.dir");
            goToPath(Paths.get(userDirectory));
        });
        goToUserDirectory_MenuItem.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCodeCombination.CONTROL_DOWN));


        goTo_Menu = new Menu("Go to...");
        goTo_Menu.getItems().addAll(goToRootDirectories_MenuItem, goToHomeDirectory_MenuItem,
                goToUserDirectory_MenuItem, goToParent_MenuItem);

        sortBy_ToggleGroup = new ToggleGroup();
        sortType_ToggleGroup = new ToggleGroup();

        sortByName_MenuItem = new RadioMenuItem("by Name");
        sortByName_MenuItem.setSelected(true);
        sortByName_MenuItem.setOnAction(event ->
        {
            requestSort();
        });
        sortByName_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        sortBySize_MenuItem = new RadioMenuItem("by Size");
        sortBySize_MenuItem.setOnAction(event ->
        {
            requestSort();
        });
        sortBySize_MenuItem.setToggleGroup(sortBy_ToggleGroup);

        ascendingSortType_MenuItem = new RadioMenuItem("Ascending");
        ascendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        ascendingSortType_MenuItem.setSelected(true);
        ascendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.ASCENDING;
            requestSort();
        });

        descendingSortType_MenuItem = new RadioMenuItem("Descending");
        descendingSortType_MenuItem.setToggleGroup(sortType_ToggleGroup);
        descendingSortType_MenuItem.setOnAction(event ->
        {
            sortType = TreeTableColumn.SortType.DESCENDING;
            requestSort();
        });

        sortType = TreeTableColumn.SortType.ASCENDING;

        sortBy_Menu = new Menu("Sort by");
        sortBy_Menu.getItems().addAll(sortByName_MenuItem, sortBySize_MenuItem,
                new SeparatorMenuItem(), ascendingSortType_MenuItem, descendingSortType_MenuItem,
                new SeparatorMenuItem());

        menu_Bar = new MenuBar(file_Menu, goTo_Menu, sortBy_Menu);

        menu_BorderPane = new BorderPane();
        menu_BorderPane.setTop(menu_Bar);
    }

    private void initializeGeneral()
    {
        nameColumn = new TreeTableColumn<>("Name");
        nameColumn.setMinWidth(preferredWidth * 0.1D);
        nameColumn.setSortable(false);
        sizeColumn = new TreeTableColumn<>("Size");
        sizeColumn.setMinWidth(preferredWidth * 0.1D);
        sizeColumn.setSortable(false);


        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getName()));
        sizeColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<FileData, Long> param) ->
                new ReadOnlyObjectWrapper<>(param.getValue().getValue().getSize()));

        rootItem = new TreeItem<>(new FileData("root", 4096L));

        content_TreeTableView = new TreeTableView<>();
        content_TreeTableView.getColumns().addAll(nameColumn, sizeColumn);
        content_TreeTableView.setRoot(rootItem);
        content_TreeTableView.setShowRoot(false);
        content_TreeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//        content_TreeTableView.getSelectionModel().selectedItemProperty().addListener((event, val, val2) ->
//        {
//            if (val2 != null)
//            {
//                System.out.println("pidar");
//                System.out.println(val2.getValue().getName());
//            }
//        });

        //Сомнительный подход
        content_TreeTableView.setOnKeyPressed(event ->
        {
            contentKeyPressed_Action(event);
        });
        content_TreeTableView.getSortOrder().add(nameColumn);

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

        fitNameColumnToData = false;

        content_VBox = new VBox(rem * 0.45D);
        content_VBox.setPadding(new Insets(rem * 0.15D, rem * 0.7D, rem * 0.7D, rem * 0.7D));
        content_VBox.getChildren().addAll(currentPath_TextField, content_TreeTableView);
    }


    private void initializeContextMenuForColumns()
    {
        contextMenuForColums = new ContextMenu();
        contextMenuForColums.setOnShowing(event ->
        {
            System.out.println("Отображается.");
            TableColumnHeader tableColumnHeader = (TableColumnHeader) contextMenuForColums.getOwnerNode();
            TreeTableColumn<FileData, String> temporaryColumn = (TreeTableColumn<FileData, String>) tableColumnHeader.getTableColumn();
            System.out.println(temporaryColumn.getText());
            ContextMenu sourceMenu = (ContextMenu) event.getSource();
            CheckMenuItem sourceMenuItem = (CheckMenuItem) sourceMenu.getItems().get(0);

            if(temporaryColumn.equals(nameColumn))
            {
                sourceMenuItem.setSelected(isFitNameColumnActive);
            }
            else if(temporaryColumn.equals(sizeColumn))
            {
                sourceMenuItem.setSelected(isFitSizeColumnActive);
            }
        });

        CheckMenuItem autoSizeColumn_MenuItem = new CheckMenuItem("Auto size");
        autoSizeColumn_MenuItem.setOnAction(event ->
        {
            TableColumnHeader tableColumnHeader = (TableColumnHeader) contextMenuForColums.getOwnerNode();
            TreeTableColumn<FileData, String> temporaryColumn = (TreeTableColumn<FileData, String>) tableColumnHeader.getTableColumn();
            System.out.println(temporaryColumn.getText());
            CheckMenuItem source = (CheckMenuItem) event.getSource();

            //Устанавливает равенство колонок
            //content_TreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
            String currentMaxLengthString = null;

            if (temporaryColumn.equals(nameColumn))
            {
                isFitNameColumnActive=autoSizeColumn_MenuItem.isSelected();
                fitNameColumnToData = source.isSelected();
                currentMaxLengthString = maxLengthName;
            }
            else if (temporaryColumn.equals(sizeColumn))
            {
                isFitSizeColumnActive=autoSizeColumn_MenuItem.isSelected();
                fitSizeColumnToData = source.isSelected();
                currentMaxLengthString = maxLengthSize_str;
            }

            if (source.isSelected())
            {
                fitColumnToData(temporaryColumn, currentMaxLengthString);
            }
        });
        contextMenuForColums.getItems().addAll(autoSizeColumn_MenuItem);

        nameColumn.setContextMenu(contextMenuForColums);
        sizeColumn.setContextMenu(contextMenuForColums);

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
        int maxNameLength = 1;
        int maxSizeLength = 1;

        try (Stream<Path> filesStream = Files.list(destinationPath))
        {
            Iterator<Path> iterator = filesStream.iterator();
            while (iterator.hasNext())
            {
                Path temporaryPath = iterator.next();

                int currentNameLength = temporaryPath.getFileName().toString().length();
                String currentSize_str = String.valueOf(Files.size(temporaryPath));
                int currentSizeLength = currentSize_str.length();

                if (currentNameLength > maxNameLength)
                {
                    maxNameLength = currentNameLength;
                    maxLengthName = temporaryPath.getFileName().toString();
                }
                if (currentSizeLength > maxSizeLength)
                {
                    maxSizeLength = currentSizeLength;
                    maxLengthSize_str = currentSize_str;
                }

                try
                {
                    rootItem.getChildren().add(new TreeItem<>(new FileData(temporaryPath.getFileName().toString(),
                            Files.size(temporaryPath))));
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

        requestSort();

        if (fitNameColumnToData)
        {
            fitColumnToData(nameColumn, maxLengthName);
        }
        if(fitSizeColumnToData)
        {
            fitColumnToData(sizeColumn,maxLengthSize_str);
        }

        return true;
    }

    private void fitColumnToData(TreeTableColumn<FileData, ?> sourceColumn,
                                 final String currentMaxLengthName)
    {
        Font cellFont = Font.font(16.0D);
        System.out.println("Длинное слово: " + currentMaxLengthName);
        Text temporaryText = new Text(currentMaxLengthName);
        System.out.println("Без шрифта: " + temporaryText.getBoundsInParent().getWidth());
        temporaryText.setFont(cellFont);
        System.out.println("Со шрифтом: " + temporaryText.getBoundsInParent().getWidth());
        double temporaryWidth = temporaryText.getBoundsInParent().getWidth();
        sourceColumn.setPrefWidth(temporaryWidth);
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
    private void requestSort()
    {
        nameColumn.setSortable(true);
        sizeColumn.setSortable(true);

        if (content_TreeTableView.getExpandedItemCount() < 2)
        {
            return;
        }

        if (sortByName_MenuItem.isSelected())
        {
            nameColumn.setSortType(sortType);
            content_TreeTableView.getSortOrder().clear();
            System.out.println("size: " + content_TreeTableView.getSortOrder().size());
            content_TreeTableView.getSortOrder().add(nameColumn);
        }
        else if (sortBySize_MenuItem.isSelected())
        {
            sizeColumn.setSortType(sortType);
            content_TreeTableView.getSortOrder().clear();
            content_TreeTableView.getSortOrder().add(sizeColumn);
        }

        content_TreeTableView.sort();

        nameColumn.setSortable(false);
        sizeColumn.setSortable(false);
    }

}
