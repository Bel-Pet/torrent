package convert;

public class PeerBehaviour {
    private String isAction;
    private String pathToFile;
    private String pathToTorrent;

    public void setAction(String download) {
        isAction = download;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    public void setPathToTorrent(String pathToTorrent) {
        this.pathToTorrent = pathToTorrent;
    }

    public String getAction() {
        return isAction;
    }

    public String getPathToTorrent() {
        return pathToTorrent;
    }

    public String getPathToFile() {
        return pathToFile;
    }
}
