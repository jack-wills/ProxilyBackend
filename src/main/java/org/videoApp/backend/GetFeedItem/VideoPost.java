package org.videoApp.backend.GetFeedItem;

public class VideoPost implements MediaPost {
    private class URL {
        private String url;

        public URL(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
    private URL video;

    public VideoPost(final String video) {
        this.video = new URL(video);
    }

    public URL getUrl() {
        return video;
    }
}
