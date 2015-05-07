import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;
import org.testng.Assert;
import org.testng.annotations.Test;
import sk.essentialdata.lucene.analysis.fst.FSTBuilder;
import sk.essentialdata.lucene.analysis.fst.FSTTokenFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author miso
 * @date 4/30/14.
 */
public class FSTTokenFilterTest {

    @Test
    public void testThroughput() throws IOException {
        String documentFileName = "src/test/resources/wikipedia_sample.txt";
        long startTime = System.currentTimeMillis();
        timedThroughput(documentFileName);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime);
    }


    public void timedThroughput(String documentFileName) throws IOException {
        String fstFileName = "fst/slovaklemma.fst";

        File fstFile = new File(fstFileName);
        FST<CharsRef> fst = FST.read(fstFile.toPath(), CharSequenceOutputs.getSingleton());

        File documentFile = new File(documentFileName);

        BufferedReader br = new BufferedReader(new FileReader(documentFile));
        String line;
        int n=0,m=0;
        while ((line = br.readLine()) != null) {
            CharsRef output = Util.get(fst, new BytesRef(line));
            if (output == null) {
                n++;
            } else {
                m++;
                Arrays.asList(output.toString().split("\\|"));
            }
        }
        br.close();
        System.out.println(documentFileName);
        System.out.println("FST returned null: " + n);
        System.out.println("Found match: " + m);
    }

    @Test
    public void testVlastMaterial() throws IOException {
        String fstFileName = "fst/slovaklemma_ascii.fst";
        File fstFile = new File(fstFileName);
        FST<CharsRef> fst = FST.read(fstFile.toPath(), CharSequenceOutputs.getSingleton());

        String material[] = new String[] {"cislo", "predpisu", "doplna"};
        Set<String> index = new HashSet<String>();
        for (String s : material) {
            CharsRef output = Util.get(fst, new BytesRef(s));
            if (output == null) {
                index.add(s);
            } else {
                for (String s1 : Arrays.asList(output.toString().split("\\|"))) {
                    index.add(s1);
                }
            }
        }
        for (String s : index) {
            System.out.println(s);
        }
        Assert.assertTrue(index.contains("cislo"));
        Assert.assertTrue(index.contains("predpis"));
        Assert.assertTrue(index.contains("doplnat"));
    }
}
