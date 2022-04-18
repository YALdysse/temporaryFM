package org.yaldysse.fm;

import java.nio.file.Path;

/**
 * Инкапсулирует данные о файле/каталоге. Необходим как модель данных таблицы.
 */
public class FileData
{
    private String name;
    private Path path;
    private long size;

    public FileData(String aName, long aSize)
    {
        name = aName;
        size = aSize;
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
}
