package org.yaldysse.fm;

import javafx.scene.control.TreeTableCell;
import javafx.scene.paint.Color;

public class CustomTreeTableCell<Type1, Type2> extends TreeTableCell<Type1, Type2>
{
    private Color textColor;

    public CustomTreeTableCell()
    {
        super();
        textColor = Color.BLACK;
        setTextFill(textColor);
    }

    @Override
    protected void updateItem(Type2 type2, boolean b)
    {
        super.updateItem(type2, b);

        if (type2 == null)
        {
            type2 = (Type2) "";
        }
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
}
