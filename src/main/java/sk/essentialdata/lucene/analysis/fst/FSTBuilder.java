package sk.essentialdata.lucene.analysis.fst;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

/**
 * Using SortedMap to ensure FST is created in sorted order.
 * @author miso
 * @date 4/29/14.
 */
public class FSTBuilder {
    private static String LEMMA_DELIMITER = "\t";
    private SortedMap<String, String> dict = new TreeMap<>();;
    private FST<CharsRef> fst;
    private Set<String> flags;

    public FSTBuilder() {
    }

    public static void main(String[] args) throws IOException {
        String inputDirPath = null;
        String inputFilePath = null;
        String outputFilePath = null;
        FSTBuilder builder = new FSTBuilder();
        builder.flags = new HashSet<>();
        int i = 0;
        for (String arg : args) {
          if (arg.startsWith("--")) {
            builder.flags.add(arg.substring(2));
          } else {
            switch (arg) {
              case "-d":
                inputDirPath = args[i+1];
              case "-f":
                inputFilePath = args[i+1];
              case "-o":
                outputFilePath = args[i+1];
            }
          }
          i++;
        }
        if (outputFilePath == null || (inputDirPath == null && inputFilePath == null)) {
            System.out.println("USAGE:");
            System.out.println("FSTBuilder -f <dictionary input file path> -o <FST output file path>");
            System.out.println("FSTBuilder -f <dictionary input file path> -o <FST output file path> --ascii");
            System.out.println("FSTBuilder -d <dictionary input dir path> -o <FST output file path>");
            System.out.println("FSTBuilder -d <dictionary input dir path> -o <FST output file path> --ascii");
            System.exit(1);
        }

        if(inputDirPath != null){
            System.out.println(String.format("Loading files from dir %s...", inputDirPath));
            builder.loadFromDir(inputDirPath);
        } else {
            System.out.println(String.format("Loading file %s", inputFilePath));
            builder.loadFromFile(inputFilePath);
        }
        System.out.println("Building FST...");
        System.out.println("(ASCII mode is " + (builder.flags.contains("ascii") ? "on" : "off") + ")");
        builder.buildFSTFromDict();
        System.out.println("Saving FST...");
        builder.save(outputFilePath);

        File file = new File(outputFilePath);
        FST<CharsRef> fst = FST.read(file.toPath(), CharSequenceOutputs.getSingleton());

         System.out.println("Sanity check: dimorphic word, words with asterisk(inflected only|lemma only|both)"); // sorry, the fifth word is the only word with two asterisks
        for (String s : Arrays.asList("najprudší", "najprudkejší", "neni", "chujovinami", "piči", "falšovanejšia")) {
            System.out.println(s + " was lemmatized as " + Util.get(fst, new BytesRef(s)));
        }
        for (String s : Arrays.asList("najprudsi", "najprudkejsi", "neni", "chujovinami", "pici")) {
            System.out.println(s + " was lemmatized as " + Util.get(fst, new BytesRef(s)));
        }
    }

    /**
     * Format of the file:
     * Each line consists of the following:
     * lemma(TAB)affixed(TAB)flags
     * The same lemma is usually on multiple lines,
     * for each affixed version.
     *
     * Example line:
     * prudký(TAB)najprudkejšiemu(TAB)AAms3z
     *
     * 4 exceptions:
     * prudký(TAB)najprudší(SPACE)najprudkejší(TAB)AAms1z
     * nebyť(TAB)*neni(TAB)VKesc-
     * *chujovina(TAB)chujovinami(TAB)SSfp7
     * *piča(TAB)*piči(TAB)SSfs2
     *
     * Fourth exception has only one occurence - no way to avoid it.
     *
     * We build the FST based on mapping (affixed -> lemma)
     *
     * @param pathname
     * @throws IOException
     */
    private void loadFromFile(String pathname) throws IOException {
        System.out.println("Loading from file " + pathname);
        File file = new File(pathname);
        Scanner scanner = new Scanner(file);
        int line = 0;
        while (scanner.hasNext()) {
            line++;
            if (line % 1000000 == 0) {
                System.out.println("Processing line " + line);
            }
            try {
                String[] parts = scanner.nextLine().split(LEMMA_DELIMITER);
                if (parts.length != 3) {
                    throw new IOException("Bad format of the input file " + pathname + ", line " + line + ": " + parts);
                }
                parts[0] = trimAsterisk(parts[0]);
                parts[1] = trimAsterisk(parts[1]);
                if (parts[1].contains(" ")) {
                    // We add both versions delimited by a space
                    // as follows from the exception 1.
                    String[] part1parts = parts[1].split(" ");
                    if (part1parts.length != 2) {
                        throw new IOException("Bad format of the input file " + pathname + ", line " + line + ": " + parts + ", " + part1parts);
                    }
                    addToDict(part1parts[0], parts[0]);
                    addToDict(part1parts[1], parts[0]);
                } else {
                    addToDict(parts[1], parts[0]);
                }

            } catch (RuntimeException e) {
                throw new RuntimeException("Line " + line + " of input file " + pathname, e);
            }
        }
        scanner.close();
    }

    private void loadFromDir(String dirPath) throws IOException {
        Files.list(Paths.get(dirPath)).forEach(path -> {
            try {
                loadFromFile(path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Ignoring duplicates and merging different outputs for the same input.
     * @param input
     * @param output
     */
    private void addToDict(String input, String output) {
        if (flags.contains("ascii")) {
            input = asciiFold(input);
            output = asciiFold(output);
//            System.out.println(input + "-" + output);
        }
        String oldOutput = dict.get(input);
        if (oldOutput == null) {
            dict.put(input, output);
        } else {
            for (String part : oldOutput.split("\\|")) {
                if (part.equals(output)) {
                    return;
                }
            }
            dict.put(input, oldOutput + "|" + output);
        }
    }

    private void buildFSTFromDict() throws IOException {
        CharSequenceOutputs charSequenceOutputs = CharSequenceOutputs.getSingleton();
        Builder<CharsRef> builder = new Builder<CharsRef>(FST.INPUT_TYPE.BYTE1, charSequenceOutputs);
        IntsRefBuilder intsRefBuilder = new IntsRefBuilder();
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            builder.add(Util.toIntsRef(new BytesRef(entry.getKey()), intsRefBuilder), new CharsRef(entry.getValue()));
        }
        fst = builder.finish();
    }

    public void save(String pathname) throws IOException {
        File file = new File(pathname);
        fst.save(file.toPath());
    }

    /**
     * If the first character is '*', return value will be without it.
     * @param string
     * @return
     */
    private String trimAsterisk(String string) {
        if (string.charAt(0) == '*') {
            return string.substring(1);
        }
        return string;
    }

    /**
     *
     * @param string
     * @return
     */
    private String asciiFold(String string) {
        char[] output = new char[4*string.length()];
        int length = ASCIIFoldingFilter.foldToASCII(string.toCharArray(), 0, output, 0, string.length());
        return new String(output).substring(0,length);
    }
}
