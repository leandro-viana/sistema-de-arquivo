import java.io.EOFException;
import java.io.IOException;

class JavaFileSystem implements FileSystem {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    Disk javaDisk = new Disk();
    FileTable javaFileTable = new FileTable();
    IndirectBlock masterFreeList = new IndirectBlock();
    InodeBlock masterIblock = new InodeBlock();

    JavaFileSystem() {

        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0, sBlock);

        if(sBlock.iSize > 0) {
            javaDisk.read(masterFreeListBlock(), masterFreeList);
            javaDisk.read(1, masterIblock);

            InodeBlock iBlock = new InodeBlock();
            javaDisk.read(1, iBlock);

            for(int i = 0; i < sBlock.iSize; i++) {
                javaDisk.read(1 + i, iBlock);
                System.out.println(iBlock.toString());

                for (int j = 0; j < 8; j++) {
                    if (iBlock.node[j].flags != 0) {
                        javaFileTable.add(iBlock.node[j], j + 1, j);
                    }
                }
            }
        }else{System.out.println("Sistema de arquivo novo, porfavor format o disco");}

    }
    
    /*
     * Inicializa o disco no estado representando por um arquivo vazio.
	 * Preenche o superblock, marcando todos os inodes como "unused", e
	 * linka todos os blocos na lista livre.
     */

    public int formatDisk(int size, int iSize) {

        SuperBlock sBlock = new SuperBlock();
        sBlock.size = size;
        sBlock.iSize = iSize;
        sBlock.freeList = iSize + 1;

        javaDisk.write(0, sBlock);

        InodeBlock iBlock;
        for(int i = 1; i <= iSize; i++){
            iBlock = new InodeBlock();

            for(int j = 0; j < 8; j++ ){
                iBlock.node[j].flags = 0;
            }

            javaDisk.write(i, iBlock);

            javaDisk.read(i, iBlock);

        }


        IndirectBlock freeBlock = new IndirectBlock();
        int freeListBlockNumber = iSize + 1;
        int currentBlock = iSize + 1;

        for(int i = 0 ; i < 1 + size / 128; i ++){

            if(i > 0) {
                if(i == 1){
                    freeListBlockNumber = allocateBlock();
                    masterFreeList.pointer[0] = freeListBlockNumber;
                    javaDisk.write(masterFreeListBlock(), masterFreeList);
                }else {
                    freeListBlockNumber = allocateBlock();
                    freeBlock.pointer[0] = freeListBlockNumber;
                    javaDisk.write(freeListBlockNumber - 1, freeBlock);
                }

            }

            freeBlock = new IndirectBlock();

            for(int j = 1; j < javaDisk.POINTERS_PER_BLOCK & currentBlock <= size; j++){
                freeBlock.pointer[j] = currentBlock;
                currentBlock++;
            }



            javaDisk.write(freeListBlockNumber, freeBlock);

            if(i == 0){
                javaDisk.read(freeListBlockNumber, masterFreeList);
                allocateBlock();
            }
        }

        return 0;
    }

    //Fecha todos os arquivos e desliga o VHDD 

    public int shutdown() {

        for(int i = 0; i < javaFileTable.MAX_FILES; i ++){
            close(i);
        }

        javaDisk.stop();

        System.exit(1);

        return 0;
    } 

    // Cria um novo arquivo vazio e retorna o file descriptor.
    public int create() {

        int iNumber = allocateInode();

        int fd = javaFileTable.allocate();
        Inode inode = new Inode();

        javaFileTable.add(inode, iNumber, fd);

        javaFileTable.getInode(fd).flags = 1;

        return fd;
    } 

    // Retorna o inumber de um arquivo aberto.
    public int inumber(int fd) {
        return javaFileTable.fdArray[fd].getInumber();
    }

    // Abre um arquivo que existe pelo seu inumber.
    public int open(int iNumber) {

        if(iNumber == 0){
            System.out.println("Entre um número do iNumber maior que 0.");
            return -1;
        }

        Inode inode = findInode(iNumber);

        if(inode.flags == 0){
            System.out.println("Arquivo não existe.");
        }

        int fd = javaFileTable.allocate();

        if(javaFileTable.add(inode, iNumber, fd) == 0){
            javaFileTable.fdArray[fd].setSeekPointer(0);
            return iNumber;
        }


        System.out.println("Error abrindo o arquivo.");
        return -1;
    } 
    
    /*
     * Lê o buffer.length bytes de um arquivo aberto indicado pelo fd,
     * iniciando no determinado seek pointer e atualizando o seek pointer.
     * Retorna o número de bytes lidos que pode ser menor que o buffer.length
     * se o seek pointer estiver perto do fim do arquivo.
     * Retorna 0 se o seek pointer é maior ou igual ao tamanho do arquivo.
     */
    
    public int read(int fd, byte[] buffer)  {

        if( javaFileTable.fdArray[fd].getSeekPointer() >= javaFileTable.fdArray[fd].getInode().fileSize){
            System.out.println(" Seek pointer está no fim do arquivo, nada mais para ler.");
            return 0;
        }

        if(javaFileTable.isValid(fd) == false){
            System.out.println("Arquivo não abriu.");
            return 0;
        }

        int currentBlock = javaFileTable.fdArray[fd].getSeekPointer() / javaDisk.BLOCK_SIZE;

        int bytesRead = 0;
        int seekPtr = javaFileTable.getSeekPointer(fd);
        byte[] readBuffer = new byte[Disk.BLOCK_SIZE];

        seekPtr = seekPtr - (Disk.BLOCK_SIZE * currentBlock);
        int seekPtrController = seekPtr;

        javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock],readBuffer);


        while (bytesRead < buffer.length & seekPtr < Disk.BLOCK_SIZE) {
            buffer[bytesRead] = readBuffer[seekPtr];
            seekPtr++;
            bytesRead++;
        }

        int counter = Disk.BLOCK_SIZE;

        if(bytesRead < buffer.length){

            while(bytesRead < buffer.length & seekPtr < javaFileTable.fdArray[fd].getInode().fileSize){
                if(counter == Disk.BLOCK_SIZE){
                    currentBlock++;
                    javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock], readBuffer);
                    System.out.println(currentBlock);

                    counter = 0;
                }

                buffer[bytesRead] = readBuffer[counter];
                seekPtr++;
                bytesRead++;
                counter++;
            }
        }

        seek(fd,seekPtr - seekPtrController, 1);
        return bytesRead;
    } 

    /*
     * Modifica o buffer.length bytes do buffer para onde o ponteiro
     * seek está apontando, andando o ponteiro seek na quantidade de
     * bytes escrita ao final do processo.
     */

    public int write(int fd, byte[] buffer) {

        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0, sBlock);

        if(javaFileTable.isValid(fd) == false){
            System.out.println("Arquivo não abriu.");
            return 0;
        }

        int bytesWritten  = 0;
        int currentBlock = javaFileTable.fdArray[fd].getSeekPointer() / javaDisk.BLOCK_SIZE;

        int seekPtr = javaFileTable.getSeekPointer(fd);
        byte[] writeBuffer = new byte[Disk.BLOCK_SIZE];

        if(javaFileTable.getInode(fd).pointer[currentBlock] != 0) {
            javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);
        }else{
            javaFileTable.getInode(fd).pointer[currentBlock] = allocateBlock();
            javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);
        }

        while(bytesWritten < buffer.length & seekPtr < Disk.BLOCK_SIZE){
            writeBuffer[seekPtr] = buffer[bytesWritten];
            seekPtr++;
            bytesWritten++;
        }

        javaDisk.write(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);

        int counter = Disk.BLOCK_SIZE;
        if(bytesWritten < buffer.length){
            while(bytesWritten < buffer.length){
                if(counter == Disk.BLOCK_SIZE){
                    javaDisk.write(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);

                    currentBlock++;

                    for(int j = 0; j < javaFileTable.getInode(fd).pointer.length; j++) {
                        if(javaFileTable.getInode(fd).pointer[j] == 0) {
                            javaFileTable.getInode(fd).pointer[j] = allocateBlock();
                            int check = javaFileTable.getInode(fd).pointer[j];

                            if( check == sBlock.size){
                                System.out.println("Fim dos blocos disponíveis.");
                                javaFileTable.setFileSize(fd, javaFileTable.getInode(fd).fileSize + buffer.length);
                                writeInode(javaFileTable.getInumber(fd), javaFileTable.getInode(fd));

                                seek(fd,seekPtr, 0);

                                return bytesWritten;
                            }

                            javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);

                            break;
                        }
                    }
                    javaDisk.read(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);

                    counter = 0;
                }

                writeBuffer[counter] = buffer[bytesWritten];
                seekPtr++;
                bytesWritten++;
                counter++;

            }
        }

        javaDisk.write(javaFileTable.getInode(fd).pointer[currentBlock], writeBuffer);



        javaFileTable.setFileSize(fd, javaFileTable.getInode(fd).fileSize + buffer.length);
        writeInode(javaFileTable.getInumber(fd), javaFileTable.getInode(fd));

        seek(fd,seekPtr, 0);
        return bytesWritten;
    } 

    /*
     * Atualiza o seek pointer e retorna o novo valor do seek pointer.
     * No caso 0 o offset é relativo ao começo do arquivo.
     * No caso 1 o offset represeta o ponteiro do seek corrente.
     * No caso 2 o offset está no final do arquivo.
     * Caso o resultado seja negativo o ponteiro não é modificado e return -1
     * Em contrapartida o valor retornado é o novo ponteiro epresentando a distância 
	 * em bytes do início do arquivo
     */

    public int seek(int fd, int offset, int whence) {

        int seekPointer = javaFileTable.fdArray[fd].getSeekPointer();
        int file_size = javaFileTable.fdArray[fd].getInode().fileSize;

        switch (whence) {
            case SEEK_SET:
                seekPointer = offset;
                break;
            case SEEK_CUR:
                seekPointer += offset;
                break;
            case SEEK_END:
                seekPointer = file_size + offset;
                break;
        }

        javaFileTable.setSeekPointer(fd,seekPointer);


        return seekPointer;
    } 

    // Escreve o inode de volta ao disco e libera a tabela de livre entrada
    public int close(int fd) {

        if(javaFileTable.isValid(fd)){

            writeInode(javaFileTable.getInumber(fd), javaFileTable.getInode(fd));

            javaFileTable.free(fd);

            return 0;
        }
        return -1;
    } 

    // Deleta o arquivo de dado inumber, liberando todos seus blocos.
    public int delete(int iNumber) {

        if(iNumber == 0){
            System.out.println("Entre um número maior que 0 para o iNumber.");
            return -1;
        }

        open(iNumber);

        Inode inode = javaFileTable.fdArray[javaFileTable.getFDfromInumber(iNumber)].getInode();

        inode.flags = 0;
        inode.fileSize = 0;

        for(int i = 0; i < 10; i++){
            freeBlock(inode.pointer[i]);
            inode.pointer[i] = 0;
        }

        inode = new Inode();

        writeInode(iNumber , javaFileTable.getInode(javaFileTable.getFDfromInumber(iNumber)));

        return 0;
    } 

    // Aloca um bloco da lista livre
    
    public int allocateBlock() {
        int blockTracker = 0;
        int currentBlock = 0;
        IndirectBlock freeList = masterFreeList;

        while(true){

            for(int i = 1; i < freeList.pointer.length; i ++){
                if(freeList.pointer[i] != 0){
                    blockTracker = freeList.pointer[i];
                    freeList.pointer[i] = 0;
                    break;
                }
            }

            if(freeList.pointer[0] != 0 & blockTracker == 0){
                if(freeList.pointer[0] != 0) {
                    javaDisk.read(freeList.pointer[0], freeList);
                    currentBlock = freeList.pointer[0];
                    continue;
                }else{
                    System.out.println("Fim dos blocos livres.");
                    return -1;
                }
            }
            break;
        }

        if(currentBlock == 0){
            javaDisk.write(masterFreeListBlock(), freeList);
            javaDisk.read(masterFreeListBlock(), masterFreeList);
        }else{
            javaDisk.write(currentBlock, freeList);
        }

        return blockTracker;
    }

    // Libera um bloco da lista livre
    
    public void freeBlock(int x) {

        IndirectBlock freeList = masterFreeList;

        int number = x;
        int listNumber = x / freeList.pointer.length;

        for(int i = 0; i < listNumber;  i++){
            number -= freeList.pointer.length;
        }

        if(x > 0) {
            javaDisk.read(javaFileTable.getInode(listNumber).pointer[0], freeList);
        }

        freeList.pointer[number] = x;


        javaDisk.write(listNumber, freeList);
    }

    // Encontra o nũmero do bloco dado o inode e byte offset
    public int findBlock(Inode inode, int offset, boolean fillHole){ 

        int finder = (offset / Disk.BLOCK_SIZE);



        if(inode.pointer[finder] == 0){
            if(fillHole == true){

                int newBlock = allocateBlock();

                return newBlock;

            }else if(fillHole == false){
                return 0;
            }
        }

        return inode.pointer[finder];
    }

    // Alloca unused
    public int allocateInode(){

        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0,sBlock);

        Inode inode;
        int counter = 1;
        InodeBlock iBlock = new InodeBlock();
        for(int i = 1; i <= sBlock.iSize; i++){

            iBlock = new InodeBlock();
            javaDisk.read(i, iBlock);

            for(int j = 0; j < 8; j++ ){
                if(iBlock.node[j].flags == 0){
                    iBlock.node[j].flags = 1;

                    javaDisk.write(i, iBlock);

                    return counter;
                }
                counter++;
            }
        }

        System.out.println("Error alocando o Inode");
        return -1;
    }

    //Escreve Inode
    
    public void writeInode(int iNumber, Inode inode){

        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0, sBlock);

        InodeBlock iBlock = new InodeBlock();
        int counter = 1;
        int blockNumber = 0;

        int check = 0;
        for(int i = 1; i <= sBlock.iSize; i ++){
            iBlock = new InodeBlock();
            javaDisk.read(i, iBlock);

            for(int j = 0; j < 8; j++){
                if(counter == iNumber ){

                    iBlock.node[j] = inode;
                    blockNumber = i;
                    check = 1;
                    break;
                }
                counter++;
            }

            if(check > 0) {
                break;
            }
        }

        javaDisk.write(blockNumber, iBlock);
    }

    // Encontra o INode pelo Inumber
    
    public Inode findInode(int iNumber){
        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0, sBlock);

        InodeBlock iBlock = new InodeBlock();

        int counter = 0;
        for(int i = 1; i <= sBlock.iSize; i++){
            javaDisk.read(i, iBlock);

            for(int j = 0; j < 8; j++){
                if(counter == (iNumber - 1)){
                    return iBlock.node[j];
                }
                counter++;
            }
        }

        return null;
    }

    // Return bloco do masterFreeList
    
    public int masterFreeListBlock(){

        SuperBlock sBlock = new SuperBlock();
        javaDisk.read(0, sBlock);

        return sBlock.iSize +1;
    }

}