package org.yaldysse.fm;

import org.yaldysse.tools.StorageCapacity;

import java.io.File;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Инкапсулирует данные о файле/каталоге. Необходим как модель данных таблицы.
 */
public class FileData
{
    private String name;
    private long size;
    private StorageCapacity size_StorageCapacity;
    private String owner;
    private FileTime lastModifiedTime;
    private FileTime creationTime;
    private boolean hasDirectory;
    private boolean hasFile;
    private boolean hasSymbolicLink;
    private boolean hasWastedSymbolicLink;
    private Path symbolicLinkTargetPath;

    public FileData(String aName, long aSize)
    {
        name = aName;
        size = aSize;
        size_StorageCapacity = StorageCapacity.ofBytes(aSize);

        if (aName == null)
        {
            name = "";
        }
    }

    public FileData(String aName, long aSize, String aOwner)
    {
        name = aName;
        size = aSize;
        size_StorageCapacity = StorageCapacity.ofBytes(aSize);
        owner = aOwner;

        if (aName == null)
        {
            name = "";
        }
        if (aOwner == null)
        {
            aOwner = "";
        }
    }


    public void setName(String newName)
    {
        name = newName;
    }

    public void setSize(long newSize)
    {
        if (newSize >= 0L)
        {
            size = newSize;
            size_StorageCapacity = StorageCapacity.ofBytes(newSize);
        }
    }

    public void setOwner(String aOwner)
    {
        if (aOwner == null)
        {
            return;
        }
        owner = aOwner;
    }

    public void setLastModifiedTime(FileTime aLastModifiedTime)
    {
        if (aLastModifiedTime == null)
        {
            return;
        }
        lastModifiedTime = aLastModifiedTime;
    }

    public void setCreationTime(FileTime aCreationTime)
    {
        if (aCreationTime == null)
        {
            return;
        }
        creationTime = aCreationTime;
    }

    public void setDirectory(boolean aDirectory)
    {
        hasDirectory = aDirectory;
    }

    public void setFile(boolean aFile)
    {
        hasFile = aFile;
    }


    public void setSymbolicLink(boolean symbolicLink)
    {
        this.hasSymbolicLink = symbolicLink;
    }

    public void setWastedSymbolicLink(boolean value)
    {
        hasWastedSymbolicLink=value;
    }

    public void setSymbolicLinkPath(final Path targetPath)
    {
        if (targetPath == null)
        {
            return;
        }
        symbolicLinkTargetPath = targetPath;
    }

    public String getName()
    {
        return name;
    }

    public long getSize()
    {
        return size;
    }

    public String getSize(boolean value)
    {
        if (size_StorageCapacity == null)
        {
            return "";
        }
        return size_StorageCapacity.toString();
    }

    public String getOwner()
    {
        return owner;
    }

    public FileTime getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    public long getLastModifiedTime(final TimeUnit timeUnit)
    {
        if (lastModifiedTime == null)
        {
            return -1L;
        }

        return lastModifiedTime.to(TimeUnit.of(ChronoUnit.DAYS));
        //return lastModifiedTime.to(timeUnit);
    }

    /**
     * Возвращает дату и время последнего изменения в заданном формате.
     */
    public String getLastModifiedTime(DateTimeFormatter formatter)
    {
        if (lastModifiedTime == null)
        {
            return "";
        }

        LocalDateTime lastModifiedTime_LocalDateTime = LocalDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault());
        return lastModifiedTime_LocalDateTime.format(formatter);
    }

    public FileTime getCreationTime()
    {
        return creationTime;
    }

    /**
     * Возвращает значение даты и времени создания файла в заданном формате.
     */
    public String getCreationTime(DateTimeFormatter formatter)
    {
        if (creationTime == null)
        {
            return "";
        }

        LocalDateTime creationTime_LocalDateTime = LocalDateTime.ofInstant(creationTime.toInstant(), ZoneId.systemDefault());
        return creationTime_LocalDateTime.format(formatter);
        //return creationTime.toString();
    }

    public String getType()
    {
        if (hasSymbolicLink)
        {
            if(hasWastedSymbolicLink)
            {
                return "Wasted symbolic link";
            }
            if (hasDirectory)
            {
                return "Symbolic link at directory " + symbolicLinkTargetPath.toAbsolutePath().toString();
            }
            else if (hasFile)
            {
                return "Symbolic link at file " + symbolicLinkTargetPath.toAbsolutePath().toString();
            }
        }
        else if (hasDirectory)
        {
            return "Directory";
        }
        else if (hasFile)
        {
            return "File";
        }
        return "Other";
    }

    public boolean isDirectory()
    {
        return hasDirectory;
    }

    public short isDirectory(boolean value)
    {
        return hasDirectory == true ? (short) 1 : (short) 0;
    }

    public boolean isFile()
    {
        return hasFile;
    }

    public boolean isSymbolicLink()
    {
        return hasSymbolicLink;
    }


    public FileData cloneFileData() throws CloneNotSupportedException
    {
        FileData temporaryFileData = new FileData(getName(), getSize());
        temporaryFileData.setOwner(getOwner());
        temporaryFileData.setCreationTime(getCreationTime());
        temporaryFileData.setLastModifiedTime(getLastModifiedTime());
        temporaryFileData.setFile(hasFile);
        temporaryFileData.setDirectory(hasDirectory);
        temporaryFileData.setSymbolicLink(hasSymbolicLink);
        return temporaryFileData;
    }
}
