package org.yaldysse.fm;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Инкапсулирует данные о файле/каталоге. Необходим как модель данных таблицы.
 */
public class FileData
{
    private String name;
    private Path path;
    private long size;
    private String owner;
    private FileTime lastModifiedTime;
    private FileTime creationTime;
    private boolean hasDirectory;
    private boolean hasFile;
    private boolean hasSymbolicLink;

    public FileData(String aName, long aSize)
    {
        name = aName;
        size = aSize;

        if (aName == null)
        {
            name = "";
        }
    }

    public FileData(String aName, long aSize, String aOwner)
    {
        name = aName;
        size = aSize;
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

    public void setPath(Path path)
    {
        this.path = path;
    }

    public void setSize(long newSize)
    {
        if (newSize > 0L)
        {
            size = newSize;
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

    public String getName()
    {
        return name;
    }

    public Path getPath()
    {
        return path;
    }

    public long getSize()
    {
        return size;
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

    public String getLastModifiedTime(boolean string)
    {
        if (lastModifiedTime == null)
        {
            return "";
        }
        return lastModifiedTime.toString();
    }

    public FileTime getCreationTime()
    {
        return creationTime;
    }

    public String getCreationTime(boolean string)
    {
        if (creationTime == null)
        {
            return "suka";
        }
        return creationTime.toString();
    }

    public String getType()
    {
        if(hasDirectory)
        {
            return "ZDirectory";
        }
        else if(hasFile)
        {
            return "File";
        }
        else if(hasSymbolicLink)
        {
            return "Symbolic Link";
        }
        return "Other";
    }

    public boolean isDirectory()
    {
        return hasDirectory;
    }

    public boolean isFile()
    {
        return hasFile;
    }

    public boolean isSymbolicLink()
    {
        return hasSymbolicLink;
    }
}
