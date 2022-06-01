package org.yaldysse.fm;

import javafx.scene.control.Button;
import org.yaldysse.fm.dialogs.ConfirmDialogButtonType;

public class ConfirmOperationButton extends Button
{
    private ConfirmDialogButtonType buttonType;

    public ConfirmOperationButton(ConfirmDialogButtonType newButtonType, String text)
    {
        super(text);
        buttonType = newButtonType;
    }

    public ConfirmDialogButtonType getButtonType()
    {
        return buttonType;
    }
}
