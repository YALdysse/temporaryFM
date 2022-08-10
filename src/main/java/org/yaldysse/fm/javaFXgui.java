package org.yaldysse.fm;

import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public abstract class javaFXgui
{
    /**
     * С помощью этой переменной можно создавать GUI без привязки к разрешению.
     */
    public static double rem = new Text("").getBoundsInParent().getHeight();
    private static Text textToCalculateWidth = new Text("");

    /**
     * Позволяет подогнать ширину елемента под его текст.
     */
    public static void fitRegionWidthToText(final Region region, final String text,
                                            final Font font)
    {
        textToCalculateWidth.setFont(font);
        textToCalculateWidth.setText(text);
        region.setPrefWidth(textToCalculateWidth.getBoundsInParent().getWidth());
    }
}
