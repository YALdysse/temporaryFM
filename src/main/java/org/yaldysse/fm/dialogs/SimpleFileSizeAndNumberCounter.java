package org.yaldysse.fm.dialogs;

import javafx.application.Platform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Описывает роботу потока выполнения, который должен обойти все файлы по указанному
 * пути, рассчитав пры этом количество этих файлов и их размер. После завершения
 * работы вызывается метод {@link org.yaldysse.fm.dialogs.FilesNumberAndSizeCalculator#appearInNodes(int, long)}.
 * Этот метод должны переопределить все классы, которые нуждаются в получение количества
 * файлов и их размер.
 */
public class SimpleFileSizeAndNumberCounter implements Runnable
{
    private final Path[] paths;
    private int filesNumber;
    private long totalSize;
    private int regularFilesNumber;
    private int directoriesNumber;
    private FilesNumberAndSizeCalculator guiDialog;
    private boolean hasInterrupt;

    public SimpleFileSizeAndNumberCounter(final Path targetPath,
                                          FilesNumberAndSizeCalculator aGuiDialog)
    {
        filesNumber = 0;
        totalSize = 0;
        regularFilesNumber = 0;
        directoriesNumber = 0;
        guiDialog = aGuiDialog;
        hasInterrupt = false;
        paths = new Path[1];
        paths[0] = targetPath;
    }

    public SimpleFileSizeAndNumberCounter(final Path[] targetPaths,
                                          FilesNumberAndSizeCalculator aGuiDialog)
    {
        filesNumber = 0;
        totalSize = 0;
        regularFilesNumber = 0;
        directoriesNumber = 0;
        paths = new Path[targetPaths.length];
        guiDialog = aGuiDialog;
        hasInterrupt = false;

        for (int k = 0; k < paths.length; k++)
        {
            paths[k] = targetPaths[k];
        }
    }


    @Override
    public void run()
    {
        try
        {
            for (int k = 0; k < paths.length; k++)
            {
                walkPathsThroughListMethod(paths[k].toAbsolutePath().toFile());
            }
        }
        catch (InterruptedException interruptedException)
        {
            System.out.println(interruptedException.getMessage());
            return;
        }


        Platform.runLater(() ->
        {
            guiDialog.appearInNodes(filesNumber, totalSize);
            guiDialog.appearInNodes(filesNumber,regularFilesNumber,directoriesNumber,
                    totalSize);
        });
    }


    /**
     * Обходит все файлы в заданном каталоге. Количество файлов и значение размера
     * записывает в соответствующие переменные.
     */
    private void walkPathsThroughListMethod(final File targetFile) throws InterruptedException
    {
        if (!Files.isSymbolicLink(targetFile.toPath()))
        {
            String[] dirList = targetFile.list();

            try
            {
                if (dirList == null)
                {
                    //это файл
                    filesNumber++;
                    regularFilesNumber++;
                    totalSize += Files.size(targetFile.toPath());
                    return;
                }
                else if (dirList.length == 0)//пустой каталог
                {
                    filesNumber++;
                    directoriesNumber++;
                    totalSize += Files.size(targetFile.toPath());
                    return;
                }
                else
                {
                    directoriesNumber++;
                    filesNumber++;
                    totalSize += Files.size(targetFile.toPath());
                }
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }


            for (int k = 0; k < dirList.length; k++)
            {
                if (hasInterrupt)
                {
                    throw new InterruptedException("Остановка поток подсчета файлов и их размера.");
                }

                try
                {
                    File temporaryFile = new File(targetFile + File.separator + dirList[k]);
                    //System.out.println(f.toString());

                    if (Files.isSymbolicLink(temporaryFile.toPath()))
                    {
                        regularFilesNumber++;
                        filesNumber++;
                        //System.out.println("Пропускаем : "+ temporaryFile);
                        continue;
                    }
                    if (temporaryFile.isDirectory())
                    {
                        walkPathsThroughListMethod(temporaryFile);
                    }
                    else
                    {
                        regularFilesNumber++;
                        filesNumber++;
                        totalSize += Files.size(temporaryFile.toPath());
                    }

                }
                catch (NoSuchFileException noSuchFileException)
                {
                    System.out.println("Скорее всего битая символическая ссылка");
                }
                catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }
        }
        else
        {
            try
            {
                regularFilesNumber++;
                totalSize += Files.size(targetFile.toPath());
                filesNumber++;
            }
            catch (IOException ioException)
            {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Устанавливает значение переменной остановки потока в true.
     * После этого поток, когда это будет возможно прервет исполнение
     * через исключение.
     */
    public void interrupt()
    {
        hasInterrupt = true;
    }

    public int getRegularFilesNumber()
    {
        return regularFilesNumber;
    }

    public int getDirectoriesNumber()
    {
        return directoriesNumber;
    }

    public int getTotalFiles()
    {
        return filesNumber;
    }

}
