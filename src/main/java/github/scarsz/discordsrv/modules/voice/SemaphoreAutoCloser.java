package github.scarsz.discordsrv.modules.voice;

import java.io.Closeable;
import java.util.concurrent.Semaphore;

public class SemaphoreAutoCloser implements Closeable {
    private final Semaphore sem;

    SemaphoreAutoCloser(Semaphore s) {
        this.sem = s;
    }

    @Override
    public void close() {
        this.sem.release();
    }
}
