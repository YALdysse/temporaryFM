package org.yaldysse.fm.dialogs;

/**
 * Интерфейс предназначен для взаимодействия компонентов
 * графического интерфейса и потока {@link org.yaldysse.fm.dialogs.SimpleFileSizeAndNumberCounter}
 * . После того как робота потока закончится, он вызывает метод,
 * {@link FilesNumberAndSizeCalculator#appearInNodes(int, long)}.
 * Реализацию этого метода должен предоставить каждый класс,
 * который нуждается в таких услугах.
 */
public interface FilesNumberAndSizeCalculator
{
    public void appearInNodes(final int aFilesNumber, final long aTotalSize);

    public default void appearInNodes(final int aFilesNumber, final int aRegularFilesNumber,
                                      final int aDirectoriesNumber, final long aTotalSize)
    {

    }
}
