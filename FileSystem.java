
package gg.filesystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import sun.util.locale.StringTokenIterator;

/**
 *
 * @author GarimaGupta
 */
public class FileSystem {

    ArrayList<Object> memoryBlock = new ArrayList<>(100);
    Directory root;
    int index=0,availableBlocks = 100, writePointer = 0;
    FileNode openedFile;
    boolean isRead = false, isWrite = false, isSeek = false;
    Dir openedFileDir;
    public FileSystem() {
        root = new Directory();
        root.back = 0;
        root.forward = 0;
    }
    public static void main(String args[]){
        FileSystem fileSystem = new FileSystem();
        fileSystem.init();
    }
    public void init(){
        memoryBlock.add(root);
        --availableBlocks;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Command");
        String  command,name;
        char type, mode ;
        do{
            command = scanner.nextLine();
            String[] commands = command.split(" ");
            String toUpperString = commands[0].toUpperCase();
            switch(toUpperString){
                case "CREATE":
                    if(commands.length!=3){
                        System.out.println("Incorrect Parameters");
                    }else{
                        type = commands[1].charAt(0);
                        name = commands[2];
                        if(isFileNameValid(name.split("/"))){
                            createFile(type, name, root);
                        }else{
                            System.out.println("Wrong File name. Filename should be less than 9 characters");
                        }

                    }
                    break;
                case "OPEN":
                    boolean wrongMode = false;
                    if(commands.length!=3){
                        System.out.println("Incorrect Parameters");
                    }else{
                        mode = commands[1].toUpperCase().charAt(0);
                        switch(mode){
                            case 'I':isRead = true;
                                     isSeek = true;
                                     isWrite = false;
                                break;
                            case 'O':isRead = false;
                                     isSeek = false;
                                     isWrite = true;
                                break;
                            case 'U':isRead = true;
                                     isSeek = true;
                                     isWrite = true;
                                break;
                            default:wrongMode = true;
                        }
                        if(wrongMode){
                            System.out.println("Wrong Mode2");
                        }else{
                            name = commands[2];
                            if((openedFile = openFile(mode, name.split("/"),root))!=null){
                                System.out.println("File Opened");
                            }else{
                                System.out.println("Specified File doesn't exist");
                            }
                        }

                    }

                    break;
                case "CLOSE":
                    if(openedFile!=null){
                        openedFile = null;
                        openedFileDir = null;
                        System.out.println("File Closed");
                    }else{
                        System.out.println("No File Opened");
                    }
                    break;
                case "DELETE":
                    if(commands.length!=2){
                        System.out.println("Incorrect Parameters");
                    }else{
                        deleteFile(commands[1].split("/"),root);
                    }
                    break;
                case "READ":
                    if(commands.length !=2){
                        System.out.println("Illegar arguments");
                    }else{
                        int size = Integer.parseInt(commands[1]);
                        if(isRead){
                            readFile(openedFile,size);
                        }else{
                            System.out.println("No Read Permissions");
                        }
                    }

                    break;
                case "WRITE":
                    if(isWrite){
                        if(openedFile != null){
                            int bytesToWrite = Integer.parseInt(commands[1]);
                            String data = command.substring(commands[1].length()+commands[2].length()+2);
                            writeFile(openedFile, data.getBytes(),bytesToWrite,writePointer,0);
                        }else{
                            System.out.println("No file opened.");
                        }
                    }else{
                        System.out.println("No write permission");
                    }


                    break;
                case "SEEK":
                    seekFile(Integer.parseInt(commands[1]), Integer.parseInt(commands[2]));
                    break;
                case "EXIT":
                    exitProgram();
                case "DATA":
                    loadData();
                    break;
                case "DISPLAY":
                    displayHeirarchy(root);
                    break;
                default:
                    System.out.println("Illegal command");
            }
        }while(true);
    }
    public void createFile(char type, String name, Directory root){
        Directory currDir = root;
        Directory temp;
        boolean isNameUsed = false;
        String[] names = name.split("/");

        for(int i=0;i<names.length-1;i++){
            temp = getDirectory(names[i],currDir,'d');
            if(temp == null){
                currDir = createResource(names[i], 'd', currDir);
            }else{
                currDir = temp;
            }
        }

        isNameUsed = doesFileExist(currDir, names[names.length-1], type);
        if(!isNameUsed){
        currDir = createResource(names[names.length-1], type, currDir);
        }else{
            System.out.println("Name Already used");
            deleteFile(names, root);
            createFile(type, name, root);
        }
    }
    public FileNode openFile(char mode, String[] names,Directory currDir){
        Directory temp;
        boolean isFileExisting = false;
        for(int i=0;i<names.length-1;i++){
            temp = getDirectory(names[i],currDir,'d');
            if(temp == null){
                return null;
            }else{
                currDir = temp;
            }
        }
        return getFile(currDir, names[names.length-1]);
    }
    public void closeFile(){
        openedFile = null;
        openedFileDir = null;
        isRead = false;
        isWrite = false;
        isSeek = false;
    }
    public void deleteFile(String[] names, Directory currDir){
        Directory temp;
        boolean isResourceExists = true;
        for(int i=0;i<names.length-1;i++){
            temp = getDirectory(names[i],currDir,'d');
            if(temp == null){
                isResourceExists = false;
                break;
            }else{
                currDir = temp;
            }
        }
        if(isResourceExists){
            Dir tempDir;
            int link;
            char type;
            String name = names[names.length-1];
            tempDir = getAndDeleteDir(name, currDir);
            if(tempDir == null){
                System.out.println("Directory or File doesn't exist");
            }else{
                if(tempDir.type == 'u'){
                    deleteFile((FileNode)memoryBlock.get(tempDir.link));
                    memoryBlock.remove(tempDir.link);
                    memoryBlock.add(tempDir.link,null);
                    ++availableBlocks;
                }else{
                deleteDirectoryResource((Directory)memoryBlock.get(tempDir.link));
                memoryBlock.remove(tempDir.link);
                memoryBlock.add(tempDir.link,null);
                ++availableBlocks;
                }
            }


            System.out.println("Deleted");
        }else{
            System.out.println("Directory or File doesn't exist");
        }
    }
    public void readFile(FileNode file, int size){
        if(size>504){
            System.out.print(new String(file.data));
            if(openedFile.forward>0){
            readFile((FileNode)memoryBlock.get(openedFile.forward),size-504);
            }else{
            System.out.println();
            }
        }else{
            byte[] output = new byte[size];
            for(int i=0;i<size;i++){
                output[i] = file.data[i];
            }
            System.out.println(new String(output));
        }

    }
    public void writeFile(FileNode file, byte[] data,int size,int pointer,int dataPointer){
        int remainingSpace = 504-pointer;
        int j = dataPointer;
        if(remainingSpace > size){
            for(int i=pointer;i<pointer+size;i++,j++){
                if(j<data.length){
                    file.data[i] = data[j];
                }else{
                    file.data[i] = ' ';
                }

            }
            openedFileDir.size = new Integer(pointer+size).shortValue();
            System.out.println("Write Succesful");

        }else{
            for(int i=pointer;i<504;i++,j++){
                if(j<data.length){
                    file.data[i] = data[j];
                }else{
                    file.data[i] = ' ';
                }
            }
            FileNode fn = new FileNode();
            if(isMemoryFree()){
                if(memoryBlock.indexOf(null)>0){
                    int indexOfNull = memoryBlock.indexOf(null);
                    memoryBlock.remove(indexOfNull);
                    memoryBlock.add(indexOfNull,fn);
                    --availableBlocks;
                }else{
                    memoryBlock.add(fn);
                    --availableBlocks;
                }
            }
            file.forward = memoryBlock.indexOf(fn);
            fn.back = memoryBlock.indexOf(file);
            writeFile(fn, data, size-remainingSpace,0,j);
        }
    }
    public void seekFile(int base , int offset){
        switch(base){
            case 1:
                writePointer = openedFileDir.size+offset;
                break;
            case -1:
                writePointer = offset;
                break;
            case 0:
                writePointer = openedFileDir.size+offset;
                break;
            default:
                System.out.println("Wrong base");
        }
    }
    public void exitProgram(){
        System.exit(0);
    }
    public Directory createResource(String resourceName, char type, Directory currDir){
        if(isMemoryFree()){
            if(type == 'u'){
                FileNode fn = new FileNode();
                Dir dir = new Dir();
                dir.type = type;
                dir.name = resourceName.getBytes();
                if(memoryBlock.indexOf(null)>0){
                    int indexOfNull = memoryBlock.indexOf(null);
                    memoryBlock.remove(indexOfNull);
                    memoryBlock.add(indexOfNull,fn);
                    --availableBlocks;
                }else{
                    memoryBlock.add(fn);
                    --availableBlocks;
                }
                dir.link = memoryBlock.indexOf(fn);
                if(currDir.dirs.size()<32){
                   currDir.dirs.add(dir);
                }else if(currDir.forward>0){
                    currDir = (Directory)memoryBlock.get(currDir.forward);
                    currDir.dirs.add(dir);
                }else{
                   Directory d = new Directory();
                   if(memoryBlock.indexOf(null)>0){
                        int indexOfNull = memoryBlock.indexOf(null);
                        memoryBlock.remove(indexOfNull);
                        memoryBlock.add(indexOfNull,fn);
                        --availableBlocks;
                    }else{
                        memoryBlock.add(fn);
                        --availableBlocks;
                    }
                   int memoryIndex = memoryBlock.indexOf(currDir);
                   currDir.forward = memoryBlock.indexOf(d);
                   currDir = d;
                   currDir.back = memoryIndex;
                   currDir.dirs.add(dir);
                }

                System.out.println("File Created");
                return currDir;
            }else{
                Directory d = new Directory();
                Dir dir = new Dir();
                dir.type = type;
                dir.name = resourceName.getBytes();
                if(memoryBlock.indexOf(null)>0){
                    int indexOfNull = memoryBlock.indexOf(null);
                    memoryBlock.remove(indexOfNull);
                    memoryBlock.add(indexOfNull,d);
                    --availableBlocks;
                }else{
                    memoryBlock.add(d);
                    --availableBlocks;
                }
                dir.link = memoryBlock.indexOf(d);
                if(currDir.dirs.size()<32){
                   currDir.dirs.add(dir);
                }else if(currDir.forward>0){
                    currDir = (Directory)memoryBlock.get(currDir.forward);
                    currDir.dirs.add(dir);
                }else{
                   Directory dd = new Directory();
                   if(memoryBlock.indexOf(null)>0){
                        int indexOfNull = memoryBlock.indexOf(null);
                        memoryBlock.remove(indexOfNull);
                        memoryBlock.add(indexOfNull,dd);
                        --availableBlocks;
                    }else{
                        memoryBlock.add(dd);
                        --availableBlocks;
                    }
                   currDir.forward = dd.back;
                   currDir = dd;
                   currDir.dirs.add(dir);
                }
                System.out.println("Directory Created");
                return d;
            }
        }else{
            System.out.println("Insufficient Memory, Free Space by deleting files or Direcctories");
        }
        return currDir;
    }
    public Directory getDirectory(String name, Directory temp, char type){
        int length = temp.dirs.size();
        for(int i=0;i<length;i++){
            Dir d = temp.dirs.get(i);
            if(new String(d.name).equals(name) && d.type == type){
                return (Directory)memoryBlock.get(d.link);
            }
        }
        if(temp.forward>0){
            return getDirectory(name, (Directory)memoryBlock.get(temp.forward), type);
        }

       return null;
    }
    public void displayHeirarchy(Directory root){
        System.out.println("Root-d");
        printData(root,1);
    }
    public void printData(Directory temp,int space){
        int length = temp.dirs.size();
        int spaceL = space;
        Dir d;
        for(int i=0;i<length;i++){
            d  = temp.dirs.get(i);
            if(d.type == 'u'){
                while(spaceL>0){
                    System.out.print("-");
                    spaceL--;
                }
                spaceL = space;
                System.out.println(new String(d.name)+"-u");
            }else{
                while(spaceL>0){
                    System.out.print("-");
                    spaceL--;
                }
                spaceL = space;
                System.out.println(new String(d.name)+"-d");
                printData((Directory)memoryBlock.get(d.link),space+1);
            }
        }
        if(temp.forward>0){
            printData((Directory)memoryBlock.get(temp.forward),space);
        }
    }
    public boolean isFileNameValid(String[] name){
        for(String val:name){
            if(val.length()>9){
                return false;
            }
        }
        return true;
    }
    public boolean isMemoryFree();
        if(availableBlocks>0){
            return true;
        }
        return false;
    }
    public boolean doesFileExist(Directory currDir, String name, char type){
        for(int i=0;i<currDir.dirs.size();i++){
            Dir d = currDir.dirs.get(i);
            if(new String(d.name).equals(name)&& d.type == type){
                return true;
            }
        }
        if(currDir.forward>0){
            return doesFileExist((Directory)memoryBlock.get(currDir.forward), name, type);
        }
        return false;
    }
    public FileNode getFile(Directory currDir, String name){
        for(int i=0;i<currDir.dirs.size();i++){
            Dir d = currDir.dirs.get(i);
            if(new String(d.name).equals(name)&& d.type == 'u'){
                openedFileDir = d;
                return (FileNode)memoryBlock.get(d.link);
            }
        }
        if(currDir.forward>0){
            return getFile((Directory)memoryBlock.get(currDir.forward),name);
        }
        return null;
    }

   public Dir getAndDeleteDir(String name, Directory currDir){
       Dir tempDir;
       for(int i =0;i<currDir.dirs.size();i++){
            tempDir = currDir.dirs.get(i);
            if(new String(tempDir.name).equals(name)){
                currDir.dirs.remove(tempDir);
                return tempDir;
            }else{

            }
        }
        if(currDir.forward>0){
            return getAndDeleteDir(name, (Directory)memoryBlock.get(currDir.forward));
        }
       return null;
    }
   public void deleteDirectoryResource(Directory currDir){
       Dir temp;
       for(int i =0;i<currDir.dirs.size();i++){
           temp = currDir.dirs.get(i);
           if(temp.type == 'u'){
               deleteFile((FileNode)memoryBlock.get(temp.link));
               memoryBlock.remove(temp.link);
               memoryBlock.add(temp.link,null);
               ++availableBlocks;
           }else{
               deleteDirectoryResource((Directory)memoryBlock.get(temp.link));
               memoryBlock.remove(temp.link);
               memoryBlock.add(temp.link,null);
               ++availableBlocks;
           }
       }
       if(currDir.forward>0){
           deleteDirectoryResource((Directory)memoryBlock.get(currDir.forward));
       }
   }
   public void deleteFile(FileNode fn){
       if(fn.forward>0){
           deleteFile((FileNode)memoryBlock.get(fn.forward));
           memoryBlock.remove(fn.forward);
           memoryBlock.add(fn.forward,null);
           ++availableBlocks;
      }
   }
   //&*************************Preload DATA************************
   public void loadData(){
       createFile('u', "file1", root);
       createFile('u', "file2", root);
       createFile('u', "dir1/file1", root);
       createFile('u', "dir1/file2", root);
       createFile('u', "dir1/dir2/file1", root);
       createFile('u', "dir1/dir2/file2", root);
       createFile('d', "dir1/dir3", root);
       createFile('d', "dir4", root);
       createFile('u', "dir5/file3", root);
       createFile('u', "file4", root);
       createFile('u', "file5", root);
        isRead = true;
        isSeek = true;
        isWrite = true;
       openedFile = openFile('U', new String("dir1/file1").split("/"), root);
       writeFile(openedFile, new String("Hello this data being loaded from init").getBytes(),200,openedFileDir.size,0);
       closeFile();
       openedFile = openFile('U', new String("file1").split("/"), root);
       writeFile(openedFile, new String("According to reports, at the dinner, Crown Prince Dipendra had been drinking heavily, "
               + "had smoked large quantities of hashish and \"misbehaved\" with a guest which resulted in his father King Birendra"
               + " telling Dipendra, who was his oldest son, to leave the party. Crown Prince Dipendra was escorted to his room by "
               + "his brother Prince Nirajan and cousin Prince Paras. [2]").getBytes(),2000,openedFileDir.size,0);
       closeFile(); }}
