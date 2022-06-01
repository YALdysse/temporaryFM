package org.yaldysse.fm.dialogs.delete;

import java.nio.file.Path;

/***/
public interface DeleteProgress
{
    public void appearDeleteProgress(final int aDeletedFilesNumber, final Path currentPath,
                                     final boolean completed, final boolean interrupted);
}
