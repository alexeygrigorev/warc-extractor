package warc;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSON;

import text.HtmlProcessor;
import text.ProcessedHtml;
import text.TextUtils;
import text.UnicodeUtils;

public class WarcProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarcProcessor.class);

    private static final int MAX_DOCUMENT_LEN = 5000000;
    private static final int MIN_DOCUMENT_LEN = 100;

    private final HtmlProcessor processor = new HtmlProcessor();

    private long documentcounter = 1;

    private final Set<String> languages;
    private final PrintWriter output;

    public WarcProcessor(Set<String> languages, PrintWriter output) {
        this.languages = languages;
        this.output = output;
    }

    public void processArchive(ArchiveReader archive) throws Exception {
        try {
            for (ArchiveRecord record : archive) {
                try {
                    process(record);

                    if (documentcounter % 1000 == 0) {
                        LOGGER.info("processed {} documents so far", documentcounter);
                        documentcounter++;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Caught Exception", ex);
                }
            }

        } catch (Exception ex) {
            LOGGER.error("Caught Exception", ex);
        }
        output.flush();
    }

    private void process(ArchiveRecord record) throws Exception {
        String url = record.getHeader().getUrl();
        if (StringUtils.isBlank(url)) {
            // if there's no URL associated with a page, it's not a web page
            return;
        }

        int documentLength = record.available();
        if (documentLength > MAX_DOCUMENT_LEN) {
            LOGGER.info("the document at {} is too big ({} bytes). Skipping it", url, documentLength);
            return;
        }

        String html = TextUtils.extractHtml(record);
        if (html.length() <= MIN_DOCUMENT_LEN) {
            return;
        }

        ProcessedHtml processed = processor.process(html);
        String content = processed.getContent();
        if (UnicodeUtils.isGarbled(content)) {
            return;
        }

        String lang = TextUtils.languageDetect(content);
        if (!languages.contains(lang)) {
            return;
        }

        String json = JSON.std.asString(processed);
        String result = url + "\t" + lang + "\t" + json;
        output.println(result);
    }

}
