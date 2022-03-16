package org.nineml.coffeepot.utils;

import net.sf.saxon.lib.Logger;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * A cache for compiled grammars.
 * <p>Grammar parsing can be time consuming, but after development is finished, they change
 * infrequently. This cache keeps compiled versions of grammars and uses them automatically
 * if they are up-to-date (based on last modified time).</p>
 * <p>The location of cached grammars is controlled by the {@link ParserOptions#cacheDir} setting.
 * If it's a relative location (such as <code>.grammar-cache</code>), the compiled grammars
 * are stored in the cache with the same filename as the original grammar except that the
 * extension is changed. If the location is absolute (begins with a "/"; such as
 * <code>/home/user/.config/nineml/grammar-cache</code>), the name of the cached file is
 * a cryptographic hash of the original absolute filename.</p>
 * <p>The cache directory must be configured; if it's unconfigured, no caching is performed.</p>
 */
public class Cache {
    /** The log category for messages related to caching. */
    public static final String logcategory = "Cache";

    private static final String filesep = System.getProperty("file.separator");
    private final ParserOptions options;
    private final String cacheDir;
    private final boolean globalCache;
    private final Processor processor;

    /** Create the cache.
     * <p>If the {@link ParserOptions#cacheDir} is null, the constructed cache will
     * never store or retrieve any cached grammars.</p>
     * @param options the parser options.
     */
    public Cache(ParserOptions options) {
        this.options = options;
        this.processor = new Processor(false);

        CaptureErrors capture = new CaptureErrors();
        processor.getUnderlyingConfiguration().setLogger(capture);

        if (options.getCacheDir() == null) {
            cacheDir = null;
            globalCache = false;
            return;
        }

        String useDir = options.getCacheDir();
        globalCache = useDir.startsWith(filesep);

        if (globalCache) {
            if (!cacheExists(useDir)) {
                useDir = null;
            }
        }

        cacheDir = useDir;
    }

    private boolean cacheExists(String useDir) {
        File cache = new File(useDir);
        if (cache.mkdirs()) {
            options.getLogger().trace(logcategory, "Created cache: %s", useDir);
        } else {
            if (!cache.exists() || !cache.isDirectory() || !cache.canWrite()) {
                options.getLogger().warn(logcategory, "Cannot use cache: %s", useDir);
                return false;
            }
        }
        return true;
    }

    private URI getCacheURI(URI grammar) {
        if (cacheDir == null || !"file".equals(grammar.getScheme())) {
            return grammar;
        }

        String path = grammar.getPath();

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path filename = cwd.resolve(path);

        if (!filename.toFile().exists()) {
            return grammar;
        }

        Path cached;
        if (globalCache) {
            Path cache = Paths.get(cacheDir);
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(filename.toString().getBytes(StandardCharsets.UTF_8));
                byte[] hash = md.digest();
                StringBuilder sb = new StringBuilder();
                for (int pos = hash.length - 8; pos < hash.length; pos++) {
                    sb.append(Integer.toString((hash[pos] & 0xff) + 0x100, 16).substring(1));
                }
                cached = cache.resolve(sb.toString() + ".cxml");
            } catch (NoSuchAlgorithmException ex) {
                options.getLogger().error(logcategory, "Failed to create message digest: %s", ex.getMessage());
                return grammar;
            }
        } else {
            Path cache = filename.getParent().resolve(cacheDir);
            if (cacheExists(cache.toString())) {
                String basename = filename.toFile().getName().replaceAll("\\.", "_") + ".cxml";
                cached = cache.resolve(basename);
            } else {
                return grammar;
            }
        }

        return cached.toUri();
    }

    /**
     * Get the cached URI for a grammar.
     * <p>This method returns the URI that should be used to access the grammar.
     * If the <code>grammar</code> URI was cached and the cached-version is up-to-date, the
     * URI of the cached grammar will be returned. Otherwise <em>the original</em> grammar
     * URI is returned.</p>
     * @param grammar the absolute URI of the grammar to be parsed
     * @return the absolute URI of the cached version, or the original grammar URI if no cached version is available
     */
    public URI getCached(URI grammar) {
        URI cacheURI = getCacheURI(grammar);
        if (cacheURI == grammar) {
            return grammar;
        }

        String path = grammar.getPath();
        Path cwd = Paths.get(System.getProperty("user.dir"));

        Path filename = cwd.resolve(path);
        Path cached = Paths.get(cacheURI);

        if (cached.toFile().exists()) {
            File gfile = filename.toFile();
            File cfile = cached.toFile();
            if (cfile.lastModified() >= gfile.lastModified()) {
                return cached.toUri();
            }
        }

        return grammar;
    }

    /**
     * Store the compiled grammar in the cache.
     * <p>If the compiled grammar is not, in fact, well-formed XML (because, for example,
     * it contains references to non-XML characters), it will not be stored.</p>
     * @param grammar The absolute URI of the original grammar
     * @param compiledGrammar the compiled grammar
     */
    public void storeCached(URI grammar, String compiledGrammar) {
        if (cacheDir == null) {
            return;
        }

        URI cacheURI = getCacheURI(grammar);
        if (cacheURI == grammar) {
            options.getLogger().debug(logcategory, "Not cached: %s", grammar);
            return;
        }

        // It was once possible to produce compiled grammars that weren't valid XML.
        // For example, they could include references to characters that may
        // not appear in XML, such as #0. Check to make sure that hasn't happened again.
        DocumentBuilder builder = processor.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(compiledGrammar.getBytes(StandardCharsets.UTF_8));
        Source source = new SAXSource(new InputSource(bais));

        try {
            builder.build(source);
        } catch (SaxonApiException ex) {
            options.getLogger().warn(logcategory, "Cannot cache compiled grammar: %s", ex.getMessage());
            return;
        }

        try {
            File cached = new File(cacheURI);
            PrintStream ps = new PrintStream(cached);
            ps.println(compiledGrammar);
            ps.close();
        } catch (IOException ex) {
            options.getLogger().warn(logcategory, "Failed to write cache: %s: %s", cacheURI, ex.getMessage());
        }
    }

    private static class CaptureErrors extends Logger {
        public final ArrayList<String> messages = new ArrayList<>();

        @Override
        public void println(String message, int severity) {
            messages.add(message);
        }
    }
}
