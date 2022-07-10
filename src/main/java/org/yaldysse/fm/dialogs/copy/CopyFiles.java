package org.yaldysse.fm.dialogs.copy;

import javafx.application.Platform;
import org.yaldysse.fm.dialogs.ConfirmDialogButtonType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Поток выполнения, предназначен для копирования файлов. Взаимодействует
 * с диалоговым окном копирования посредством вызова методов, что предоставлены
 * интерфейсом {@link org.yaldysse.fm.dialogs.copy.CopyOperationProgress}.
 */
public class CopyFiles implements Runnable
{
    private final Path[] sourcePaths;
    private final Path[] destinationPaths;
    private int copiedFilesNumber;
    private long copiedBytes;
    private int finishedFilesNumber;
    /**
     * Хранит количество скопированных байт, для того,
     * чтобы можно было вычислить скорость.
     */
    private long copiedBytesPortion;
    private long copiedBytesPortion_backup;
    private Path currentFilePath;
    private final CopyOperationProgress copyOperationProgress;
    private boolean interrupt;
    private ConfirmDialogButtonType currentCopyOption;
    public final Object forLock = new Object();
    private Timer timer;
    private TimerTask timerTask;
    public final int timerDelay = 1;
    public final int timerPeriod = 1000;
    private boolean removeFilesAfterCopy;
    private boolean copyAttributes;

    public CopyFiles(final Path[] targetPaths, final Path[] aDestinationPaths,
                     CopyOperationProgress aCopyProgress, final boolean removeFiles)
    {
        sourcePaths = targetPaths;
        destinationPaths = aDestinationPaths;
        copiedFilesNumber = 0;
        copiedBytes = 0;
        finishedFilesNumber = 0;
        copyOperationProgress = aCopyProgress;
        interrupt = false;
        copyAttributes = true;
        currentCopyOption = ConfirmDialogButtonType.ASK_ME;
        removeFilesAfterCopy = removeFiles;
        //forLock = new Object();
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                //System.out.println("Таймер сработал.");
                notifyAboutCurrentProgress(false, false);
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
            notifyAboutCurrentProgress(true, true);
            System.out.println(interruptedException);
            timer.cancel();
            return;
        }
        notifyAboutCurrentProgress(true, false);
        timer.cancel();
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

        notifyAboutCurrentProgress(false, false);

        try
        {
            //Если файл, в месте назначение с таким именем уже существует
            if (Files.exists(destinationFile.toPath(), LinkOption.NOFOLLOW_LINKS))
            {
                System.out.println("Такой файл уже существует.");

                if (currentCopyOption == ConfirmDialogButtonType.ASK_ME)
                {
                    timer.cancel();
                    timer.purge();
                    Platform.runLater(() -> copyOperationProgress.fileAlreadyExists(
                            sourceFile.toPath(), destinationFile.toPath()));

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
                            notifyAboutCurrentProgress(false, false);
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask, timerDelay, timerPeriod);
                    System.out.println("Получено действие: " + currentCopyOption.name());
                    System.out.println("Выходим из режима ожидания");
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
            byte[] readBytes = null;

            while ((readBytes = bufferedInputStream.readNBytes(4096)
            ) != null)
            {
                if (interrupt)
                {
                    bufferedOutputStream.close();
                    bufferedInputStream.close();
                    throw new InterruptedException("Операция отменена пользователем.");
                }

                bufferedOutputStream.write(readBytes);
                copiedBytes += readBytes.length;
                copiedBytesPortion += readBytes.length;

                if (bufferedInputStream.available() < 1)
                {
                    System.out.println("Скорее всего писать нечего");
                    copiedFilesNumber++;
                    finishedFilesNumber++;
                    break;
                }
            }

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

    private void notifyAboutCurrentProgress(final boolean completed,
                                            final boolean interrupted)
    {
        copiedBytesPortion_backup = copiedBytesPortion;
        Platform.runLater(() ->
                copyOperationProgress.appearOperationProgress(completed, interrupted, copiedFilesNumber,
                        finishedFilesNumber, copiedBytes, copiedBytesPortion_backup, currentFilePath));
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

}
