package org.yaldysse.fm.nio_file_attribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Set;

/**
 * Хранит все аттрибуты, которые могут быть считаны при помощи класса FileAttributeView
 * и его производных. Умеет обрабатывать такие виды: <br>{@link BasicFileAttributeView},
 * <br>{@link PosixFileAttributeView}, <br>{@link DosFileAttributeView},
 * <br>{@link FileOwnerAttributeView}.
 */
public class NioFileAttributes
{
    //------------------ basic/dos/posix = <super>BasicFileAttributes, DosFileAttributes, PosixFileAttributes
    private FileTime creationTime;
    private FileTime lastModifiedTime;
    private FileTime lastAccessTime;
    private Long size;
    private Boolean regularFile;
    private Boolean directory;
    private Boolean symbolicLink;
    private Boolean other;
    private Object fileKey;

    //------------------ dos = DosFileAttributes
    private Boolean readOnly;
    private Boolean hidden;
    private Boolean system;
    private Boolean archive;
    //------------------ owner/posix/acl = <super>FileOwnerAttributeView,
    // PosixFileAttributes, AclFileAttributeView
    private UserPrincipal owner;
    //------------------ posix = PosixFileAttributes
    private GroupPrincipal group;
    private Set<PosixFilePermission> posixPermissions;

    private Path path;
    private Set<String> supportedAttributeViews;

    private NioFileAttributes(final Path targetPath)
    {
        path = targetPath;

        readOnly = null;
        hidden = null;
        system = null;
        archive = null;

        regularFile = null;
        directory = null;
        symbolicLink = null;
        other = null;
    }

    public static NioFileAttributes get(final Path targetPath)
    {
        NioFileAttributes attributes = new NioFileAttributes(targetPath);
        attributes.readSupported();
        attributes.printSupportedAttributeViews();
        return attributes;
    }

    /**
     * Определяет допустимый тип аттрибутов и читает их.
     */
    private void readSupported()
    {
        supportedAttributeViews = path.getFileSystem().supportedFileAttributeViews();

        /**При помощи posix можно получить почти все необходимые аттрибуты, кроме
         * dos.*/
        if (supportedAttributeViews.contains("posix"))
        {
            readAllPosixAttributes();
            readDosAttributes(false);
        }
        else if (supportedAttributeViews.contains("dos"))
        {
            /**В таком случае аттрибут группы получить скорее всего не удастся.*/
            readDosAttributes(true);
            owner = determineOwner();
        }
        else if (supportedAttributeViews.contains("basic"))
        {
            readAllBasicAttributes();
            owner = determineOwner();
        }
    }

    /**
     * Читает все аттрибуты типа basic.
     */
    private void readAllBasicAttributes()
    {
        try
        {
            BasicFileAttributes basic = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            creationTime = basic.creationTime();
            lastModifiedTime = basic.lastModifiedTime();
            lastAccessTime = basic.lastAccessTime();

            directory = basic.isDirectory();
            regularFile = basic.isRegularFile();
            symbolicLink = basic.isSymbolicLink();
            other = basic.isOther();
            size = basic.size();

            fileKey = basic.fileKey();
        }
        catch (IOException ioException)
        {
//            System.out.println(ioException);
        }
    }

    /**
     * Читает все аттрибуты типа posix.
     */
    private void readAllPosixAttributes()
    {
        try
        {
            PosixFileAttributes posix = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            owner = posix.owner();
            group = posix.group();
            posixPermissions = posix.permissions();

            creationTime = posix.creationTime();
            lastModifiedTime = posix.lastModifiedTime();
            lastAccessTime = posix.lastAccessTime();

            directory = posix.isDirectory();
            regularFile = posix.isRegularFile();
            symbolicLink = posix.isSymbolicLink();
            other = posix.isOther();
            size = posix.size();

            fileKey = posix.fileKey();
        }
        catch (IOException ioException)
        {
//            System.out.println(ioException);
        }
    }


