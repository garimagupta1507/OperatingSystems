package gg.filesystem;

/**
*
* @author GarimaGupta
*/

public class FileNode {
    int back;
    int forward;
    byte[] data = new byte[504];

    public FileNode() {
        this.back = -1;
        this.forward = -1;
    }

}
