package org.yaldysse.fm.dialogs.copy;

import javafx.application.Platform;
import org.yaldysse.fm.dialogs.ConfirmDialogButtonType;
import org.yaldysse.patterns.observer.Observer;
import org.yaldysse.patterns.observer.Subject;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Поток выполнения, предназначен для копирования файлов.09.09.2022 Отправка данных
 * в окно программы теперь не осуществляется после каждого обработанного файла,
 * а строго по таймеру.
 * Было внесено существенное изменение - теперь класс не реализует интерфейс
 * CopyOperationProgress (был удалён). Это сделано потому, что отправка данных
 * была реализована при помощи паттерна Наблюдатель. Теперь же класс реализует
 * интерфейс субъекта, который владеет данными - {@link org.yaldysse.patterns.observer.Subject}.
 */
public class CopyFiles implements Runnable, Subject
{
    private final Path[] sourcePaths;
    private final Path[] destinationPaths;
    /**
     * Хранит количество файлов, что были обработаны.
     */
    private int copiedFilesNumber;
    private long copiedBytes;
    /**
     * Хранит количество файлов, которое было успешно скопировано.
     */
    private int finishedFilesNumber;
    /**
     * Хранит количество скопированных байт, для того,
     * чтобы можно было вычислить скорость.
     */
    private long copiedBytesPortion;
    private long copiedBytesPortion_backup;

    private Path currentFilePath;
    //private final CopyOperationProgress copyOperationProgress;
    /**
     * Указывает, нужно ли отменить операцию.
     */
    private boolean interrupt;
    /**
     * Указывает, была ли отменена операция или нет.
     */
    private boolean interrupted;
    private boolean completed;
    private ConfirmDialogButtonType currentCopyOption;
    public final Object forLock = new Object();
    private Timer timer;
    private TimerTask timerTask;
    public final int timerDelay = 1;
    public final int timerPeriod = 1000;
    private final boolean removeFilesAfterCopy;
    private final boolean copyAttributes;
    private final ArrayList<Observer> observers;
    private boolean fileAlreadyExistsState;
    /**
     * Хранит путь к исходному файлу для копирования, который уже существует
     * в каталоге назначения.
     */
    private Path sourceFileAlreadyExists_Path;
    private Path targetFileAlreadyExists_Path;

