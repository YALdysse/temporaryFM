package org.yaldysse.fm;

import javafx.geometry.Insets;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class CustomTreeTableCell<Type1, Type2> extends TreeTableCell<Type1, Type2>
{
    private Color textColor;
    private Image icon;

    public CustomTreeTableCell()
    {
        super();
        textColor = Color.BLACK;
        setTextFill(textColor);
    }

    public CustomTreeTableCell(Image newIcon)
    {
        super();
        textColor = Color.BLACK;
        setTextFill(textColor);
        icon = newIcon;
    }

    @Override
    protected void updateItem(Type2 type2, boolean b)
    {
        super.updateItem(type2, b);

        if(icon!=null)
        {
            ImageView temporaryImageView = new ImageView(icon);
            temporaryImageView.setPreserveRatio(true);
            temporaryImageView.setFitWidth(22.0D);
            temporaryImageView.setSmooth(true);
            setGraphic(temporaryImageView);
        }
        if (type2 == null)
        {
            type2 = (Type2) "";
        }
//        setGraphicTextGap(50.0D);
        setText(type2.toString());

    }


    public void setTextColor(final Color newTextColor)
    {
        if (newTextColor == null)
        {
            return;
        }
        textColor = newTextColor;
    }

    /**Позволяет задать изображения в качестве иконки. Чтобы убрать иконку  нужно
     * передать null значеніе.*/
    public void setIcon(final Image newIcon)
    {
        icon = newIcon;
    }
}
