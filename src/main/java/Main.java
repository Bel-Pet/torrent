import interections.TorrentClient;

import java.io.IOException;

public class Main {

    public static void main(String[] args)
    {
        TorrentClient client = new TorrentClient();
        try {
            client.execute();
        } catch (IOException e) {
            System.out.println("Some problem happened. For more information look in a log file. Torrent client is terminated.");
        }
    }
}
