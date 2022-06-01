package org.yaldysse.fm;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.yaldysse.tools.StorageCapacity;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**Описывает всплывающее окно, в котором отображена информация об хранилище данных
 * (раздел, диск и т.д.).*/
public class FileStoreInfoPopup extends Popup
{
    private Path path;

    final public static double rem = new Text("").getBoundsInParent().getHeight();
    private VBox root;
    private GridPane info_GridPane;

    private Label fileStoreName_Label;
    private Label totalSpace_Label;
    private Label freeSpace_Label;
    private Label usedSpace_Label;
    //private Label unallocatedSpace_Label;
    private Label fileSystemName_Label;
    private Label fileStoreLabel_Label;
    private Label fileStorePath_Label;

    private Label totalSpaceValue_Label;
    private Label freeSpaceValue_Label;
    private Label usedSpaceValue_Label;
    //private Label unallocatedSpaceValue_Label;
    private Label fileSystemNameValue_Label;
    private Label fileStoreLabelValue_Label;
    private Label fileStorePathValue_Label;

    public FileStoreInfoPopup(final Path targetPath)
    {
        if (targetPath == null)
        {
            return;
        }

        path = targetPath;
        initializeComponents();
    }

    private void initializeComponents()
    {
        root = new VBox();
        root.setPadding(new Insets(rem * 0.8D));
        root.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.MEDIUM)));
        root.setBackground(new Background(new BackgroundFill(Color.MEDIUMAQUAMARINE,
                CornerRadii.EMPTY, Insets.EMPTY)));

        info_GridPane = new GridPane();
        info_GridPane.setHgap(rem * 0.45D);
        info_GridPane.setVgap(rem * 0.4D);
        //info_GridPane.setPadding(new Insets(rem*1.0D));

        //fileStoreName_Label = new Label("Name");

        Font textFont = Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 13.0D);
        totalSpace_Label = new Label("Total space:");
        //totalSpace_Label.setTextFill(Color.GREEN);
        totalSpace_Label.setFont(textFont);

        freeSpace_Label = new Label("Free space:");
        freeSpace_Label.setFont(textFont);

        usedSpace_Label = new Label("Used space:");
        usedSpace_Label.setFont(textFont);

//        unallocatedSpace_Label = new Label("Unallocated space:");
//        unallocatedSpace_Label.setFont(textFont);

        fileSystemName_Label = new Label("File System:");
        fileSystemName_Label.setFont(textFont);

        fileStoreLabel_Label = new Label("Label:");
        fileStoreLabel_Label.setFont(textFont);

        fileStorePath_Label = new Label("Path");
        fileStorePath_Label.setFont(textFont);


        totalSpaceValue_Label = new Label();
        totalSpaceValue_Label.setFont(textFont);

        freeSpaceValue_Label = new Label();
        freeSpaceValue_Label.setFont(textFont);

//        unallocatedSpaceValue_Label = new Label();
//        unallocatedSpaceValue_Label.setFont(textFont);

        usedSpaceValue_Label = new Label();
        usedSpaceValue_Label.setFont(textFont);

        fileSystemNameValue_Label = new Label();
        fileSystemNameValue_Label.setFont(textFont);

        fileStoreLabelValue_Label = new Label();
        fileStoreLabelValue_Label.setFont(textFont);

        fileStorePathValue_Label = new Label();
        fileStorePathValue_Label.setFont(textFont);


        //info_GridPane.add(fileStoreName_Label, 0, 0, 2, 1);
        info_GridPane.addRow(info_GridPane.getRowCount(), freeSpace_Label,
                freeSpaceValue_Label);
        info_GridPane.addRow(info_GridPane.getRowCount(), usedSpace_Label,
                usedSpaceValue_Label);
//        info_GridPane.addRow(info_GridPane.getRowCount(), unallocatedSpace_Label,
//                unallocatedSpaceValue_Label);
        info_GridPane.addRow(info_GridPane.getRowCount(), totalSpace_Label,
                totalSpaceValue_Label);
        info_GridPane.add(new Separator(Orientation.HORIZONTAL), 0, info_GridPane.getRowCount(),
                2, 1);
        info_GridPane.addRow(info_GridPane.getRowCount(), fileStoreLabel_Label,
                fileStoreLabelValue_Label);
        info_GridPane.addRow(info_GridPane.getRowCount(), fileSystemName_Label,
                fileSystemNameValue_Label);
        info_GridPane.addRow(info_GridPane.getRowCount(), fileStorePath_Label,
                fileStorePathValue_Label);


        //GridPane.setHalignment(fileStoreName_Label, HPos.CENTER);

        root.getChildren().add(info_GridPane);

        readData();

        getScene().setRoot(root);
        getScene().setFill(Color.WHITESMOKE);
        getScene().getWindow().setWidth(100.0D);
        setWidth(100.0D);
        setAutoHide(true);
    }

    private void readData()
    {
        try
        {
            FileStore temporaryFileStore = Files.getFileStore(path);

            long freeSpace = temporaryFileStore.getUsableSpace();
            long totalSpace = temporaryFileStore.getTotalSpace();

            totalSpaceValue_Label.setText(StorageCapacity.ofBytes(totalSpace).toString());
            //unallocatedSpaceValue_Label.setText(StorageCapacity.ofBytes(temporaryFileStore.getUnallocatedSpace()).toString());
            freeSpaceValue_Label.setText(StorageCapacity.ofBytes(freeSpace).toString());
            usedSpaceValue_Label.setText(StorageCapacity.ofBytes(totalSpace - freeSpace).toString());
            fileSystemNameValue_Label.setText(temporaryFileStore.type());

            String temporaryLabel = temporaryFileStore.toString();
            if (temporaryLabel.indexOf("(") == 0)
            {
                fileStoreLabelValue_Label.setText("---");
            }
            else
            {
                fileStoreLabelValue_Label.setText(Paths.get(temporaryLabel.substring(0,
                        temporaryLabel.indexOf("(") - 1)).getFileName().toString());
            }

            //-------------------- fileStorePath
            String osName = System.getProperty("os.name");
            if (osName.contains("Windows"))
            {
                String fullName = temporaryFileStore.toString();
                Path temporaryPath = Paths.get(fullName.substring(fullName.indexOf("(") + 1,
                        fullName.indexOf(")")));
                //System.out.println("Path: " + temporaryPath);
                //System.out.println("AbsolutePath: " + temporaryPath.toAbsolutePath().toString());
                fileStorePathValue_Label.setText(temporaryPath.toAbsolutePath().toString());
            }
            else
            {
                if (temporaryLabel.indexOf("(") == 0)
                {
                    fileStorePathValue_Label.setText("---");
                }
                else
                {
                    fileStorePathValue_Label.setText(Paths.get(temporaryLabel.substring(0,
                            temporaryLabel.indexOf("(") - 1)).toAbsolutePath().toString());
                }
            }
            //==================================
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }


    public void updateData(final Path newTargetPath)
    {
        if (newTargetPath == null)
        {
            return;
        }

        path = newTargetPath;
        readData();

        getScene().getWindow().setWidth(100.0D);
        setWidth(100.0D);
    }
}
