package org.yaldysse.fm;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

public class EditableBasicFileAttributes implements BasicFileAttributes
{
    private long size;
    private boolean hasSymbolicLink;
    private boolean hasFile;
    private boolean hasDirectory;
    private boolean hasOther;
    private FileTime creationTime;
    private FileTime lastAccessTime;
    private FileTime lastModifiedTime;

    public EditableBasicFileAttributes(long size, boolean hasSymbolicLink, boolean hasFile, boolean hasDirectory, boolean hasOther, FileTime creationTime, FileTime lastAccessTime, FileTime lastModifiedTime)
    {
        this.size = size;
        this.hasSymbolicLink = hasSymbolicLink;
        this.hasFile = hasFile;
        this.hasDirectory = hasDirectory;
        this.hasOther = hasOther;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
        this.lastModifiedTime = lastModifiedTime;
    }

    public EditableBasicFileAttributes()
    {
        size = 0;
        hasFile = false;
        hasDirectory = false;
        hasOther = true;
        hasSymbolicLink = false;
        creationTime = FileTime.from(Instant.now());
        lastModifiedTime = FileTime.from(Instant.now());
        lastAccessTime = FileTime.from(Instant.now());
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public void setHasSymbolicLink(boolean hasSymbolicLink)
    {
        this.hasSymbolicLink = hasSymbolicLink;
    }

    public void setHasFile(boolean hasFile)
    {
        this.hasFile = hasFile;
    }

    public void setHasDirectory(boolean hasDirectory)
    {
        this.hasDirectory = hasDirectory;
    }

    public void setHasOther(boolean hasOther)
    {
        this.hasOther = hasOther;
    }

    public void setCreationTime(FileTime creationTime)
    {
        this.creationTime = creationTime;
    }

    public void setLastAccessTime(FileTime lastAccessTime)
    {
        this.lastAccessTime = lastAccessTime;
    }

    public void setLastModifiedTime(FileTime lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public FileTime lastModifiedTime()
    {
        return lastModifiedTime;
    }

    @Override
    public FileTime lastAccessTime()
    {
        return lastAccessTime;
    }

    @Override
    public FileTime creationTime()
    {
        return creationTime;
    }

    @Override
    public boolean isRegularFile()
    {
        return hasFile;
    }

    @Override
    public boolean isDirectory()
    {
        return hasDirectory;
    }

    @Override
    public boolean isSymbolicLink()
    {
        return hasSymbolicLink;
    }

    @Override
    public boolean isOther()
    {
        return hasOther;
    }

    @Override
    public long size()
    {
        return size;
    }

    @Override
    public Object fileKey()
    {
        return null;
    }
}
