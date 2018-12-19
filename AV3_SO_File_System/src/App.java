/** 
 * Trabalho de sistema de arquivos da disciplina Sistemas Operacionais da UNIFOR
 * Professor Bruno Lopes
 * Referente a AV3, realizado por @LeandroViana e @LucasMendes
 * Foi utilizado como fonte de pesquisa além do livro Silberschatz, Galvin and Gagne
 * também vídeo aulas do Youtube e aula presencial da disciplina.
 * Classe com o main e comandos básicos para testar o projeto.
 * Utilizar o comando "help" na linha de comando para eventuais testes.
 * 
 **/

import java.io.*;
import java.util.*;

class App 
{
    private static FileSystem fs;
    private static Hashtable vars = new Hashtable();
    public static void main(String [] args){
        if (args.length > 1) System.err.println ("Usage: TestFS [filename]");
        boolean fromFile = (args.length==1);

        // Cria o arquivo de teste
        fs = new JavaFileSystem();
        BufferedReader data = null;
        if (fromFile) {
            try {
                data = new BufferedReader (new FileReader(new File(args[0])));
            }
            catch (FileNotFoundException e) {
                System.err.println("Error: File " + args[0] + " not found.");
                System.exit(1);
            }
        }
        else data = new BufferedReader (new InputStreamReader(System.in));
        for (;;) {
            try {
                if (!fromFile) {
                    System.out.print("--> ");
                    System.out.flush();
                }
                String line = data.readLine();
                //System.out.println(line);
                if (line == null) System.exit(0);  
                line = line.trim();                
                if (line.length() == 0) {          
                    System.out.println();
                    continue;
                }
                if (line.startsWith("//")) {
                    if (fromFile)
                        System.out.println(line);
                    continue;
                }
                if (line.startsWith("/*")) continue;
                if (fromFile) System.out.println("--> " + line);

                String target = null;
                int equals = line.indexOf('=');
                if (equals > 0) {
                    target = line.substring(0,equals).trim();
                    line = line.substring(equals+1).trim();
                }

                StringTokenizer cmds = new StringTokenizer (line);
                String cmd = cmds.nextToken();

                int result = 0;
                if (cmd.equalsIgnoreCase("formatDisk")
                        || cmd.equalsIgnoreCase("format"))
                {
                    int arg1 = nextValue(cmds);
                    int arg2 = nextValue(cmds);
                    result = fs.formatDisk(arg1,arg2);
                }
                else if (cmd.equalsIgnoreCase("shutdown")) {
                    result = fs.shutdown();
                }
                else if (cmd.equalsIgnoreCase("create")) {
                    result = fs.create();
                }
                else if (cmd.equalsIgnoreCase("open")) {
                    result = fs.open(nextValue(cmds));
                } 
                else if (cmd.equalsIgnoreCase("inumber")) {
                    result = fs.inumber(nextValue(cmds));
                } 
                else if (cmd.equalsIgnoreCase("read")) {
                    int arg1 = nextValue(cmds);
                    int arg2 = nextValue(cmds);
                    result = readTest(arg1,arg2);
                } 
                else if (cmd.equalsIgnoreCase("write")) {
                    int arg1 = nextValue(cmds);
                    String arg2 = cmds.nextToken();
                    int arg3 = nextValue(cmds);
                    result = writeTest(arg1,arg2,arg3);
                } 
                else if (cmd.equalsIgnoreCase("seek")) {
                    int arg1 = nextValue(cmds);
                    int arg2 = nextValue(cmds);
                    int arg3 = nextValue(cmds);
                    result = fs.seek(arg1,arg2,arg3);
                } 
                else if (cmd.equalsIgnoreCase("close")) {
                    result = fs.close(nextValue(cmds));
                } 
                else if (cmd.equalsIgnoreCase("delete")) {
                    result = fs.delete(nextValue(cmds));
                } 
                else if (cmd.equalsIgnoreCase("quit")) {
                    System.exit(0);
                } 
                else if (cmd.equalsIgnoreCase("vars")) {
                    for (Enumeration e = vars.keys(); e.hasMoreElements(); ) {
                        Object key = e.nextElement();
                        Object val = vars.get(key);
                        System.out.println("\t" + key + " = " + val);
                    }
                    continue;
                }
                else if (cmd.equalsIgnoreCase("help")) {
                    help();
                    continue;
                } 
                else {
                    System.out.println("unknown command");
                    continue;
                }

                if (target == null)
                    System.out.println("    Result is " + result);
                else {
                    vars.put(target,new Integer(result));
                    System.out.println("    " + target + " = " + result);
                }
            }

            catch (NumberFormatException e) {
                System.out.println("Incorrect argument type");
            }

            catch (NoSuchElementException e) {
                System.out.println("Incorrect number of elements");
            }
            catch (IOException e) {
                System.err.println(e);
            }
        }
    } 

    static private int nextValue(StringTokenizer cmds)
    {
        String arg = cmds.nextToken();
        Object val = vars.get(arg);
        return
            (val == null) ?  Integer.parseInt(arg) : ((Integer)val).intValue();
    }

    private static void help() {
        System.out.println ("\tformatDisk size iSize");
        System.out.println ("\tshutdown");
        System.out.println ("\tcreate");
        System.out.println ("\topen inum");
        System.out.println ("\tinumber fd");
        System.out.println ("\tread fd size");
        System.out.println ("\twrite fd pattern size");
        System.out.println ("\tseek fd offset whence");
        System.out.println ("\tclose fd");
        System.out.println ("\tdelete inum");
        System.out.println ("\tquit");
        System.out.println ("\tvars");
        System.out.println ("\thelp");
    }

    private static int readTest(int fd, int size) {
        byte[] buffer = new byte[size];
        int length;

        for (int i = 0; i < size; i++)
            buffer[i] = (byte) '*';
        length = fs.read(fd, buffer);
        for (int i = 0; i < length; i++) 
            showchar(buffer[i]);
        if (length != -1) System.out.println();
        return length;
    }

 
    private static int writeTest (int fd, String str, int size) {
        byte[] buffer = new byte[size];

        for (int i = 0; i < buffer.length; i++) 
            buffer[i] = (byte)str.charAt(i % str.length());

        return fs.write(fd, buffer);
    }
    

    private static void showchar(byte b) {
        if (b < 0) {
            System.out.print("M-");
            b += 0x80;                // Make b positive
        }
        if (b >= ' ' && b <= '~') {
            System.out.print((char)b);
            return;
        }
        switch (b) {
            case '\0': System.out.print("\\0"); return;
            case '\n': System.out.print("\\n"); return;
            case '\r': System.out.print("\\r"); return;
            case '\b': System.out.print("\\b"); return;
            case 0x7f: System.out.print("\\?"); return;
            default:   System.out.print("^" + (char)(b + '@')); return;
        }
    }
}
