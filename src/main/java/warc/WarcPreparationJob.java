package warc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.warc.WARCReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class WarcPreparationJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarcPreparationJob.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[] { "--input", "/home/agrigorev/Downloads/cc/warc", "--output",
                    "/home/agrigorev/Downloads/cc/warc-processed" };
        }

        Map<String, String> params = Params.parse(args);
        LOGGER.info("using paramgs: {}", params);

        String input = params.get("--input");
        LOGGER.info("Input path: {}", input);

        String output = params.get("--output");
        LOGGER.info("Output path: {}", output);

        Set<String> langs = languages(params);

        run(input, output, langs);
    }

    private static void run(String input, String output, Set<String> langs) throws Exception {
        Collection<File> inFiles = FileUtils.listFiles(new File(input), new String[] { "warc.gz" }, false);

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (File in : inFiles) {
            pool.submit(() -> process(in, output, langs));
        }

        pool.shutdown();
        pool.awaitTermination(100, TimeUnit.DAYS);
    }

    private static void process(File in, String output, Set<String> langs) {
        LOGGER.info("processing {}...", in);

        new File(output).mkdirs();

        try {
            File outName = new File(output, in.getName() + "_processed.gz");

            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outName));
                    GzipCompressorOutputStream out = new GzipCompressorOutputStream(outputStream);
                    PrintWriter pw = new PrintWriter(out);
                    ArchiveReader archive = WARCReaderFactory.get(in);) {

                WarcProcessor processor = new WarcProcessor(langs, pw);
                processor.processArchive(archive);
            }

            LOGGER.info("successfully processed {}", in);
        } catch (Exception e) {
            LOGGER.warn("got exception!", e);
        }
    }

    private static Set<String> languages(Map<String, String> params) {
        if (params.containsKey("--languages")) {
            String string = params.get("--languages");
            return ImmutableSet.copyOf(string.split(","));
        }

        return ImmutableSet.of("en", "de");
    }
}
