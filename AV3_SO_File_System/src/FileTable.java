import java.util.*;
import java.io.*;

/*
 * Classe que mantém o registro dos arquivos abertos.
 * Um array de bytes Bitmap diz onde existe um arquivo aberto.
 * Um fileDescriptor array fdArray tem FD objetos.
 * Cada objeto FD é uma instância da classe FileDescriptor que guarda as informações
 * de um arquivo específico.
 */

class FileTable {
    public static final int MAX_FILES = 21; 
    public int bitmap[];
    public FileDescriptor[] fdArray;
    
    public FileTable(){
	fdArray = new FileDescriptor[MAX_FILES];
	bitmap = new int[MAX_FILES];
	for(int i = 0; i < MAX_FILES; i++){
	    bitmap[i] = 0;
	}
    }
    
    // Return o index do file table
    public int allocate(){
	for(int i = 0; i < MAX_FILES; i++){
	    if(bitmap[i] == 0)
		return i;
	}
	System.out.println("ERRO: File table está cheia.");
	return -1; 
    }
    
    public int add(Inode inode, int inumber, int fd){
	if(bitmap[fd] != 0)
	    return -1;
	bitmap[fd] = 1;
	fdArray[fd] = new FileDescriptor(inode, inumber);
	return 0;  
    }
    

    public void free(int fd){
	bitmap[fd] = 0;
    }

    public boolean isValid(int fd){
	if (fd < 0 || fd >= MAX_FILES) {
	    System.out.println("ERRO: File descriptor inválida (deve ser 0 <= fd <= "+ MAX_FILES + ") : " + fd);
	    return false;
	}
	else if (bitmap[fd] > 0){
	    return true;
	}
	else{
	    return false;
	}
    }

    public Inode getInode(int fd){
	if(bitmap[fd] == 0){
	    return null;
	}
	else{
	    return fdArray[fd].getInode();
	}
    }

    public int getInumber(int fd){
	if(bitmap[fd] == 0){
	    return 0;   
	}
	else{
	    return fdArray[fd].getInumber();
	}
    }

    public int getSeekPointer(int fd){
	if(bitmap[fd] == 0){
	    return 0;  
	}
	else{
	    return fdArray[fd].getSeekPointer();
	}
    }

    public int setSeekPointer(int fd, int newPointer) {
	if(bitmap[fd] == 0){
	    return 0;  
	}
	else{
	    fdArray[fd].setSeekPointer(newPointer);
	    return 1;  
	}
    }

    public int setFileSize(int fd, int newFileSize){
	if(bitmap[fd] == 0){
	    return 0; 
	}
	else{
	    fdArray[fd].setFileSize(newFileSize);
	    return 1;
	}
    }

    public int getFDfromInumber(int inumber){       
	for(int i = 0; i < MAX_FILES; i++){
	    if(bitmap[i] == 1){
		if (fdArray[i].getInumber() == inumber){
		    return i;
		}
	    }	    
	}   
	return -1;
    }
}