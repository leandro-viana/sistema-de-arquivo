class Inode {
	// Tamanho do inode em bytes
	public final static int SIZE = 64;	

	int flags;
	int owner;
	int fileSize;
	int pointer[] = new int[13];

	public String toString() {
		String s = "[Flags: " + flags
		+ "  Size: " + fileSize + "  ";
		for (int i = 0; i < 13; i++) 
			s += "|" + pointer[i];
		return s + "]";
	}

	public int getPointer(int i) {
		return pointer[i];
	}

	public void setPointer(int i) {
		this.pointer[i] = pointer[i];
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}


	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
}