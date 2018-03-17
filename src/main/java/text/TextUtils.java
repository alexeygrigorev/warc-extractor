package text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.archive.io.ArchiveRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class TextUtils {

    private static final StanfordCoreNLP ENGLISH_NLP_PIPELINE = createEnglishPipeline();
    private static final char NO_BREAK_SPACE = '\u00A0';
    private static final Set<String> ALLOWED_INNER_SENTENCE_PUNCTUATION = ImmutableSet.of(".", "+");

    private static final LanguageDetector LANG_DETECTOR = createLangDetector();

    public static String extractHtml(ArchiveRecord r) {
        try {
            byte[] rawData = IOUtils.toByteArray(r, r.available());
            String rawContent = new String(rawData, "UTF-8");
            String[] split = rawContent.split("(\r?\n){2}", 2);
            return split[1].trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<String> extractText(String html) {
        Document document = Jsoup.parse(html);
        Element body = document.body();
        if (body == null) {
            return Optional.empty();
        }

        JsoupTextExtractor textExtractor = new JsoupTextExtractor();
        body.traverse(textExtractor);
        String text = textExtractor.getText();
        return Optional.of(text);
    }

    public static String clean(String input) {
        input = StringEscapeUtils.unescapeHtml4(input);
        input = cleanMarkup(input);
        return input;
    }

    public static String cleanMarkup(String line) {
        String after = line.replaceAll("</?\\w+(\\s.+?)?>", " ");
        // .replaceAll("\\[/?\\w+(\\s.+?)?\\]", " ");
        return after;
    }

    public static List<List<String>> tokenize(String input) {
        Annotation lineAnnotation = new Annotation(input);
        ENGLISH_NLP_PIPELINE.annotate(lineAnnotation);
        List<List<String>> result = Lists.newArrayList();

        for (CoreMap sentence : lineAnnotation.get(SentencesAnnotation.class)) {
            List<String> sentenceRes = Lists.newArrayList();

            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String textToken = token.get(TextAnnotation.class);

                if (textToken.indexOf(NO_BREAK_SPACE) >= 0) {
                    String newToken = textToken.replace(NO_BREAK_SPACE, ' ');
                    sentenceRes.addAll(Arrays.asList(newToken.split(" ")));
                    continue;
                }

                if (isInnerSentencePunctuation(textToken)) {
                    sentenceRes = cleanSentence(sentenceRes);

                    if (!sentenceRes.isEmpty()) {
                        result.add(sentenceRes);
                    }

                    sentenceRes = Lists.newArrayList();
                } else {
                    sentenceRes.add(textToken);
                }
            }

            sentenceRes = cleanSentence(sentenceRes);

            if (!sentenceRes.isEmpty()) {
                result.add(sentenceRes);
            }
        }

        return result;
    }

    private static List<String> cleanSentence(List<String> sentence) {
        List<String> result = Lists.newArrayListWithExpectedSize(sentence.size());

        for (String token : sentence) {
            if (token.length() <= 1) {
                continue;
            }

            if (TextUtils.isPunctuation(token)) {
                continue;
            }

            if (token.length() > 4) {
                if (NumberUtils.isDigits(token)) {
                    continue;
                }
            }

            result.add(token);
        }

        return result;
    }

    private static boolean isInnerSentencePunctuation(String textToken) {
        if (ALLOWED_INNER_SENTENCE_PUNCTUATION.contains(textToken)) {
            return false;
        }

        return TextUtils.isPunctuation(textToken);
    }

    public static boolean isPunctuation(String str) {
        int first = str.charAt(0);
        return !Character.isDigit(first) && !Character.isLetter(first);
    }

    public static List<List<String>> filterBad(List<List<String>> tokens) {
        List<List<String>> result = new ArrayList<>();

        for (List<String> sentence : tokens) {
            if (isGood(sentence)) {
                result.add(sentence);
            }
        }

        return result;
    }

    private static boolean isGood(List<String> sentence) {
        if (sentence.isEmpty()) {
            return false;
        }

        if (sentence.size() > 1) {
            return true;
        }

        String word = sentence.get(0);
        if (word.length() <= 3) {
            return false;
        }

        if (isPunctuation(word)) {
            return false;
        }

        if (NumberUtils.isNumber(word)) {
            return false;
        }

        return true;
    }

    public static String languageDetect(String text) {
        com.google.common.base.Optional<LdLocale> result = LANG_DETECTOR.detect(text);

        if (result.isPresent()) {
            return result.get().getLanguage();
        } else {
            return "unk";
        }
    }

    private static LanguageDetector createLangDetector() {
        try {
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            return LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StanfordCoreNLP createEnglishPipeline() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        // see tokenize options here: PTBTokenizer
        props.put("tokenize.options", "normalizeParentheses=false,normalizeOtherBrackets=false,"
                + "normalizeAmpersandEntity=true,untokenizable=noneKeep");
        return new StanfordCoreNLP(props);
    }

}
