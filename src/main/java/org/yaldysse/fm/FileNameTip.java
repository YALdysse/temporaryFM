package org.yaldysse.fm;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Shadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;

public class FileNameTip extends Tooltip
{
    private Label text;
    private VBox root;
    public static final double rem = new Text("").getBoundsInParent().getHeight();

    public FileNameTip(String s)
    {
        super();

        text = new Label(s);
        text.setTextFill(Color.DARKGOLDENROD);
        text.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD,
                12.0D));
        //text.setEffect(new DropShadow(0.05D, Color.BLACK));
        text.setWrapText(true);
        text.setMaxWidth(Screen.getPrimary().getBounds().getWidth()*0.25D);

        root = new VBox(text);
        root.setBorder(new Border(new BorderStroke(Color.BURLYWOOD,
                BorderStrokeStyle.SOLID, new CornerRadii(rem * 0.3D),
                BorderStroke.MEDIUM, Insets.EMPTY)));
        root.setBackground(new Background(new BackgroundFill(Color.BEIGE,
                root.getBorder().getStrokes().get(0).getRadii(), Insets.EMPTY)));
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(rem * 0.4D));

        Scene scene = this.getScene();
        scene.setFill(Color.TRANSPARENT);
        scene.setRoot(root);

    }

    public void setLabelText(final String newText)
    {
        text.setText(newText);
    }

}
