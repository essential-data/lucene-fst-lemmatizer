package sk.essentialdata.lucene.analysis.fst;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * TokenFilterFactory that creates instances of {@link sk.essentialdata.lucene.analysis.fst.FSTTokenFilter}.
 * Example config for Slovak including a custom dictionary:
 * <pre class="prettyprint" >
 * &lt;filter class=&quot;sk.essentialdata.lucene.analysis.fst.FSTTokenFilterFactory&quot;
 *    fst=&quot;lib/slovaklemma.fst&quot;</pre>
 *
 * See <a href="https://github.com/essential-data/lucene-fst-lemmatizer">https://github.com/essential-data/lucene-fst-lemmatizer</a>
 *
 * @author miso
 * @date 4/30/14.
 */
public class FSTTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    private String fstFileName;

    public static final String PARAM_DICTIONARY = "fst";

    /**
     * Initialize this factory via a set of key-value pairs.
     */
    public FSTTokenFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        try {
            return new FSTTokenFilter(tokenStream, fstFileName);
        } catch (IOException e) {
            RuntimeException runtimeException = new RuntimeException(e.getMessage(), e);
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        String dictionaryArg = getOriginalArgs().get(PARAM_DICTIONARY);
        if (dictionaryArg == null) {
            throw new IllegalArgumentException("Parameter " + PARAM_DICTIONARY + " is mandatory.");
        }
        fstFileName = getOriginalArgs().get(PARAM_DICTIONARY);
    }

}
