interface FileSystem {
	 /**
	 * Inicializa o disco no estado representando por um arquivo vazio.
	 * Preenche o superblock, marcando todos os inodes como "unused", e
	 * linka todos os blocos na lista livre.
     *
     * @param size o tamanho dos blocos do disco
     * @param iSize o número de inode blocos
     * @return 0 se funciona -1 se não
     */
    public int formatDisk(int size, int iSize); 
    
    /**
     * Inicializa o disco "vazio". Preenche com o Superbloco
     * realiza a ligação de todos os blocos de dados na lista de 
     * espaço vazio.
     * Size = Quantidade de blocos do disco
     * iSize = Número de blocos de inode
     * 
     */
    public int shutdown(); 
    
    /**
     * Fecha todos os arquivos abertos e desliga o VHDD
     */
    public int create(); 
    
    /**
     * Return the inumber of an open file
     * Cria um arquivo vazio e o abre.
     * 
     * @param fd o file descriptor do arquivo
     * @return o número do inoe do arquivo
     */
    public int inumber(int fd); 
    
    /**
     * Abre um arquivo preexistente identificado pelo inumber
     * 
     * @param inum o número do inode do arquivo
     * @return o file descriptor number
     */
    public int open(int iNumber); 
    
    /**
     * Lê o buffer.length bytes de um arquivo aberto indicado pelo fd,
     * iniciando no determinado seek pointer e atualizando o seek pointer.
     * Retorna o número de bytes lidos que pode ser menor que o buffer.length
     * se o seek pointer estiver perto do fim do arquivo.
     * Retorna 0 se o seek pointer é maior ou igual ao tamanho do arquivo.
     *
     * @param fd o file descriptor
     * @param buffer a pre inicializado leitura do buffer
     * @return número de bytes lidos ou -1 se error
     */
    public int read(int fd, byte[] buffer); 
    
    /**
     * Realiza a leitura para um buffer.length bytes iniciando do ponteiro
     * do seek atual
     * Transfer buffer.length bytes from the buffer to the file, starting
     * at the current seek pointer, and add buffer.length to the seek pointer.
     *
     * @param fd o file descriptor
     * @param buffer o buffer para leitura
     * @return número de bytes lidos na operação, -1 se error
     */
    public int write(int fd, byte[] buffer); 
    
    /**
     * Modifica o buffer.length bytes do buffer para onde o ponteiro
     * seek está apontando, andando o ponteiro seek na quantidade de
     * bytes escrita ao final do processo.
     * Retorna o novo valor do seek pointer, se o novo valor for negativo,
     * não se altera e retorna -1.
     * 
     * @param fd file descriptor
     * @return o novo valor do seek pointer, ou -1 se error
     */
    public int seek(int fd, int offset, int whence); 
    
    /**
     * Escreve o inode de volta no disco e libera o file table
     *
     * @param fd o file descriptor
     * @return 0 se funciona e -1 se não
     */
    public int close(int fd); 
    
    /**
     * Deleta o arquivo de dado inumber, liberando todos os seus blocos.
     *
     * @param inum o número do inode
     * @return 0 se funciona e -1 se não
     */
    public int delete(int iNumber); 
}
