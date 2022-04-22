package org.yaldysse.fm;

import com.sun.javafx.collections.ImmutableObservableList;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConfirmOperationDialog extends Stage
{
    private Label header_Label;
    private Label text_Label;
    public final double rem;
    private double preferredWidth;
    private double preferredHeight;
    private ConfirmDialogButtonType activatedOperationButton;

    private Scene scene;
    private BorderPane root;
    private VBox content;
    private HBox header_HBox;
    private HBox message_HBox;
    private HBox operationButtons_HBox;
    private ScrollPane info_ScrollPane;

    public ConfirmOperationDialog(StageStyle stageStyle)
    {
        super(stageStyle);
        rem = new Text("").getBoundsInParent().getHeight();
        initializeComponents();
    }

    public ConfirmOperationDialog(StageStyle stageStyle, String newTitle)
    {
        super(stageStyle);
        rem = new Text("").getBoundsInParent().getHeight();
        initializeComponents();
        setTitle(newTitle);
    }

    private void initializeComponents()
    {
        header_Label = new Label("Header");
        header_Label.setFont(Font.font(header_Label.getFont().getName(),
                FontWeight.EXTRA_BOLD, 18.0D));
        //header_Label.setPadding(new Insets(0.0D, rem * 0.4D, 0.0D, 0.4D));
        header_Label.setTextFill(Color.GHOSTWHITE);
        text_Label = new Label("Message");
        text_Label.setWrapText(true);

        header_HBox = new HBox(header_Label);
        header_HBox.setAlignment(Pos.TOP_CENTER);
        header_HBox.setBackground(new Background(new BackgroundFill(Color.CORNFLOWERBLUE,
                CornerRadii.EMPTY, Insets.EMPTY)));
        //header_HBox.setPadding(new Insets(0.0D, rem * 0.4D, 0.0D, 0.4D));

        message_HBox = new HBox(text_Label);
        message_HBox.setAlignment(Pos.TOP_LEFT);

        operationButtons_HBox = new HBox(rem * 0.8D);
        operationButtons_HBox.setAlignment(Pos.BOTTOM_RIGHT);

        preferredWidth = rem * 21.3;
        preferredHeight = rem * 12.0D;

        info_ScrollPane = new ScrollPane();
        info_ScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        info_ScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        info_ScrollPane.setPadding(new Insets(0.55D, rem * 0.25D, 0.55D, rem * 0.5D));
        info_ScrollPane.setFitToWidth(true);
        //info_ScrollPane.setBackground(new Background(new BackgroundFill(Color.LIGHTCYAN, CornerRadii.EMPTY, Insets.EMPTY)));

        content = new VBox(rem * 0.35D);
        content.getChildren().addAll(header_HBox, message_HBox);


        root = new BorderPane();
        root.setPadding(new Insets(rem * 0.6D));
        root.setTop(content);
        root.setCenter(info_ScrollPane);
        root.setBottom(operationButtons_HBox);
        BorderPane.setMargin(info_ScrollPane, new Insets(rem * 0.5D, 0.0D, rem * 0.5D, 0.0D));
        root.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderStroke.THIN, root.getInsets())));

        scene = new Scene(root);
        scene.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE)
            {
                activatedOperationButton = ConfirmDialogButtonType.CANCEL;
                hide();
            }
        });

        setScene(scene);
        setMinHeight(preferredHeight);
        setMinWidth(preferredWidth);
        setMaxHeight(preferredHeight + 20);
        setMaxWidth(preferredWidth + 20);

        activatedOperationButton = ConfirmDialogButtonType.CANCEL;
    }


    public void setHeaderText(final String newHeaderText)
    {
        header_Label.setText(newHeaderText);
    }

    public void setMessageText(String newMessageText)
    {
        text_Label.setText(newMessageText);
    }

    /**
     * Позволяет настроить цвет заливки области с заголовком.
     */
    public void setHeaderColor(Color newHeaderColor)
    {
        if (newHeaderColor == null)
        {
            return;
        }
        BackgroundFill oldBackgroundFill = header_HBox.getBackground().getFills().get(0);
        header_HBox.setBackground(new Background(new BackgroundFill(
                newHeaderColor, oldBackgroundFill.getRadii(), oldBackgroundFill.getInsets())));

    }


    /**Устанавливает цвет текста зоголовка.*/
    public void setHeaderTextColor(Paint color)
    {
        if (color == null)
        {
            return;
        }
        header_Label.setTextFill(color);
    }

    /**Позволяет установить шрифт для заголовка.*/
    public void setHeaderTextFont(final Font newHeaderFont)
    {
        if(newHeaderFont==null)
        {
            return;
        }

        header_Label.setFont(newHeaderFont);
    }

    /**Позволяет установить шрифт для сообщения.*/
    public void setMessageTextFont(final Font newMessageFont)
    {
        if(newMessageFont == null)
        {
            return;
        }
        text_Label.setFont(newMessageFont);
    }

    /**
     * Устанавливает цвет текста сообщения.
     */
    public void setMessageTextColor(Paint color)
    {
        if (color == null)
        {
            return;
        }
        text_Label.setTextFill(color);
    }

    /**
     * Устанавливает содержимое диалогового окна. Оно будет добавлено в область
     * прокрутки, что расположена в середине окна.
     */
    public void setContent(Node content)
    {
        if (content == null)
        {
            return;
        }

        info_ScrollPane.setContent(content);
    }

    /**
     * Позволяет задать кнопки выбора действия, заменяя уже существующие.
     */
    public void setOperationButtons(ConfirmDialogButtonType... buttons)
    {
        operationButtons_HBox.getChildren().clear();
        ConfirmOperationButton temporaryButton = null;
        for (ConfirmDialogButtonType temporaryConfirmButtonType : buttons)
        {
            temporaryButton = new ConfirmOperationButton(temporaryConfirmButtonType,
                    temporaryConfirmButtonType.name());
            temporaryButton.setOnAction(event ->
            {
                activatedOperationButton = temporaryConfirmButtonType;
                hide();
            });

            operationButtons_HBox.getChildren().add(temporaryButton);
        }
    }

    /**
     * Возвращает кнопку, которая была нажата.
     */
    public ConfirmDialogButtonType getActivatedOperationButton()
    {
        return activatedOperationButton;
    }

    public boolean setConfirmOperationOnEnterKey(boolean policy)
    {
        if (policy && info_ScrollPane.getOnKeyPressed() == null)
        {
            info_ScrollPane.setOnKeyPressed(event ->
            {
                if (event.getCode() == KeyCode.ENTER)
                {
                    System.out.println("Нажата Enter. Выполняем операцию.");
                    activatedOperationButton = ConfirmDialogButtonType.OK;
                    hide();
                }
            });
        }
        else
        {
            return false;
        }
        return true;

    }

    /**
     * Устанавливает фон для корневого узла. в данном случае будет окрашено
     * большая часть пространства окна.
     */
    public void setBackgroundToRootNode(Background newBackground)
    {
        if (newBackground == null)
        {
            return;
        }

        root.setBackground(newBackground);
    }


    /**Возвращает список кнопок управления.*/
    public ObservableList<Node> getOperationButtons()
    {
        return operationButtons_HBox.getChildren();
    }

}


enum ConfirmDialogButtonType
{
    CANCEL, OK, REPLACE_FILE, SKIP, UNITE;
}
