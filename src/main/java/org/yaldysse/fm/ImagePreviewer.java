package org.yaldysse.fm;

import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Превью окно основанное на Popup для быстрого просмотра изображений.
 */
public class ImagePreviewer
{
    private ImageView imageView;
    private Image image;
    private int maxSizeViewerPercent;

    private Popup imagePreview_Popup;
    private VBox root;
    private VBox image_VBox;

    /**
     * @param newMaxSizePercent Значение в процентах, что определяет максимальный
     *                          размер превью относительно текущего разрешения.
     */
    public ImagePreviewer(final Image newImage, final int newMaxSizePercent)
    {
        image = newImage;
        createImageView();
        maxSizeViewerPercent = newMaxSizePercent;
        initializeComponents();
    }

    public ImagePreviewer()
    {
        maxSizeViewerPercent = 20;
        initializeComponents();
    }

    private void initializeComponents()
    {
        imagePreview_Popup = new Popup();
        imagePreview_Popup.setAutoHide(true);

        image_VBox = new VBox();
//        image_VBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT,
//                CornerRadii.EMPTY, Insets.EMPTY)));

        root = new VBox(image_VBox);
        root.setPadding(new Insets(FM_GUI.rem * 0.1D));
//        root.setBackground(new Background(new BackgroundFill(Color.WHITESMOKE,
//                CornerRadii.EMPTY, Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(Color.WHITESMOKE,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.MEDIUM)));

        imagePreview_Popup.getContent().add(root);
    }

    public void setImage(final Image newImage)
    {
        if (newImage != null)
        {
            image = newImage;
        }
        createImageView();
    }


    /**
     * Рассчитывает размер области превью таким образом, чтобы изображение
     * не растягивалось и при этом не было значительно больше максимально допустимых
     * размеров.
     */
    private void calculateMaxSize()
    {
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        final int screenWidth = (int) screenBounds.getWidth();
        final int screenHeight = (int) screenBounds.getHeight();

        int maxPopupWidth = -1;
        int maxPopupHeight = -1;

        //Вычисляем максимальный размер с сохранением пропорций
        maxPopupWidth = (int) (screenWidth * ((double) maxSizeViewerPercent / 100.0D));
        maxPopupHeight = (int) (screenHeight * ((double) maxSizeViewerPercent / 100.0D));

        int preferredWidth = -1;
        int preferredHeight = -1;

        //Ширина изображения - большая сторона
        if (image.getWidth() > image.getHeight())
        {
            if (image.getWidth() > maxPopupWidth)
            {
                preferredWidth = maxPopupWidth;
            }
            else
            {
                preferredWidth = (int) image.getWidth();
            }

            imageView.setFitWidth(preferredWidth);
        }
        else//Высота больше
        {
            if (image.getHeight() > maxPopupHeight)
            {
                preferredHeight = maxPopupHeight;
            }
            else
            {
                preferredHeight = (int) image.getHeight();
            }
            imageView.setFitHeight(preferredHeight);
        }

    }


    private void createImageView()
    {
        imageView = new ImageView(image);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(true);

        image_VBox.getChildren().clear();
        image_VBox.getChildren().add(imageView);

        calculateMaxSize();
    }

    public void setMaxSizePreview(final int percent)
    {
        maxSizeViewerPercent = percent;
        calculateMaxSize();
    }

    public void show(final Window window)
    {
        if (imagePreview_Popup.isShowing())
        {
            imagePreview_Popup.hide();
        }

        imagePreview_Popup.show(window);
    }

    public void show(final Window window, final double x, final double y)
    {
        if (imagePreview_Popup.isShowing())
        {
            imagePreview_Popup.hide();
        }

        imagePreview_Popup.show(window, x, y);
    }

    /**
     * Позволяет отображить Popup рядом с окном-владельцем, если параметр равен
     * true.
     */
    public void show(final Window window, boolean nearOwnerWindow)
    {
        if (imagePreview_Popup.isShowing())
        {
            imagePreview_Popup.hide();
        }

        int x;
        int y;

        if (!nearOwnerWindow)
        {
            x = 20;
            y = 20;
        }

        x = (int) (window.getX() - root.getBoundsInParent().getWidth() - FM_GUI.rem * 0.3D);
        y = (int) window.getY();
        //System.out.println(x + ": " + y);
        imagePreview_Popup.show(window, x, y);
    }

}
