package text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class HtmlProcessor {
    private static final String TAGS_TO_REMOVE = "iframe, embed, script, noscript, nobr, "
            + "style, link, img, source, wbr, area, base, param";

    public String clean(String html) {
        Document doc = Jsoup.parse(html);
        doc.select(TAGS_TO_REMOVE).remove();
        removeComments(doc);
        return doc.outerHtml();
    }

    public ProcessedHtml process(String html) {
        Document doc = Jsoup.parse(html);
        String title = doc.title();

        ProcessedHtml result = new ProcessedHtml();
        result.setTitle(title);

        Elements description = doc.select("meta[name=description]");
        if (!description.isEmpty()) {
            String metaContent = description.attr("content");
            result.setMetaContent(metaContent);
        }

        Elements keywords = doc.select("meta[name=keywords]");
        if (!keywords.isEmpty()) {
            String metaKeywords = keywords.attr("content");
            result.setMetaKeywords(metaKeywords);
        }

        doc.select(TAGS_TO_REMOVE).remove();

        String bodyHtml = doc.select("body").outerHtml();
        Document body = Jsoup.parse(bodyHtml);

        JsoupTextExtractor visitor = new JsoupTextExtractor();
        body.traverse(visitor);

        List<List<String>> sentences = new ArrayList<>();
        List<String> blocks = visitor.getAllTextBlocks();

        for (String block : blocks) {
            block = TextUtils.clean(block);
            List<List<String>> tokenized = TextUtils.tokenize(block);
            sentences.addAll(tokenized);
        }

        String content = sentences.stream().map(s -> String.join(" ", s)).collect(Collectors.joining("\n"));
        result.setContent(content);

        ListMultimap<String, String> tags = ArrayListMultimap.create();
        Elements headers = body.select("h1, h2, h3, h4, h5, h6");
        for (Element htag : headers) {
            String tagName = htag.nodeName().toLowerCase();
            String text = htag.text().trim();
            if (!text.isEmpty()) {
                tags.put(tagName, text);
            }
        }

        result.setH1(tags.get("h1"));
        result.setH2(tags.get("h2"));
        result.setH3(tags.get("h3"));
        result.setH4(tags.get("h4"));
        result.setH5(tags.get("h5"));
        result.setH6(tags.get("h6"));

        return result;
    }

    private static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }

}
