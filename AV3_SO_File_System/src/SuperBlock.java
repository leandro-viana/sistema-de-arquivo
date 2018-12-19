class SuperBlock {
	// Número de blocks no sistema de arquivos.
	int size;
	//public int size;
	// Número de blocos de índice no sistema de arquivos. 
	int iSize;
	//public int iSize;
	// Primeiro bloco da lista de blocos livres
	int freeList;
	//public int freeList;

	public String toString () {
		return
			"SUPERBLOCK:\n"
			+ "Size: " + size
			+ "  Isize: " + iSize
			+ "  freeList: " + freeList;
	}
}