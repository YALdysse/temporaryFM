package org.yaldysse.fm;


import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;

public class FolderWatcher implements Runnable
{
    private Path targetPath;
    private FM_GUI fm_gui;
    private WatchService watchService;

    public FolderWatcher(final Path newTargetPath, final FM_GUI gui)
    {
        targetPath = newTargetPath;
        fm_gui = gui;
    }

    @Override
    public void run()
    {
        try
        {
            watchService = targetPath.getFileSystem().newWatchService();
            targetPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

            WatchKey targetKey = null;

            while(true)
            {
                targetKey = watchService.take();
                for (WatchEvent<?> event : targetKey.pollEvents())
                {
                    WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
                   Path eventPath = pathWatchEvent.context();
                    Platform.runLater(()->
                    {
                        fm_gui.directoryWatchOccured(eventPath, pathWatchEvent.kind());
                    });
                }
                targetKey.reset();
            }
        }
        catch(IOException ioException)
        {
            ioException.printStackTrace();
        }
        catch(InterruptedException interruptedException)
        {
            interruptedException.printStackTrace();
        }
        catch(ClosedWatchServiceException closedWatchServiceException)
        {
            System.out.println("WatchService остановлен.");
        }
        System.out.println("Поток завершается");
    }

    public void stopWatchService() throws IOException
    {
        watchService.close();
    }


}
