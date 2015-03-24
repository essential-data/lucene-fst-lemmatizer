package sk.essentialdata.lucene.analysis.fst;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import java.io.*;

/**
 * @author miso
 * @date 25.2.2015.
 */
public class FSTUtils {
    public static void main(String[] args) {
        if(args.length >= 2 && "lemmatize".equals(args[0])) {
            boolean echo = (args.length >=3 && "-e".equals(args[2]));
            try {
                File file = new File(args[1]);
                FST<CharsRef> fst = FST.read(file, CharSequenceOutputs.getSingleton());

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                StreamTokenizer st = new StreamTokenizer(br);
                long exceptionCount = 0;
                long tokens = 0;
                while (st.nextToken() != StreamTokenizer.TT_EOF) {
                    try {
                        tokens++;
                        String word = st.sval;
                        if (word == null || word.length() < 1) {
                            continue;
                        }
                        CharsRef lemma = Util.get(fst, new BytesRef(word));
                        if (lemma != null) {
                            System.out.print(lemma.toString().replace("|", " ") + " "); // multiple lemmas are delimited by "|"
                        } else if (echo) {
                            System.out.print(word + " ");
                        }
                    } catch (RuntimeException e) {
                        exceptionCount++;
                    }
                }
                if (tokens == 0) {
                    System.out.println("0 tokens");
                } else if (exceptionCount * 2 > tokens) {
                    System.out.println("Too many exceptions: " + exceptionCount + " of " + tokens + " tokens");
                }
            } catch (IOException io){
                io.printStackTrace();
            }
        } else {
            System.out.println("Usage: fstutils lemmatize <path-to-fst> <options>, where options are:\n" +
                    "-e: echo when a word is not in the dictionary, e.g. 'foo bar' -> 'foo bar'.\n" +
                    "Without the -e option it is 'foo bar' -> 'bar'");
        }
    }
}
