package gg.filesystem;

import java.util.ArrayList;

/**
 *
 * @author GarimaGupta
 */
public class Directory {
    int back;
    int forward;
    int free;
    int filler;
    ArrayList<Dir> dirs = new ArrayList<>();

    public Directory() {
        this.back = -1;
        this.forward = -1;
        this.free = 0;
    }


}
class Dir {
    char type;
    byte[] name = new byte[9];
    int link;
    short size;

    public Dir() {
        this.link = -1;
        this.size = 0;
    }

}
