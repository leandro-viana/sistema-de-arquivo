import java.util.*;
import java.io.*;

class Disk {
    // Tamanho em bytes de cada bloco
    public final static int BLOCK_SIZE = 512;
    // O número de blocos no sistema
    public final static int NUM_BLOCKS = 2000;

    public final static int POINTERS_PER_BLOCK = (BLOCK_SIZE/4);

    // Número de reads e writes no file system
    private int readCount = 0;
	private int writeCount = 0;

	// Arquivo que representa o disco simulado
	private File fileName;
	private RandomAccessFile disk;
	public Disk() {
		try {
			fileName = new File("VHDD");
			disk = new RandomAccessFile(fileName, "rw");
		}
		catch (IOException e) {
			System.err.println ("Impossível iniciar o disco.");
			System.exit(1);
		}
	}


	private void seek(int blocknum) throws IOException {
		if (blocknum < 0 || blocknum >= NUM_BLOCKS) 
			throw new RuntimeException ("Tentando ler o bloco " +
					       blocknum + " , não conseguiu localizar.");
		disk.seek((long)(blocknum * BLOCK_SIZE));
	}
	

	/**
 	 * Lê o conteúdo do bloco do disco especificado por blockNume
 	 * para o buffer.
	 */
	public void read(int blocknum, byte[] buffer) {
		if (buffer.length != BLOCK_SIZE) 
			throw new RuntimeException(
				"Read: bad buffer size " + buffer.length);
		try {
			seek(blocknum);
			disk.read(buffer);
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	} 

	public void read(int blocknum, SuperBlock block) {
		try {
			seek(blocknum);
			block.size = disk.readInt();
			block.iSize = disk.readInt();
			block.freeList = disk.readInt();
		}
		catch (EOFException e) {
			if (blocknum != 0) {
				System.out.println("aqui");
				System.err.println(e);
				System.exit(1);
			}
			block.size = block.iSize = block.freeList = 0;
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	} 

	public void read(int blocknum, InodeBlock block) {
		try {
			seek(blocknum);
			for (int i=0; i<block.node.length; i++) {
				block.node[i].flags = disk.readInt();
				block.node[i].owner = disk.readInt();
				//block.node[i].size = disk.readInt();
				block.node[i].fileSize = disk.readInt();
				for (int j=0; j<13; j++)
					block.node[i].pointer[j] = disk.readInt();
			}
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	} 
		
	public void read(int blocknum, IndirectBlock block) {
		try {
			seek(blocknum);
			for (int i=0; i<block.pointer.length; i++)
				block.pointer[i] = disk.readInt();
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		readCount++;
	} 
	
	/** 
	 * Dá o Write do buffer pelo blockNum 	
	 */
	public void write(int blocknum, byte[] buffer) {
		if (buffer.length != BLOCK_SIZE) 
			throw new RuntimeException(
				"Write: bad buffer size " + buffer.length);
		try {
			seek(blocknum);
			disk.write(buffer);
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	}

	public void write(int blocknum, SuperBlock block) {
		try {
			seek(blocknum);
			disk.writeInt(block.size);
			disk.writeInt(block.iSize);
			disk.writeInt(block.freeList);
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	} 

	public void write(int blocknum, InodeBlock block) {

		try {
			seek(blocknum);
			for (int i=0; i<block.node.length; i++) {
				disk.writeInt(block.node[i].flags);
				disk.writeInt(block.node[i].owner);
				//disk.writeInt(block.node[i].size);
				disk.writeInt(block.node[i].fileSize);
				for (int j=0; j<13; j++)
					disk.writeInt(block.node[i].pointer[j]);
			}
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}

		writeCount++;
	} 
		
	public void write(int blocknum, IndirectBlock block) {
		try {
			seek(blocknum);
			for (int i=0; i<block.pointer.length; i++)
				disk.writeInt(block.pointer[i]);
		}
		catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		writeCount++;
	} 
	
	/** 
	 * Para o disco e gera as estatisticas para o relatório
	 */
	
	public void stop(boolean removeFile) {
		//System.out.println (toString());
		System.out.println (generateStats());
		if (removeFile)
			fileName.delete();
	}

	public void stop() {
		stop(true);
	}

	/**
	 * Mostra o relatório das operações de leitura e escrita
	 * que foram realizadas.
	 */
	//public String toString() {
	public String generateStats() {
		return ("DISK: Read feitas: " + readCount + " Write feitas: " + 
			writeCount);
	}
}
