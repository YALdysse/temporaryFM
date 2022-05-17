package org.yaldysse.fm;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.nio.file.Path;

public class FileStorageButton extends Button
{
    private Path fileStoragePath;

    private static DropShadow shadowEffect = new DropShadow(4.0D, 4.0D, 3.0D, Color.BLACK);
    private static Border border = new Border(new BorderStroke(Color.DARKSLATEBLUE, BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY, BorderStroke.MEDIUM));

    public FileStorageButton(String s, final Path targetPath)
    {
        super(s);

        if (targetPath != null)
        {
            fileStoragePath = targetPath;
        }

        setBackground(new Background(new BackgroundFill(Color.MEDIUMPURPLE,
                CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(border);
        setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD,
                13.0D));
        setTextFill(Color.HONEYDEW);
        setEffect(shadowEffect);
    }

    public Path getFileStoragePath()
    {
        return fileStoragePath;
    }
}
