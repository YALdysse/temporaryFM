package org.yaldysse.fm.dialogs.copy;

import java.nio.file.Path;

/**Интерфейс описывает методы, которые позволят обмениваться данными между потоком
 * копирования файлов {@link org.yaldysse.fm.dialogs.copy.CopyFiles} и соответсвующим
 * диалоговым окном {@link org.yaldysse.fm.dialogs.copy.CopyFilesDialog}. При этом
 * класс диалогового окна должен расширять этот интерфейс.
 * @author Y@Ldysse*/
public interface CopyOperationProgress
{
    public void appearOperationProgress(final boolean completed, final boolean interrupted,
                                        final int copiedFilesNumber, final int finishedFileNumber,
                                        final long copiedBytes, final long copiedBytesPortion,
                                        final Path processedFilePath);

    /**Этот метод будет вызван, если при копировании обнаружится, что файл с
     * таким именем уже существует. Класс, расширяющий этот интерфейс должен
     * определить реализацию этого метода, что позволит контролировать поведение
     * операции копирования в таком случае.*/
    public void fileAlreadyExists(final Path sourceFilePath,
                                  final Path destinationFilePath);
}
