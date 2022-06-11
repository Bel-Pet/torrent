package convert;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TorrentFile {

    private final File file;
    DataOutputStream torrentFile;

    private final long fileLength;
    private final int pieceLength = 256000;

    private TorrentFile(String pathToFile, String pathToTorrent) throws FileNotFoundException {
        file = new File(pathToFile);
        torrentFile = new DataOutputStream(new FileOutputStream(pathToTorrent));
        fileLength = file.length();
    }

    public static void create(String pathToFile, String pathToTorrent) throws IOException {
        TorrentFile create = new TorrentFile(pathToFile, pathToTorrent);
        create.create();
    }

    private void create() throws IOException {
        try {
            long fileLength = file.length();
            String filename = file.getName();

            torrentFile.writeBytes("d 7:length i" + fileLength + "e");
            torrentFile.writeBytes("5:name" + filename.length() + ":" + filename);
            torrentFile.writeBytes("12:piece length");
            torrentFile.writeBytes("i" + pieceLength + "e 7:pieces20:");

            writeHashPieces();

            torrentFile.close();
        }
        catch (Exception e) {
            throw new IOException("Can't create torrent file");
        }
    }

    private void writeHashPieces() throws NoSuchAlgorithmException, IOException {

        RandomAccessFile inFile = new RandomAccessFile(file, "r");
        byte[] piece = new byte[pieceLength];
        byte[] lastPiece;
        int piecesCount;

        if(fileLength % pieceLength == 0) {
            lastPiece = new byte[pieceLength];
            piecesCount = (int)fileLength/pieceLength;
        } else {
            lastPiece = new byte[(int)fileLength%pieceLength];
            piecesCount = (int)fileLength/pieceLength + 1;
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[][] hash = new byte[piecesCount][20];

        for(int i = 0; i < piecesCount - 1; ++i) {
            inFile.read(piece);
            md.update(piece);
            hash[i] = md.digest();
            torrentFile.write(hash[i]);
        }

        inFile.read(lastPiece);
        md.update(lastPiece);
        hash[piecesCount - 1] = md.digest();
        torrentFile.write(hash[piecesCount - 1]);

        inFile.close();
    }
}
