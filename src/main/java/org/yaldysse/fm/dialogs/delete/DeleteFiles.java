package org.yaldysse.fm.dialogs.delete;

import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Класс, описывающие роботу потока по удалению заданных файлов. Перед завершением
 * работы потока вызывается метод {@link org.yaldysse.fm.dialogs.delete.DeleteProgress#appearDeleteProgress(int, Path, boolean, boolean)},
 * для указанного обьекта класса, что реализовывает интерфейс {@link org.yaldysse.fm.dialogs.delete.DeleteProgress}.
 * Таким образом поток передает данные об состоянии операции классу, что реализовывает
 * этот интерфейс.
 */
public class DeleteFiles implements Runnable
{
    private Path[] paths;
    private int deletedFiles;
    private Path currentFilePath;
    private DeleteProgress deleteProgress;
    private boolean interrupt;

    public DeleteFiles(Path[] targetPaths, DeleteProgress aDeleteProgress)
    {
        paths = targetPaths;
        deletedFiles = 0;
        deleteProgress = aDeleteProgress;
        interrupt = false;
    }

    @Override
    public void run()
    {
        try
        {
            for (int k = 0; k < paths.length; k++)
            {
                walkPathsThroughListMethod(paths[k].toFile());
            }
        }
        catch (InterruptedException interruptedException)
        {
            notifyAboutCurrentProgress(true, true);
            System.out.println(interruptedException);
            return;
        }
        notifyAboutCurrentProgress(true, false);
    }

    public void walkPathsThroughListMethod(final File targetFile) throws InterruptedException
    {
        if (!Files.isSymbolicLink(targetFile.toPath()))
        {

            //Переходит по символическим ссылкам
            String[] dirList = targetFile.list();

            if (dirList != null)
            {

                for (int k = 0; k < dirList.length; k++)
                {
                    if (interrupt)
                    {
                        throw new InterruptedException("Отмена операции удаления.");
                    }

                    try
                    {
                        File temporaryFile = new File(targetFile + File.separator + dirList[k]);
                        currentFilePath = temporaryFile.toPath();
                        //System.out.println(f.toString());

//                if (Files.isSymbolicLink(temporaryFile.toPath()))
//                {
//                    //System.out.println("Пропускаем : "+ temporaryFile);
//                    continue;
//                }
                        if (Files.isDirectory(currentFilePath, LinkOption.NOFOLLOW_LINKS))
                        {
                            walkPathsThroughListMethod(temporaryFile);
                        }
                        if (Files.deleteIfExists(currentFilePath))
                        {
                            deletedFiles++;
                            notifyAboutCurrentProgress(false, false);
                        }
                    }
                    catch (NoSuchFileException noSuchFileException)
                    {
                        System.out.println("Скорее всего битая символическая ссылка при удалении." + noSuchFileException);
                    }
                    catch (IOException ioException)
                    {
                        ioException.printStackTrace();
                    }
                }
            }
        }

        try
        {
            if (Files.deleteIfExists(targetFile.toPath()))
            {
                deletedFiles++;
                currentFilePath = targetFile.toPath();
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

    private void notifyAboutCurrentProgress(final boolean completed,
                                            final boolean interrupted)
    {
        Platform.runLater(() ->
        {
            deleteProgress.appearDeleteProgress(deletedFiles,
                    currentFilePath, completed, interrupted);
        });
    }

    public int getDeletedFilesNumber()
    {
        return deletedFiles;
    }

    public void setInterrupt(final boolean value)
    {
        interrupt = value;
    }


    /**
     * Позволяет удалить файл. Также удаляет каталоги с файлами и символические
     * ссылки не переходя по ним.
     *
     * @throws IOException В случае возникновения ошибки ввода-вывода.
     */
    public static void deleteFileRecursively(final Path targetPath) throws IOException
    {
        if (!Files.isSymbolicLink(targetPath))
        {
            //Переходит по символическим ссылкам
            String[] dirList = targetPath.toFile().list();
            Path temporaryPath = null;

            if (dirList != null)
            {
                for (int k = 0; k < dirList.length; k++)
                {
                    temporaryPath = targetPath.resolve(dirList[k]);

                    if (Files.isDirectory(temporaryPath, LinkOption.NOFOLLOW_LINKS))
                    {
                        deleteFileRecursively(temporaryPath);
                    }
                    else
                    {
                        Files.delete(targetPath);
                    }
                }
            }
        }
        Files.delete(targetPath);

    }
}
