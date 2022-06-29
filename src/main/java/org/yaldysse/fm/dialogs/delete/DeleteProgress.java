package org.yaldysse.fm.dialogs.delete;

import java.nio.file.Path;
import java.util.ArrayList;

/***/
public interface DeleteProgress
{
    public void appearDeleteProgress(final int aDeletedFilesNumber, final Path currentPath,
                                     final boolean completed, final boolean interrupted,
                                     final ArrayList<Path> accessDeniedErrorsPaths);

    default public void errorDetected(final Path targetPath, final String description)
    {

    }
}
