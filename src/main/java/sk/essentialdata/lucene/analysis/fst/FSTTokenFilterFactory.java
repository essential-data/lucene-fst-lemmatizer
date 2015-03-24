package sk.essentialdata.lucene.analysis.fst;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * TokenFilterFactory that creates instances of {@link sk.essentialdata.lucene.analysis.fst.FSTTokenFilter}.
 * Example config for British English including a custom dictionary, case insensitive matching:
 * <pre class="prettyprint" >
 * &lt;filter class=&quot;solr.HunspellStemFilterFactory&quot;
 *    dictionary=&quot;en_GB&quot;
 *    affix=&quot;en_GB.aff&quot;
 *    ignoreCase=&quot;true&quot; /&gt;</pre>
 * Both parameters dictionary and affix are mandatory.
 * <br/>
 * The parameter ignoreCase (true/false) controls whether matching is case sensitive or not. Default false.
 * <br/>
 * The parameter strictAffixParsing (true/false) controls whether the affix parsing is strict or not. Default true.
 * If strict an error while reading an affix rule causes a ParseException, otherwise is ignored.
 * <br/>
 * Dictionaries for many languages are available through the OpenOffice project.
 *
 * See <a href="http://wiki.apache.org/solr/Hunspell">http://wiki.apache.org/solr/Hunspell</a>
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
        assureMatchVersion();
        String dictionaryArg = getOriginalArgs().get(PARAM_DICTIONARY);
        if (dictionaryArg == null) {
            throw new IllegalArgumentException("Parameter " + PARAM_DICTIONARY + " is mandatory.");
        }
        fstFileName = getOriginalArgs().get(PARAM_DICTIONARY);
    }

}
