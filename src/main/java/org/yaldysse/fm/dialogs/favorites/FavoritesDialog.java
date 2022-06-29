package org.yaldysse.fm.dialogs.favorites;

import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.yaldysse.fm.FM_GUI;
import org.yaldysse.fm.dialogs.ConfirmOperationDialog;
import org.yaldysse.fm.dialogs.FileAttributesEditor;
import org.yaldysse.fm.dialogs.copy.CopyFiles;
import org.yaldysse.fm.dialogs.delete.DeleteOperationResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;


/***/
public class FavoritesDialog
{
    public static final double rem = new Text("").getBoundsInParent().getHeight();

    private Scene scene;
    private VBox root;
    private VBox content_VBox;
    private Stage stage;
    private ScrollPane scrollPane;
    private double preferredWidth;
    private double preferedHeight;

    private Properties language;
    private ArrayList<Path> favorites_List;
    private FM_GUI Fm_gui;

    public FavoritesDialog(final Properties languageProperties, FM_GUI aFm_gui)
    {
        language = languageProperties;
        Fm_gui=aFm_gui;

        initializeComponents();

    }

    public FavoritesDialog(final ArrayList<Path> newFavorites_List,
                           final Properties languageProperties, FM_GUI aFm_gui)
    {
        language = languageProperties;
        Fm_gui=aFm_gui;

        initializeComponents();

        for (int k = 0; k < newFavorites_List.size(); k++)
        {
            addFavoriteFile(newFavorites_List.get(k));
        }
    }


    private void initializeComponents()
    {
        stage = new Stage();
        stage.setTitle("Favorites");

        root = new VBox(rem * 0.2D);
        root.setPadding(new Insets(rem * 0.7D));

        scene = new Scene(root);

        favorites_List = new ArrayList<>();

        content_VBox = new VBox(rem * 0.8D);
        scrollPane = new ScrollPane(content_VBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(rem * 0.5D));


        root.getChildren().add(scrollPane);

        stage.setScene(scene);
        preferredWidth = rem * 28.0D;
        preferedHeight = rem * 18.0D;
        stage.setHeight(preferedHeight);
        stage.setWidth(preferredWidth);
    }

    public void addFavoriteFile(final Path pathToFavoriteFile)
    {
        if (pathToFavoriteFile == null)
        {
            return;
        }
        else
        {
            favorites_List.add(pathToFavoriteFile);
            FavoritesItem temporaryItem = new FavoritesItem(pathToFavoriteFile,
                    language);
            Label removeFavorite_Button = temporaryItem.getRemoveFavoriteButton();
            removeFavorite_Button.setOnMouseClicked(event ->
            {
                content_VBox.getChildren().remove(temporaryItem);
                favorites_List.remove(temporaryItem.getFilePath());
            });

            Label goToFile_Button = temporaryItem.getGoToFileButton();
            goToFile_Button.setOnMouseClicked(event ->
            {
                Fm_gui.goToFavoriteFile(pathToFavoriteFile);
            });

            temporaryItem.getShowAttributesButton().setOnMouseClicked(event ->
            {
                new FileAttributesEditor(pathToFavoriteFile,language).show();
            });

            content_VBox.getChildren().addAll(temporaryItem);
        }
    }

    public void show()
    {
        stage.show();
    }

    public void hide()
    {
        stage.hide();
    }

    public ArrayList<Path> getFavoritesList()
    {
        return favorites_List;
    }
}
