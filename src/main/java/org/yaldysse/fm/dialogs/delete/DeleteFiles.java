package org.yaldysse.fm.dialogs.delete;

import javafx.application.Platform;
import org.yaldysse.patterns.observer.Observer;
import org.yaldysse.patterns.observer.Subject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * Класс, описывающие роботу потока по удалению заданных файлов. Перед завершением
 * работы потока вызывается метод {@link org.yaldysse.fm.dialogs.delete.DeleteProgress#appearDeleteProgress(int, Path, boolean, boolean, ArrayList)},
 * для указанного объекта класса, что реализовывает интерфейс {@link org.yaldysse.fm.dialogs.delete.DeleteProgress}.
 * Таким образом поток передает данные об состоянии операции классу, что реализовывает
 * этот интерфейс.
 * 14.09.2022 Адаптирован для использования паттерна Стратегия.
 */
public class DeleteFiles implements Runnable, Subject
{
    private Path[] paths;
    private int deletedFiles;
    private Path currentFilePath;
    private boolean interrupt;
    private ArrayList<Path> accessDeniedErrorPaths_ArrayList;
    private ArrayList<Observer> observers;
    private boolean interrupted;
    private boolean completed;

    public DeleteFiles(Path[] targetPaths, Observer newObserver)
    {
        observers = new ArrayList<>();
        registerObserver(newObserver);
        paths = targetPaths;
        deletedFiles = 0;
        interrupt = false;
        interrupted = false;
        completed = false;
        accessDeniedErrorPaths_ArrayList = new ArrayList<>();
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
            interrupted = true;
            completed = true;
            //notifyAboutCurrentProgress(completed, interrupted);
            notifyObservers();
            System.out.println(interruptedException);
            return;
        }
        completed = true;
        //notifyAboutCurrentProgress(completed, interrupted);
        notifyObservers();
    }

    public void walkPathsThroughListMethod(final File targetFile) throws InterruptedException
    {
        if (!Files.isSymbolicLink(targetFile.toPath()))
        {
            currentFilePath = targetFile.toPath();
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

                        if (Files.isDirectory(currentFilePath, LinkOption.NOFOLLOW_LINKS))
                        {
                            walkPathsThroughListMethod(temporaryFile);
                        }
                        if (Files.deleteIfExists(currentFilePath))
                        {
                            deletedFiles++;
                            //notifyAboutCurrentProgress(completed, interrupted);
                            notifyObservers();
                        }
                    }
                    catch (AccessDeniedException accessDeniedException)
                    {
                        accessDeniedErrorPaths_ArrayList.add(currentFilePath);
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
        catch (AccessDeniedException accessDeniedException)
        {
            accessDeniedErrorPaths_ArrayList.add(currentFilePath);
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
    }

//    private void notifyAboutCurrentProgress(final boolean completed,
//                                            final boolean interrupted)
//    {
//        Platform.runLater(() ->
//        {
//            deleteProgress.appearDeleteProgress(deletedFiles,
//                    currentFilePath, completed, interrupted, accessDeniedErrorPaths_ArrayList);
//        });
//    }

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

    public boolean isInterrupted()
    {
        return interrupted;
    }

    public boolean isCompleted()
    {
        return completed;
    }

    /**
     * Возвращает путь к файлу, который находится в обработке на данный момент.
     */
    public Path getCurrentFilePath()
    {
        return currentFilePath;
    }

    /**
     * Возвращает пути к файлам, которые не были удалены из-за невозможности получения
     * доступа к ним.
     */
    public ArrayList<Path> getAccessDeniedErrorPaths()
    {
        return accessDeniedErrorPaths_ArrayList;
    }

    @Override
    public void registerObserver(Observer observer)
    {
        observers.add(observer);
        System.out.println("Новый Наблюдатель для субъекта удаления зарегистрирован.");
    }

    @Override
    public void removeObserver(Observer observer)
    {
        observers.remove(observer);
        System.out.println("Удален Наблюдатель для субъекта удаления.");
    }

    @Override
    public void notifyObservers()
    {
        for (Observer observer : observers)
        {
            Platform.runLater(() ->
            {
                observer.updateData(this);
            });
        }
    }
}
