package interections;

import reactions.Pair;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Writer implements Runnable {
    private RandomAccessFile file;
    private int pieceLength;
    private boolean stop;
    private Thread thread;

    private final BlockingQueue<Pair<Integer, byte[]>> mustWriteQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Integer> readyWriteQueue = new LinkedBlockingQueue<>();

    public void initiate(String path, int pieceLength) throws IOException {
        this.pieceLength = pieceLength;
        File tmp = new File(path);
        file = new RandomAccessFile(tmp, "rw");
    }

    public void start() {
        thread = new Thread(this, "Writer");
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                Pair<Integer, byte[]> pieceInfo = mustWriteQueue.take();
                write(pieceInfo.first, pieceInfo.second);
            }
        }
        catch(InterruptedException | IOException ignored) {}
    }

    public void write(int idx, byte[] piece) throws InterruptedException, IOException {
            file.seek((long) idx * pieceLength);
            file.write(piece);
            readyWriteQueue.put(idx);
    }

    public void stop() throws IOException {
        stop = true;
        thread.interrupt();
        if(file != null) file.close();
    }

    public BlockingQueue<Pair<Integer, byte[]>> getMustWriteQueue() {
        return mustWriteQueue;
    }

    public BlockingQueue<Integer> getReadyWriteQueue() {
        return readyWriteQueue;
    }
}