    /**
     * Читает аттрибуты типа dos полностью (те, что определены в basic) либо
     * только особые для dos.
     *
     * @param readAll Читать ли все аттрибуты, доступные для типа dos
     *                (наследуемые от basic).
     *                Если false - то будут считаны только archive, readOnly,
     *                system и hidden.
     */
    private void readDosAttributes(boolean readAll)
    {
        try
        {
            DosFileAttributes dos = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            if (readAll)
            {
                creationTime = dos.creationTime();
                lastModifiedTime = dos.lastModifiedTime();
                lastAccessTime = dos.lastAccessTime();

                regularFile = dos.isRegularFile();
                directory = dos.isDirectory();
                symbolicLink = dos.isSymbolicLink();
                other = dos.isOther();
                fileKey = dos.fileKey();

                size = dos.size();
            }

            archive = dos.isArchive();
            readOnly = dos.isReadOnly();
            hidden = dos.isHidden();
            system = dos.isSystem();
        }
        catch (IOException ioException)
        {
//            System.out.println(ioException);
        }
    }


    /**
     * Читает аттрибуты, характерные для интерфейса {@link BasicFileAttributes} и его
     * наследников.
     */
    private void readAllBasicSuperAttributes(Class<BasicFileAttributes> basicClass)
    {
        try
        {
            BasicFileAttributes basic = Files.readAttributes(path, basicClass, LinkOption.NOFOLLOW_LINKS);
            creationTime = basic.creationTime();
            lastModifiedTime = basic.lastModifiedTime();
            lastAccessTime = basic.lastAccessTime();
            size = basic.size();
            directory = basic.isDirectory();
            regularFile = basic.isRegularFile();
            symbolicLink = basic.isSymbolicLink();
            other = basic.isOther();
            fileKey = basic.fileKey();
        }
        catch (IOException ioException)
        {
            //System.out.println(ioException);
        }
    }

    /**
     * Определяет владельца файла и возвращает его объект. Если определить не
     * удается, то будет возвращено null. Определяет благодаря типам аттрибутов
     * owner и acl.
     */
    private UserPrincipal determineOwner()
    {
        UserPrincipal tempOwner = null;

        try
        {
            FileOwnerAttributeView fileOwner = Files.getFileAttributeView(path, FileOwnerAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            tempOwner = fileOwner.getOwner();

            if (tempOwner != null)
            {
                return fileOwner.getOwner();
            }

            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

            tempOwner = acl.getOwner();
            if (tempOwner != null)
            {
                return acl.getOwner();
            }
        }
        catch (IOException ioException)
        {
            //System.out.println(ioException);
        }
        return null;
    }

    public Set<String> getSupportedAttributeViews()
    {
        return supportedAttributeViews;
    }

    public void printSupportedAttributeViews()
    {
        System.out.print("Supported AttributeViews: ");
        for (String s : supportedAttributeViews)
        {
            System.out.print(s + ", ");
        }
    }


    public static void main(String[] args)
    {
        Path path = Path.of("/home/yaroslav/IdeaProjects/PsevdoFM/PsevdoFM.iml");
        NioFileAttributes nio = NioFileAttributes.get(path);

        nio.printSupportedAttributeViews();
    }


    public FileTime getCreationTime()
    {
        return creationTime;
    }

    public FileTime getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    public FileTime getLastAccessTime()
    {
        return lastAccessTime;
    }

    public Long getSize()
    {
        return size;
    }

    public Boolean isRegularFile()
    {
        return regularFile;
    }

    public Boolean isDirectory()
    {
        return directory;
    }

    public Boolean isSymbolicLink()
    {
        return symbolicLink;
    }

    public Boolean isOther()
    {
        return other;
    }

    public Object getFileKey()
    {
        return fileKey;
    }

    public Boolean isReadOnly()
    {
        return readOnly;
    }

    public Boolean isHidden()
    {
        return hidden;
    }

    public Boolean isSystem()
    {
        return system;
    }

    public Boolean isArchive()
    {
        return archive;
    }

    public UserPrincipal getOwner()
    {
        return owner;
    }

    public GroupPrincipal getGroup()
    {
        return group;
    }

    public Set<PosixFilePermission> getPosixPermissions()
    {
        return posixPermissions;
    }

    public Path getPath()
    {
        return path;
    }
}
