import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Filter{  
static String helpMessage = """
        Usage: filter [OPTION]... [FILE]...
        Filters the contents of files
        
        Options:
            -p [prefix]\tset prefix for output file names
            -o [dir]\tset output directory
            -a\tappend to the file (default rewrites files)
            -A\trewrite files
            -s\tshort stats (default)
            -f\tfull stats
            -b\tignore blank lines
            -B\tkeep blank lines
            -q\tdon't show stats
            --help\tdisplay this help and exit""";
static ArrayList<String> fileNames = new ArrayList<String>();
static String prefix = ""; //  -p
static String outputPath = ""; // -o
static boolean shouldAppend = false; // -a / -A
static boolean shortStats = true; // -s / -f
static boolean ignoreSpaces = false; // -b / -B
static boolean quiet = false; // -q

static BigInteger minInt = null;
static BigInteger maxInt = null;
static BigDecimal sumInt = BigDecimal.ZERO;

static double minFloat = Double.POSITIVE_INFINITY;
static double maxFloat = Double.NEGATIVE_INFINITY;
static double sumFloat = 0;

static int shortestString = Integer.MAX_VALUE;
static int longestString = 0;

static int integersCount = 0;
static int stringsCount = 0;
static int floatsCount = 0;

static BufferedWriter intWriter, floatWriter, stringWriter;
public static BufferedWriter getWriter(String type) throws IOException {
    if (type.equals("int")) {
        //just like a singleton i guess
        if (intWriter == null) intWriter = createWriter("integers.txt");
        return intWriter;
    }
    if (type.equals("float")) {
        if (floatWriter == null) floatWriter = createWriter("floats.txt");
        return floatWriter;
    }
    if (stringWriter == null) stringWriter = createWriter("strings.txt");
    return stringWriter;
}   

public static BufferedWriter createWriter(String fileName) throws IOException {
    try{
        if (!outputPath.isEmpty()) {
            Path dir = Paths.get(outputPath);
            if (!Files.exists(dir)) {
                System.err.println("filter: " + outputPath + ": No such directory");
                System.exit(1);
            }
        }
        Path path = Paths.get(outputPath, prefix + fileName);
        return Files.newBufferedWriter(path, StandardOpenOption.CREATE, shouldAppend ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
    }
    catch(AccessDeniedException exception){
        System.err.println("filter: " + outputPath + ": Permission denied");
        System.exit(1);
        return null;
    }
}

public static void showHelp(){
    System.out.println(helpMessage);
    System.exit(0);
}

public static void parseArgs(String args[]){
    for(int i = 0; i < args.length; i++){
        String arg = args[i];
        if (arg.equals("--help")){
            showHelp();
            return;
        }
        if(arg.startsWith("-") && arg.length() > 1){
            for(int j = 1; j < arg.length(); j++){
                switch (arg.charAt(j)) {
                    case 'p':
                        if (i + 1 < args.length && !args[i+1].startsWith("-")){
                            prefix = args[++i];
                            j = arg.length();
                        }
                        else {
                            System.err.println("filter: option -p requires an argument");
                            System.exit(1);
                        }
                    break;
                    case 'o':
                        if (i + 1 < args.length && !args[i+1].startsWith("-")){
                            outputPath = args[++i];
                            j = arg.length();
                        }
                        else{
                            System.err.println("filter: option -o requires an argument");
                            System.exit(1);
                        }
                    break;
                    case 'a': shouldAppend = true; break;
                    case 'A': shouldAppend = false; break;
                    case 's': shortStats = true; break;
                    case 'f': shortStats = false; break;
                    case 'b': ignoreSpaces = true; break;
                    case 'B': ignoreSpaces = false; break;
                    case 'q': quiet = true; break;
                    default: 
                        System.err.println("filter: usage: filter [OPTION]... [FILE]..."); 
                        System.exit(1);
                }
            }
        }
        else{
            fileNames.add(args[i]);
        }
    }
}

public static void readInput(){
    if (fileNames.isEmpty()) processFile(new InputStreamReader(System.in), "stdin");
    else{
        for(String fileName : fileNames){
            try {processFile(new FileReader(fileName), fileName);}
            catch(IOException exception){
                System.err.println("filter: " +  fileName + ": No such file or directory");
            }
        }
    }
}

public static void processFile(Reader reader, String fileName){
    try (BufferedReader br = new BufferedReader(reader)) {
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            if (ignoreSpaces && currentLine.isBlank()) continue;
            if (currentLine.matches("^-?\\d+$")){
                integersCount++;
                getWriter("int").write(currentLine+"\n");
                if(shortStats) continue;

                BigInteger currentInt = new BigInteger(currentLine);
                if (maxInt == null || currentInt.compareTo(maxInt) > 0) maxInt = currentInt;
                if (minInt == null || currentInt.compareTo(minInt) < 0) minInt = currentInt;
                sumInt = sumInt.add(new BigDecimal(currentInt));
            }
            else if (currentLine.matches("^-?\\d*\\.?\\d+([eE][-+]?\\d+)?$")) {
                floatsCount++;
                getWriter("float").write(currentLine+"\n");
                if (shortStats) continue;

                double currentFloat = Double.parseDouble(currentLine);
                if (currentFloat > maxFloat) maxFloat = currentFloat;
                if (currentFloat < minFloat) minFloat = currentFloat;
                sumFloat += currentFloat;
            }
            else{
                stringsCount++;
                getWriter("string").write(currentLine+"\n");
                if(shortStats) continue;

                if(currentLine.length() > longestString) longestString = currentLine.length();
                if(currentLine.length() < shortestString) shortestString = currentLine.length();
            }
        }
    }
    catch(FileNotFoundException exception){
        System.err.println("filter: " +  fileName + ": No such file or directory");
    }
    catch(AccessDeniedException exception){
        System.err.println("filter: " + fileName + ": Permission denied");
    }
    catch(IOException exception){
        System.err.println("filter: " + exception.getMessage());
    }
}

public static void writeStats(){
    System.out.println("Stats:\n\tintegers:\t" + integersCount + "\n\tfloats:\t" + floatsCount + "\n\tstrings:\t" + stringsCount);
    if (!shortStats){
        if (integersCount > 0) System.out.println("Integers:\n\tmin:\t"+minInt+"\n\tmax:\t"+maxInt+"\n\tsum:\t"+sumInt+"\n\tavg:\t"+ sumInt.divide(new BigDecimal(integersCount), 10, RoundingMode.HALF_UP));
        if (floatsCount > 0) System.out.println("Floats:\n\tmin:\t"+minFloat+"\n\tmax:\t"+maxFloat+"\n\tsum:\t"+sumFloat+"\n\tavg:\t"+sumFloat/floatsCount);
        if (stringsCount > 0) System.out.println("Strings:\n\tshortest string length:\t"+shortestString+"\n\tlongest string length:\t"+longestString);
    }
}

public static void main (String args[]){
    parseArgs(args);
    readInput();
    try {
        if (intWriter != null) intWriter.close();
        if (floatWriter != null) floatWriter.close();
        if (stringWriter != null) stringWriter.close();
    } 
    catch (IOException exception) {
        System.err.println(exception.getMessage());
    }
    if(!quiet) writeStats();
}
}