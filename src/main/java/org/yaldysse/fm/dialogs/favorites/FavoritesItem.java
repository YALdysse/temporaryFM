package org.yaldysse.fm.dialogs.favorites;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.yaldysse.fm.FM_GUI;
import org.yaldysse.fm.FileNameTip;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Properties;

public class FavoritesItem extends VBox
{
    private Label fileName_Label;
    private Label fileType_Label;
    private Label filePath_Label;
    private Label goToFile_Button;
    private Label removeFromFavorites_Button;
    private Label attributes_Button;
    private Path filePath;
    private Properties language;


    public FavoritesItem(final Path targetPath, final Properties languageProperties)
    {
        filePath = targetPath;
        language = languageProperties;

        initializeComponents();
    }

    private void initializeComponents()
    {

        fileName_Label = new Label();
        fileName_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 14.0D));

        fileType_Label = new Label();
        fileType_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 11.0D));
        fileType_Label.setTextFill(Color.GRAY);

        filePath_Label = new Label();
        filePath_Label.setTextFill(Color.DIMGRAY);
        filePath_Label.setFont(Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 11.0D));

        Font buttonFont = Font.font(Font.getDefault().getName(),
                FontWeight.BOLD, 26.0D);

        goToFile_Button = new Label("➤");
        goToFile_Button.setTooltip(new FileNameTip(
                language.getProperty("goToFile_str",
                        "Go to file")));
        goToFile_Button.setFont(buttonFont);
        goToFile_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        goToFile_Button.setOnMouseEntered(event ->
        {
            goToFile_Button.setTextFill(Color.DARKORANGE);
        });
        goToFile_Button.setOnMouseExited(event ->
        {
            goToFile_Button.setTextFill(Color.BLACK);
        });

        removeFromFavorites_Button = new Label("✘");
        removeFromFavorites_Button.setTooltip(new FileNameTip(
                language.getProperty("removeFromFavorites_str",
                "Remove from favorites")));
        removeFromFavorites_Button.setFont(buttonFont);
        removeFromFavorites_Button.setTextFill(Color.BLACK);
        removeFromFavorites_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        removeFromFavorites_Button.setOnMouseEntered(event ->
        {
            removeFromFavorites_Button.setTextFill(Color.CRIMSON);
        });
        removeFromFavorites_Button.setOnMouseExited(event ->
        {
            removeFromFavorites_Button.setTextFill(Color.BLACK);
        });

        attributes_Button = new Label("Ã");
        attributes_Button.setTooltip(new FileNameTip(
                language.getProperty("attributes_menuItem",
                        "Attributes")));
        attributes_Button.setFont(buttonFont);
        attributes_Button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        attributes_Button.setOnMouseEntered(event ->
        {
            attributes_Button.setTextFill(Color.DARKORCHID);
        });
        attributes_Button.setOnMouseExited(event ->
        {
            attributes_Button.setTextFill(Color.BLACK);
        });


        HBox buttons_HBox = new HBox(FM_GUI.rem * 0.9D, removeFromFavorites_Button,
                attributes_Button, goToFile_Button);
        buttons_HBox.setPadding(new Insets(0.0D, FM_GUI.rem * 0.6D, 0.0D, 0.0D));
        buttons_HBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(buttons_HBox, Priority.ALWAYS);


        HBox fileTypeAndButtons_HBox = new HBox(FM_GUI.rem * 0.6D, fileType_Label,
                buttons_HBox);
        fileTypeAndButtons_HBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fileTypeAndButtons_HBox, Priority.ALWAYS);

        setFillWidth(true);
        setSpacing(FM_GUI.rem * 0.2D);
        getChildren().addAll(fileName_Label, filePath_Label, fileTypeAndButtons_HBox,
                new Separator(Orientation.HORIZONTAL));

        updateData();
    }


    private void updateData()
    {
        if (filePath.getFileName() != null)
        {
            fileName_Label.setText(filePath.getFileName().toString());
        }
        else
        {
            fileName_Label.setText(filePath.toString());
        }

        //--------------------- Type
        if (Files.isDirectory(filePath, LinkOption.NOFOLLOW_LINKS))
        {
            fileType_Label.setText(language.getProperty("directory_str",
                    "Directory"));
        }
        else if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS))
        {
            fileType_Label.setText(language.getProperty("regularFile_str",
                    "File"));
        }
        else
        {
            fileType_Label.setText(language.getProperty("symbolikLink_str",
                    "Symbolic Link"));
        }

        //---------------------
        filePath_Label.setText(filePath.toAbsolutePath().toString());
    }

    public Label getRemoveFavoriteButton()
    {
        return removeFromFavorites_Button;
    }

    public Path getFilePath()
    {
        return filePath;
    }

    public Label getGoToFileButton()
    {
        return goToFile_Button;
    }

    public Label getShowAttributesButton(){return attributes_Button;}

}
