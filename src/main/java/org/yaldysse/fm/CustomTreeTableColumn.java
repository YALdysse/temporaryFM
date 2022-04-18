package org.yaldysse.fm;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CustomTreeTableColumn<Type1, Type2> extends TreeTableColumn<Type1, Type2>
{
    private Type2 maxLengthCellData_Type2;
    private int maxLengthCellData;
    private boolean autoFitSizeColumn;
    private ChangeListener expandedItemListener;

    public CustomTreeTableColumn(String s)
    {
        super(s);
        autoFitSizeColumn = false;
    }

    public void fitColumnWidthToData(Font font)
    {
        int currentCellDataLength = 1;
        String value = null;
        int valueLength = 1;
        maxLengthCellData = 1;

        TreeTableView<?> treeTableView = getTreeTableView();


        for (int k = 0; k < treeTableView.getExpandedItemCount(); k++)
        {
            value = String.valueOf(getCellData(k));
            valueLength = value.length();
            if (valueLength > maxLengthCellData)
            {
                maxLengthCellData = valueLength;
                maxLengthCellData_Type2 = (Type2) value;
            }
        }
        System.out.println("Самый длинный элемент: " + maxLengthCellData_Type2);

        Text temporaryText = new Text((String) maxLengthCellData_Type2);
        temporaryText.setFont(font);
        setPrefWidth(temporaryText.getBoundsInParent().getWidth());

    }

    /**
     * Позволяет задать автоматический подгон ширины ячеек под содержание.
     *
     * @return false  если не удается получить доступ к TreeTableView, за которым
     * закреплена данная колонка.
     * @deprecated Жестко закодирован шрифт.
     */
    public boolean setAutoFitColumnWidthToData(boolean autoFit)
    {
        autoFitSizeColumn = autoFit;

        if (getTreeTableView() == null)
        {
            return false;
        }
        if (autoFit)
        {
            expandedItemListener = new ChangeListener()
            {
                @Override
                public void changed(ObservableValue observableValue, Object o, Object t1)
                {
                    fitColumnWidthToData(new Font(16.0D));

                }
            };
            getTreeTableView().expandedItemCountProperty().addListener(expandedItemListener);
            fitColumnWidthToData(new Font(16.0D));
        }
        else
        {
            getTreeTableView().expandedItemCountProperty().removeListener(expandedItemListener);
        }

        return true;
    }

    public boolean isAutoFitSizeColumn()
    {
        return autoFitSizeColumn;
    }
}
