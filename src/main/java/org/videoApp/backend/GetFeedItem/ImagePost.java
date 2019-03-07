package org.videoApp.backend.GetFeedItem;

public class ImagePost implements MediaPost {
    private class URL {
        private String url;

        public URL(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
    private URL image;

    public ImagePost(final String image) {
        this.image = new URL(image);
    }

    public URL getUrl() {
        return image;
    }
}
