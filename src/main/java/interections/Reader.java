package interections;

import reactions.Pair;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Reader implements Runnable {

    private RandomAccessFile file;
    private int pieceLength;
    private int piecesCount;
    private byte[] piece;
    private byte[] lastPiece;
    private boolean stop;
    private Thread thread;

    private final BlockingQueue<Trio<Integer, byte[], SocketChannel>> readyQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Pair<Integer, SocketChannel>> mustReadQueue = new LinkedBlockingQueue<>();


    public void initiate(String path, long fileLength, int pieceLength, int piecesCount) throws FileNotFoundException {
        this.pieceLength = pieceLength;
        this.piecesCount = piecesCount;
        piece = new byte[pieceLength];
        int lastPieceLength;
        if (fileLength % pieceLength != 0) {
            lastPieceLength = (int)(fileLength % pieceLength);
        } else {
            lastPieceLength = pieceLength;
        }
        lastPiece = new byte[lastPieceLength];
        File tmp = new File(path);
        file = new RandomAccessFile(tmp, "r");
    }

    public void start() {
        stop = false;
        thread = new Thread(this, "Reader");
        thread.start();
    }

    public void stop() throws IOException {
            stop = true;
            thread.interrupt();
            file.close();
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                read(mustReadQueue.take());
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    public void read(Pair<Integer, SocketChannel> data) {
        Integer idx = data.first;
        SocketChannel socket = data.second;
        try {
            file.seek((long) idx * pieceLength);

            if (idx != piecesCount - 1) {
                file.read(piece);
                try {
                    readyQueue.put(new Trio<>(idx, Arrays.copyOf(piece, piece.length), socket));
                } catch (InterruptedException ignored){}
            } else {
                file.read(lastPiece);
                try {
                    readyQueue.put(new Trio<>(idx, Arrays.copyOf(lastPiece, lastPiece.length), socket));
                } catch (InterruptedException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public BlockingQueue<Pair<Integer, SocketChannel>> getMustReadQueue() {
        return mustReadQueue;
    }

    public BlockingQueue<Trio<Integer, byte[], SocketChannel>> getReadyReadQueue() {
        return readyQueue;
    }
}