    public CopyFiles(final Path[] targetPaths, final Path[] aDestinationPaths,
                     final boolean removeFiles,
                     Observer observer)
    {
        observers = new ArrayList<>();
        registerObserver(observer);
        sourcePaths = targetPaths;
        destinationPaths = aDestinationPaths;
        copiedFilesNumber = 0;
        copiedBytes = 0;
        finishedFilesNumber = 0;
        interrupt = false;
        interrupted = false;
        completed = false;
        copyAttributes = true;
        currentCopyOption = ConfirmDialogButtonType.ASK_ME;
        removeFilesAfterCopy = removeFiles;
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                //System.out.println("Таймер сработал.");
                notifyAboutCurrentProgress();
            }
        };
        timer = new Timer();
    }

    @Override
    public void run()
    {
        timer.schedule(timerTask, timerDelay, timerPeriod);
        try
        {
            for (int k = 0; k < sourcePaths.length; k++)
            {
                walkFilesAndCopy(sourcePaths[k].toFile(), destinationPaths[k].toFile());
            }
        }
        catch (InterruptedException interruptedException)
        {
            timer.cancel();
            completed = true;
            interrupted = true;
            System.out.println(interruptedException);
            notifyAboutCurrentProgress();
            return;
        }

        timer.cancel();
        completed = true;
        notifyAboutCurrentProgress();
    }


    private void walkFilesAndCopy(final File sourceFile, final File destinationFile) throws InterruptedException
    {
        currentFilePath = sourceFile.toPath();
        System.out.println("Обработка файла: " + sourceFile.getAbsolutePath());
        if (interrupt)
        {
            throw new InterruptedException("Отмена операции копирования.");
        }

        if (currentCopyOption != ConfirmDialogButtonType.UNITE_ALL)
        {
            currentCopyOption = ConfirmDialogButtonType.ASK_ME;
        }

        //notifyAboutCurrentProgress();

        try
        {
            //Если файл, в месте назначение с таким именем уже существует
            if (Files.exists(destinationFile.toPath(), LinkOption.NOFOLLOW_LINKS))
            {
                System.out.println("Такой файл уже существует.");
                fileAlreadyExistsState = true;
                sourceFileAlreadyExists_Path = sourceFile.toPath();
                targetFileAlreadyExists_Path = destinationFile.toPath();

                if (currentCopyOption == ConfirmDialogButtonType.ASK_ME)
                {
                    timer.cancel();
                    timer.purge();

                    notifyObservers();

                    System.out.println("Текущий поток: " + Thread.currentThread().getName() +
                            ". Перехожу в режим ожидания.");

                    synchronized (forLock)
                    {
                        forLock.wait();
                    }

                    timerTask = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            notifyAboutCurrentProgress();
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask, timerDelay, timerPeriod);
                    System.out.println("Получено действие: " + currentCopyOption.name());
                    System.out.println("Выходим из режима ожидания");
                    fileAlreadyExistsState = false;
                    sourceFileAlreadyExists_Path = null;
                    targetFileAlreadyExists_Path = null;
                }

                if (currentCopyOption == ConfirmDialogButtonType.SKIP)
                {
                    finishedFilesNumber++;
                    return;
                }
                else if (currentCopyOption == ConfirmDialogButtonType.CANCEL)
                {
                    throw new InterruptedException("Операция копирования отменена пользователем.");
                }
            }


            if (Files.isSymbolicLink(sourceFile.toPath()))
            {
                if (currentCopyOption == ConfirmDialogButtonType.UNITE
                        || currentCopyOption == ConfirmDialogButtonType.UNITE_ALL)
                {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
                }
                else
                {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
                            LinkOption.NOFOLLOW_LINKS);
                }

                if (removeFilesAfterCopy)
                {
                    Files.delete(sourceFile.toPath());
                }

                copiedFilesNumber++;
                long temporaryBytes = Files.size(destinationFile.toPath());
                copiedBytes += temporaryBytes;
                copiedBytesPortion += temporaryBytes;
                finishedFilesNumber++;
                return;
            }
            else if (Files.isRegularFile(sourceFile.toPath(), LinkOption.NOFOLLOW_LINKS))
            {
                if (currentCopyOption == ConfirmDialogButtonType.UNITE
                        || currentCopyOption == ConfirmDialogButtonType.UNITE_ALL)
                {
                    Files.delete(destinationFile.toPath());
                }

                copyFileUsingIOStream(sourceFile, destinationFile);
                if (copyAttributes)
                {
                    copyAttributes(sourceFile.toPath(), destinationFile.toPath());
                }

                if (removeFilesAfterCopy)
                {
                    Files.delete(sourceFile.toPath());
                }
                return;
            }
            else
            {
                if (currentCopyOption != ConfirmDialogButtonType.UNITE &&
                        currentCopyOption != ConfirmDialogButtonType.UNITE_ALL)
                {
                    Files.createDirectory(destinationFile.toPath());
                }

                if (copyAttributes)
                {
                    copyAttributes(sourceFile.toPath(), destinationFile.toPath());
                }

                copiedFilesNumber++;
                finishedFilesNumber++;
                long temporaryBytes = Files.size(destinationFile.toPath());
                copiedBytes += temporaryBytes;
                copiedBytesPortion += temporaryBytes;
            }
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }


        //Нужно быть внимательным, т.к. переходит по символическим ссылкам
        String[] dirList = sourceFile.list();

        for (int k = 0; k < dirList.length; k++)
        {
            File nextSourceFile = new File(sourceFile + File.separator + dirList[k]);
            File nextDestinationFile = new File(destinationFile + File.separator + dirList[k]);
            walkFilesAndCopy(nextSourceFile, nextDestinationFile);
        }
    }

    /**
     * Копирует заданный файл в заданное место используя FileInputStream для чтения
     * и FileOutputStream для записи. Предназначен только для копирования.
     */
    private void copyFileUsingIOStream(final File sourceFile, final File destinationFile) throws InterruptedException
    {
        System.out.println("Старт копирования файла через IO : " + sourceFile.getAbsolutePath());
        try (BufferedInputStream bufferedInputStream =
                     new BufferedInputStream(new FileInputStream(sourceFile));
             BufferedOutputStream bufferedOutputStream =
                     new BufferedOutputStream(new FileOutputStream(destinationFile)))
        {
            byte[] readBytes = new byte[8192];
            int i;

//            while ((readBytes = bufferedInputStream.readNBytes(4096)
//            ) != null)
            while ((i = bufferedInputStream.read(readBytes)) != -1)
            {
                if (interrupt)
                {
                    bufferedOutputStream.close();
                    bufferedInputStream.close();
                    throw new InterruptedException("Операция отменена пользователем.");
                }

                bufferedOutputStream.write(readBytes, 0, i);
                copiedBytes += i;
                copiedBytesPortion += i;
            }
            System.out.println("Скорее всего писать нечего");
            copiedFilesNumber++;
            finishedFilesNumber++;
        }
        catch (FileNotFoundException fileNotFoundException)
        {
            fileNotFoundException.printStackTrace();
        }
        catch (IOException ioException)
        {
            ioException.printStackTrace();
        }
        System.out.println("Файл " + destinationFile.toPath() + " должен быть успешно скопирован.");
    }


    /**
     * Копирует аттрибуты времени и расширенные аттрибуты.
     */
    private void copyAttributes(final Path sourceFile, final Path targetFile) throws IOException
    {
        BasicFileAttributeView basicFileAttributeView_source = Files.getFileAttributeView(sourceFile, BasicFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributeView basicFileAttributeView_target = Files.getFileAttributeView(targetFile, BasicFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);

        BasicFileAttributes basicFileAttributes_source = basicFileAttributeView_source.readAttributes();
        basicFileAttributeView_target.setTimes(basicFileAttributes_source.lastModifiedTime(),
                basicFileAttributes_source.lastAccessTime(), basicFileAttributes_source.creationTime());


        UserDefinedFileAttributeView extendedAttributes_source = Files.getFileAttributeView(sourceFile,
                UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        UserDefinedFileAttributeView extendedAttributes_target = Files.getFileAttributeView(targetFile,
                UserDefinedFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

        ByteBuffer byteBuffer = null;

        for (String temporaryAttributeName : extendedAttributes_source.list())
        {
            byteBuffer = ByteBuffer.allocate(extendedAttributes_source.size(temporaryAttributeName));
            extendedAttributes_source.read(temporaryAttributeName, byteBuffer);
            byteBuffer.flip();
            extendedAttributes_target.write(temporaryAttributeName, byteBuffer);
            byteBuffer.clear();
        }
    }

    private void notifyAboutCurrentProgress()
    {
        copiedBytesPortion_backup = copiedBytesPortion;
        notifyObservers();
        copiedBytesPortion = 0;
    }


    public void setInterrupt(final boolean value)
    {
        interrupt = value;
    }


    public void setCopyOption(final ConfirmDialogButtonType newCopyOption)
    {
        currentCopyOption = newCopyOption;
    }

    @Override
    public void registerObserver(Observer observer)
    {
        System.out.println("Добавлен новый наблюдатель: " + observer.toString());
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer)
    {
        observers.remove(observer);
        System.out.println("Удаление наблюдателя: " + observer.toString());
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

    /**
     * Возвращает общее количество скопированный байт.
     */
    public long getCopiedBytesNumber()
    {
        return copiedBytes;
    }

    /**
     * Возвращает количество файлов, которые были обработаны.
     */
    public int getProcessedFilesNumber()
    {
        return copiedFilesNumber;
    }

    /**
     * Возвращает путь к файлу, который обрабатывается в данный момент.
     */
    public Path getCurrentFilePath()
    {
        return currentFilePath;
    }

    /**
     * Возвращает количество байтов, что были скопировано от предыдущей отправки
     * результатов.
     */
    public long getCopiedBytesPortion()
    {
        return copiedBytesPortion_backup;
    }

    /**
     * Позволяет узнать, завершена ли операция. Данный метод ничего не сообщает
     * об успешности операции.
     */
    public boolean isCompleted()
    {
        return completed;
    }

    /**
     * Позволяет узнать, была ли прервана операция.
     */
    public boolean isInterrupted()
    {
        return interrupted;
    }

    /**
     * Возвращает количество файлов, который были действительно скопированы.
     */
    public int getSuccessfullyCopiedFilesNumber()
    {
        return finishedFilesNumber;
    }

    public Path getSourceFilePathAlreadyExists()
    {
        return sourceFileAlreadyExists_Path;
    }

    public Path getTargetFilePathAlreadyExists()
    {
        return targetFileAlreadyExists_Path;
    }

    public boolean isFileAlreadyExists()
    {
        return fileAlreadyExistsState;
    }
}
