package convert;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Parser {
    public static void parseTorrent(DataInputStream torrentFile, Info info) throws IOException {
        try {
            torrentFile.skipBytes(11);
            long length = getInteger(torrentFile);

            info.setFileLength(length);

            torrentFile.skipBytes(7);
            int n = (int) getNumber(torrentFile);
            byte[] name = new byte[n];
            torrentFile.read(name);
            info.setFilename(new String(name));

            torrentFile.skipBytes(15);System.out.println((char) torrentFile.read());
            n = (int) getInteger(torrentFile);
            info.setPieceLength(n);

            torrentFile.skipBytes(16);
            int piecesCount  = info.getPiecesCount();
            byte[] hash = new byte[piecesCount * 20];
            for(int i = 0; i < piecesCount; ++i) {
                torrentFile.read(hash, i*20, 20);
            }
            info.setPiecesHash(hash);
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
                md.update(hash);
                info.setHandshakeHash(md.digest());
            }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        catch(IOException e) {
            throw new IOException("Error");
        }
    }

    private static long getInteger(InputStream is) throws IOException {
        int leadingByte = is.read();
        if (leadingByte != 'i') throw new IOException("Error");

        int c;
        long number = 0;
        while (isDigit(c = is.read())) {
            number = number * 10 + (c - '0');
        }
        if(c != 'e') throw new IOException("Error");

        return number;
    }

    private static long getNumber(InputStream is) throws IOException {
        long number = 0;
        int c;

        while (isDigit(c = is.read())) {
            number = number * 10 + (c - '0');
        }

        if(c != ':') throw new IOException("Error");

        return number;
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

}
