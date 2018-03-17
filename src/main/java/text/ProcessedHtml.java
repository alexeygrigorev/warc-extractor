package text;

import java.util.List;

public class ProcessedHtml {

    private String title;
    private String content;
    private List<String> h1;
    private List<String> h2;
    private List<String> h3;
    private List<String> h4;
    private List<String> h5;
    private List<String> h6;
    private String metaContent = "";
    private String metaKeywords = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getH1() {
        return h1;
    }

    public void setH1(List<String> h1) {
        this.h1 = h1;
    }

    public List<String> getH2() {
        return h2;
    }

    public void setH2(List<String> h2) {
        this.h2 = h2;
    }

    public List<String> getH3() {
        return h3;
    }

    public void setH3(List<String> h3) {
        this.h3 = h3;
    }

    public List<String> getH4() {
        return h4;
    }

    public void setH4(List<String> h4) {
        this.h4 = h4;
    }

    public List<String> getH5() {
        return h5;
    }

    public void setH5(List<String> h5) {
        this.h5 = h5;
    }

    public List<String> getH6() {
        return h6;
    }

    public void setH6(List<String> h6) {
        this.h6 = h6;
    }

    public String getMetaContent() {
        return metaContent;
    }

    public void setMetaContent(String metaContent) {
        this.metaContent = metaContent;
    }

    public String getMetaKeywords() {
        return metaKeywords;
    }

    public void setMetaKeywords(String metaKeywords) {
        this.metaKeywords = metaKeywords;
    }

}
