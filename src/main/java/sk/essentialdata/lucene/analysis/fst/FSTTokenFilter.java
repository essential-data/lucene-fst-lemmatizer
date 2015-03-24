package sk.essentialdata.lucene.analysis.fst;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author miso
 * @date 4/29/14.
 */
public class FSTTokenFilter extends TokenFilter {


    private static void log(String s) throws IOException {
        String filename= "logs/FSTTokenFilter.log";
        FileWriter fw = new FileWriter(filename,true); //the true will append the new data
        fw.write(s + "\n");//appends the string to the file
        fw.close();
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
    private List<String> buffer;
    private State savedState;
    FST<CharsRef> fst;
    private BytesRef bytesRef;
    private CharsRef charsRef;
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected FSTTokenFilter(TokenStream input, String fstFileName) throws IOException {
        super(input);
        File file = new File(fstFileName);
        fst = FST.read(file, CharSequenceOutputs.getSingleton());
    }

    /**
     * 1. Read next token
     * 2. Apply stemmer
     * 3. If there are multiple stems
     *      just one increment take place and other stems are put into the buffer
     *      the buffer and state of the attributes are saved
     * 4. In the next calls, if the buffer is not empty, the stemmer is not called,
     *      but the value from buffer is used (@var nextStem)
     * {@inheritDoc}
     */
    @Override
    public boolean incrementToken() throws IOException {
        if (buffer != null && !buffer.isEmpty()) {
            String nextStem = buffer.remove(0);
            restoreState(savedState);
            posIncAtt.setPositionIncrement(0);
            termAtt.copyBuffer(nextStem.toCharArray(), 0, nextStem.length());
            return true;
        }

        if (!input.incrementToken()) {
            return false;
        }

        if (keywordAtt.isKeyword()) {
            return true;
        }

        // this can surely be optimized, but we have no need yet
        // @see UnicodeUtil::UTF16toUTF8() which adds a lot of zero bytes at the end.

        int length = termAtt.length();

        /**
         * @var termBuffer input buffer, which is being overwritten from the start by the next word.
         */
        char[] termBuffer = termAtt.buffer();

        /**
         * @var bytesRef wrapper for byte array, which is an input for FST
         */
        bytesRef = new BytesRef(new String(termBuffer).substring(0,length).getBytes("UTF-8"));
        /**
         * @var charsRef wrapper for char array, which is an output from FST.
         * In case of multiple outputs, they are separated by "|"
         */
        charsRef = Util.get(fst, bytesRef);
//        charsRef = utilGetDebug(fst, bytesRef);

        if (charsRef == null) {
            return true; // not found in FST
        }
        buffer = new ArrayList<String>(Arrays.asList(charsRef.toString().split("\\|")));

        if (buffer.isEmpty()) { // we do not know this word, return it unchanged
            return true;
        }

        String stem = buffer.remove(0);
        termAtt.copyBuffer(stem.toCharArray(), 0, stem.length());

        if (!buffer.isEmpty()) {
            savedState = captureState();
        }

        return true;
    }

    /**
     * When debugging, use this method instead of Util.get
     * @return
     * @throws IOException
     */
    protected static CharsRef utilGetDebug(FST<CharsRef> fst, BytesRef bytesRef) throws IOException {
        final FST.BytesReader fstReader = fst.getBytesReader();

        final FST.Arc<CharsRef> arc = fst.getFirstArc(new FST.Arc<CharsRef>());

        log("First arc: " + arc);

        // Accumulate output as we go
        CharsRef output = fst.outputs.getNoOutput();
        for(int i=0;i<bytesRef.length;i++) {
            log(i + ", searching for " + (bytesRef.bytes[i+bytesRef.offset] & 0xFF));
            if (fst.findTargetArc(bytesRef.bytes[i+bytesRef.offset] & 0xFF, arc, arc, fstReader) == null) {
                log("Not found");
                return null;
            }
            log("Found: " + arc);
            log("Output: " + arc.output);
            output = fst.outputs.add(output, arc.output);
        }

        if (arc.isFinal()) {
            CharsRef result = fst.outputs.add(output, arc.nextFinalOutput);
            log("Final " + result);
            return result;
        } else {
            log("Not final a word already ended");
            return null;
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        super.reset();
        buffer = null;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("USAGE: FSTBuilder <FST input file path>");
            System.exit(1);
        }
        File file = new File(args[0]);
        FST<CharsRef> fst = FST.read(file, CharSequenceOutputs.getSingleton());

        for (String s : Arrays.asList("najprudší", "najprudkejší", "neni", "chujovinami", "piči", "mám")) {
            System.out.println(utilGetDebug(fst, new BytesRef(s)));
        }

    }
}
