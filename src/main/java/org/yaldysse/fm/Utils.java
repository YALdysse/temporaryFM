package org.yaldysse.fm;

import java.nio.file.Path;

public class Utils
{
    /***/
    public static String getExtension(final Path targetPath)
    {
        String extension = "";
        if (targetPath.getFileName() != null)
        {
            int extensionStartIndex = targetPath.getFileName().toString().lastIndexOf('.');

            if (extensionStartIndex > 0)
            {
                extension = targetPath.getFileName().toString().substring(extensionStartIndex + 1);
                extension = extension.toUpperCase();
            }
        }
        else
        {
            System.out.println("Нельзя определить расширения, т.к. не удалось" +
                    "получить имя.");
            return "";
        }
        return extension;
    }
}
