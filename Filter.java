import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Filter{  
    static ArrayList<String> text = new ArrayList<String>();
    static ArrayList<String> fileNames = new ArrayList<String>();
    static String prefix = ""; //  -p
    static String outputPath = ""; // -o
    static boolean shouldAppend = false; // -a
    static boolean shortStats = true; // -s / -f

    static BigInteger minInt = null;
    static BigInteger maxInt = null;
    static BigDecimal sumInt = BigDecimal.ZERO;

    static double minFloat = Double.MAX_VALUE;
    static double maxFloat = Double.MIN_VALUE;
    static double sumFloat = 0;

    static int shortestString = Integer.MAX_VALUE;
    static int longestString = Integer.MIN_VALUE;

    static int integersCount = 0;
    static int stringsCount = 0;
    static int floatsCount = 0;
    
    static StringBuilder integersBuilder = new StringBuilder();
    static StringBuilder floatsBuilder = new StringBuilder();
    static StringBuilder stringsBuilder = new StringBuilder();
    
    public static void showHelp(){
        System.out.println("Usage: filter [OPTION]... [FILE]...\nFilters the contents of files\n\nOptions:\n  -p [prefix]\tset prefix for output file names\n  -o [dir]\tset output directory\n  -a\tappend to the file (default rewrites files)\n  -s\tshort stats (default)\n  -f\tfull stats\n  --help\tdisplay this help and exit");
        System.exit(0);
    }

    public static void readInput(){
        if (fileNames.isEmpty()){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                text.addAll(reader.lines().collect(Collectors.toList()));
                reader.close();
            }
            catch(IOException exception){
                System.err.println(exception);
            }
        }
        else{
            for(String fileName : fileNames){
                try{
                    List<String> lines = Files.readAllLines(Path.of(fileName));
                    text.addAll(lines);
                }
                catch(NoSuchFileException exception){
                    System.err.println("filter: " +  fileName + ": No such file or directory");
                    System.exit(1);
                }
                catch(IOException exception){
                    System.err.println(exception);
                }
            }
        }
    }

    public static void parseArgs(String args[]){
        for(int i = 0; i < args.length; i++){
            String arg = args[i];
            if (arg.equals("--help")){
                showHelp();
                return;
            }
            if(arg.startsWith("-") && arg.length() > 1){
                switch (arg.charAt(1)) {
                    case 'p':
                        if (i + 1 < args.length) prefix = args[++i];
                        else {
                            System.err.println("filter: option -p requires an argument");
                            System.exit(1);
                        }
                    break;
                    case 'o':
                        if (i + 1 < args.length) outputPath = args[++i];
                        else{
                            System.err.println("filter: option -o requires an argument");
                            System.exit(1);
                        }
                    break;
                    case 'a': shouldAppend = true; break;
                    case 's': shortStats = true; break;
                    case 'f': shortStats = false; break;
                    default: 
                        System.err.println("filter: usage: filter [OPTION]... [FILE]..."); 
                        System.exit(1);
                }
            }
            else{
                fileNames.add(args[i]);
            }
        }
    }

    public static void writeOutput(){
        if(integersCount > 0){
            writeFile(Paths.get(outputPath, prefix+"integers.txt"), integersBuilder);
        }
        if(floatsCount > 0){
            writeFile(Paths.get(outputPath, prefix+"floats.txt"), floatsBuilder);
        }
        if(stringsCount > 0){
            writeFile(Paths.get(outputPath, prefix+"strings.txt"), stringsBuilder);
        }  
    }

    public static void writeFile(Path path, StringBuilder builder){
        try(FileWriter fr = new FileWriter(path.toFile(), shouldAppend);){
            fr.write(builder.toString());
        }
        catch(FileNotFoundException exception){
            System.err.println("filter: " +  path.getParent() + ": directory doesn't exist");
            System.exit(1);
        }
        catch(IOException exception){
            System.err.println(exception);
        }
    }

    public static void processLines(){
        for (int i = 0; i < text.size(); i++) {
            String currentLine = text.get(i);
            if (currentLine.matches("^-?\\d+$")){
                integersBuilder.append(currentLine).append("\n");
                integersCount++;
                if(shortStats) continue;

                BigInteger currentInt = new BigInteger(currentLine);
                if (maxInt == null || currentInt.compareTo(maxInt) > 0) maxInt = currentInt;
                if (minInt == null || currentInt.compareTo(minInt) < 0) minInt = currentInt;
                sumInt = sumInt.add(new BigDecimal(currentInt));
            }
            //^-?\\d+\\.\\d+$
            else if (currentLine.matches("^-?\\d*\\.?\\d+([eE][-+]?\\d+)?$")) {
                floatsBuilder.append(currentLine).append("\n");
                floatsCount++;
                if (shortStats) continue;
                double currentFloat = Double.parseDouble(currentLine);
                if (currentFloat > maxFloat) maxFloat = currentFloat;
                if (currentFloat < minFloat) minFloat = currentFloat;
                sumFloat += currentFloat;
            }
            else{
                stringsBuilder.append(currentLine).append("\n");
                stringsCount++;
                if(shortStats) continue;
                if(currentLine.length() > longestString) longestString = currentLine.length();
                if(currentLine.length() < shortestString) shortestString = currentLine.length();
            }
        }
    }

    public static void writeStats(){
        System.out.println("Stats:\n\t" + prefix+"integers.txt:\t" + integersCount + "\n\t"+prefix+"floats.txt:\t\t" + floatsCount + "\n\t"+prefix+"strings.txt:\t" + stringsCount);
        if (!shortStats){
            if (integersCount > 0) System.out.println("Integers:\nmin:\t"+minInt+"\nmax:\t"+maxInt+"\nsum:\t"+sumInt+"\navg:\t"+ sumInt.divide(new BigDecimal(integersCount), 10, RoundingMode.HALF_UP));
            if (floatsCount > 0) System.out.println("Floats:\nmin:\t"+minFloat+"\nmax:\t"+maxFloat+"\nsum:\t"+sumFloat+"\navg:\t"+sumFloat/floatsCount);
            if (stringsCount > 0) System.out.println("Strings:\nshortest string length:\t"+shortestString+"\nlongest string length:\t"+longestString);
        }
    }

    public static void main (String args[]){
        parseArgs(args);
        readInput();
        processLines();
        writeOutput();
        writeStats();
    }
}