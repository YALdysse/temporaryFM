package org.yaldysse.fm;

import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;

/**Внимание: Нет реализации под MacOS*/
public class FileStoreInfo
{
    private String label;
    //Linux - /dev/sda5 ; Windows - C:
    private String deviceName;
    private Path path;
    private FileStore fileStore;


    /**
     * Возвращает метку хранилища данных.
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Возвращает имя устройства. Актуально для систем
     * Linux, где каждый раздел накопителя считается
     * как отдельное устройство. Пример, /dev/sda3
     */
    public String getDeviceName()
    {
        return deviceName;
    }

    /**
     * Возвращает путь к хранилищу данных.
     */
    public Path getPath()
    {
        return path;
    }

    public FileStore getFileStore()
    {
        return fileStore;
    }

    /**
     * @deprecated Реализация только для Linux и Windows.
     */
    public static ArrayList<FileStoreInfo> getAll()
    {
        ArrayList<FileStoreInfo> allObjects = new ArrayList<>();
        String pathToFileStore = "";
        String fileStoreLabel = "";
        String fileStoreDevice = "";//Linux
        String string = "";

        Iterable<FileStore> fileStoreIterable = FileSystems.getDefault().getFileStores();

        for (FileStore fileStore : fileStoreIterable)
        {
            string = fileStore.toString();

            if (System.getProperty("os.name").contains("Linux"))
            {
                if (string.indexOf("(") == 0)
                {
                    fileStoreDevice = "";
                }
                else
                {
                    pathToFileStore = string.substring(0,
                            string.indexOf("(") - 1);
//                    fileStoreDevice = string.substring(
//                            string.indexOf("(") + 1, string.indexOf(")"));
                    fileStoreDevice = fileStore.name();
                    Path temporaryPath = Path.of(pathToFileStore);
                    if (temporaryPath.getFileName() == null)
                    {
                        fileStoreLabel = "";
                    }
                    else
                    {
                        fileStoreLabel = temporaryPath.getFileName().toString();
                    }
                }
            }
            else if (System.getProperty("os.name").contains("Windows"))
            {
                /*YAL-USB (D:) or (E:)*/
                int pathIndex = string.indexOf("(") + 1;
                pathToFileStore = string.substring(pathIndex,
                        string.indexOf(")"));

                //int labelEndIndex = string.indexOf("(");
//                if (labelEndIndex == 0)
//                {
//                    fileStoreLabel = "";
//                }
//                else
//                {
//                    fileStoreLabel = string.substring(0,
//                            labelEndIndex - 1);
//                }
                fileStoreLabel = fileStore.name();
                fileStoreDevice = pathToFileStore;
                pathToFileStore = pathToFileStore + "\\";
            }

            FileStoreInfo fileStoreInfo = new FileStoreInfo();
            fileStoreInfo.fileStore = fileStore;
            fileStoreInfo.deviceName = fileStoreDevice;
            fileStoreInfo.label = fileStoreLabel;
            fileStoreInfo.path = Path.of(pathToFileStore);
            allObjects.add(fileStoreInfo);

//            System.out.println("=== Info ==="
//                    +"\nDeviceName: " +fileStoreDevice
//            +"\nLabel: " + fileStoreLabel
//            +"\nPath: " + fileStoreInfo.getPath().toAbsolutePath().toString());
        }
        return allObjects;
    }

    /**
     * Для Linux возвращает метку хранилища данных и полный путь.
     * Для Windows - Метку хранилища и букву диска.
     */
    @Override
    public String toString()
    {
        String currentOS = System.getProperty("os.name");
        if (currentOS.contains("Windows"))
        {
            return label + " (" + deviceName + ")";
        }
        else if (currentOS.contains("Linux"))
        {
            return label + " (" + path.toAbsolutePath().toString() + ")";
        }
        else
        {
            return label;
        }
    }
}
