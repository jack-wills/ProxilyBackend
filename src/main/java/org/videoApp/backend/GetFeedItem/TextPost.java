package org.videoApp.backend.GetFeedItem;

public class TextPost implements MediaPost {

    private class Content {
        private String content;

        public Content(final String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }
    private Content text;

    public TextPost(final String text) {
        this.text = new Content(text);
    }

    public Content getText() {
        return text;
    }
}
