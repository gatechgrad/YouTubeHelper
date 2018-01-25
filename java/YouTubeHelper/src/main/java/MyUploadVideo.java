/**
 * 2018 - Levi D. Smith
 * programmatically uploads videos to YouTube
 */

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;
import com.sun.scenario.effect.impl.prism.ps.PPSOneSamplerPeer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class MyUploadVideo {

    private static final String VIDEO_FILE_FORMAT = "video/*";
//    private static final String SAMPLE_VIDEO_FILENAME = "/videos_to_upload/testvideo.mp4";
    private static final String VIDEO_UPLOAD_FOLDER = "E:\\ldsmith\\projects\\YouTubeHelper\\videos_to_upload";

    /** Application name. */
    private static final String APPLICATION_NAME = "My Upload Video";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
//            System.getProperty("user.home"), ".credentials/youtube-java-quickstart");
            System.getProperty("user.dir") + "/conf", ".credentials/youtube-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
//    private static final List<String> SCOPES =
//            Arrays.asList(YouTubeScopes.YOUTUBE_READONLY);

        private static final List<String> SCOPES =
            Arrays.asList(YouTubeScopes.YOUTUBE_UPLOAD, YouTubeScopes.YOUTUBE_READONLY);


    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Create an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        System.out.println("JSON file location");
        String strClientSecretFile = "/client_secret.json";
        System.out.println(strClientSecretFile);
        InputStream in =
                Quickstart.class.getResourceAsStream(strClientSecretFile);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized API client service, such as a YouTube
     * Data API client service.
     * @return an authorized API client service
     * @throws IOException
     */
    public static YouTube getYouTubeService() throws IOException {
        Credential credential = authorize();
        return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void uploadVideo(String strFile, String strTitle, DateTime publishDateTime, YouTube youtube) throws IOException {
            System.out.println("Uploading: " + strFile);

            System.out.println("Publish on " + publishDateTime.toStringRfc3339());
            Video videoObjectDefiningMetadata = new Video();
            VideoStatus status = new VideoStatus();
//            status.setPrivacyStatus("public");
            status.setPrivacyStatus("private");
            status.setPublishAt(publishDateTime);
            videoObjectDefiningMetadata.setStatus(status);

            VideoSnippet snippet = new VideoSnippet();
            Calendar cal = Calendar.getInstance();
            snippet.setTitle(strTitle);
            snippet.setDescription("Games that I played for Ludum Dare 40");

            List<String> tags = new ArrayList<String>();
            tags.add("ludum dare");
            tags.add("ludum dare 40");
            snippet.setTags(tags);

            videoObjectDefiningMetadata.setSnippet(snippet);
            InputStream is = new FileInputStream(strFile);

            /*
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    MyUploadVideo.class.getResourceAsStream("/testvideo.mp4"));
                    */
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    is);
            YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status",
                    videoObjectDefiningMetadata, mediaContent);

            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch(uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println("Upload in progress");
                            //System.out.println("Upload percentage: " + uploader.getProgress());
                            System.out.println("Upload percentage: " + uploader.getNumBytesUploaded());
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload Completed");
                            break;
                        case NOT_STARTED:
                            System.out.println("Upload not started");
                            break;
                    }
                }
            };

            uploader.setProgressListener(progressListener);
            Video retrunedVideo = videoInsert.execute();
            is.close();

            System.out.println("\n Returned Video");
            System.out.println("ID: " + retrunedVideo.getId());
            System.out.println("Title: " + retrunedVideo.getSnippet().getTitle());
            System.out.println("Tags: " + retrunedVideo.getSnippet().getTags());
            System.out.println("Privacy Status: " + retrunedVideo.getStatus().getPrivacyStatus());
            System.out.println("Video Count: " + retrunedVideo.getStatistics().getViewCount());
    }


    public static void main(String[] args) throws IOException {
        YouTube youtube = getYouTubeService();
        try {
            YouTube.Channels.List channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics");
//            channelsListByUsernameRequest.setForUsername("GoogleDevelopers");
            channelsListByUsernameRequest.setForUsername("gitcommand");

            ChannelListResponse response = channelsListByUsernameRequest.execute();
            Channel channel = response.getItems().get(0);
            System.out.printf(
                    "This channel's ID is %s. Its title is '%s', and it has %s views.\n",
                    channel.getId(),
                    channel.getSnippet().getTitle(),
                    channel.getStatistics().getViewCount());

            //upload a video
            File folder = new File(VIDEO_UPLOAD_FOLDER);
            File[] listOfFiles = folder.listFiles();
            int i;
            for (i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String strFile = listOfFiles[i].getName();
                    System.out.println("File: " + strFile);
                    Calendar c = Calendar.getInstance();
                    c.setTime(new Date());
                    c.set(Calendar.HOUR_OF_DAY, 8);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    c.add(Calendar.DATE, (1 + i));
                    DateTime dt = new DateTime(c.getTime());





                    MyUploadVideo.uploadVideo(VIDEO_UPLOAD_FOLDER + "\\" + strFile,
                            "Ludum Dare 40 - gameplay " + (i + 1), dt,
                            youtube);
                }

            }

            System.exit(0);
            /*
//            System.out.println("Uploading: " + SAMPLE_VIDEO_FILENAME);
            Video videoObjectDefiningMetadata = new Video();
            VideoStatus status = new VideoStatus();
//            status.setPrivacyStatus("public");
            status.setPrivacyStatus("private");
            videoObjectDefiningMetadata.setStatus(status);

            VideoSnippet snippet = new VideoSnippet();
            Calendar cal = Calendar.getInstance();
            snippet.setTitle("Test Upload with Java on " + cal.getTime());
            snippet.setDescription("Video uploaded using Java");

            List<String> tags = new ArrayList<String>();
            tags.add("hello world");
            snippet.setTags(tags);

            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    MyUploadVideo.class.getResourceAsStream("/testvideo.mp4"));
            YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status",
                    videoObjectDefiningMetadata, mediaContent);

            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch(uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println("Upload in progress");
                            System.out.println("Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload Completed");
                            break;
                        case NOT_STARTED:
                            System.out.println("Upload not started");
                            break;
                    }
                }
            };

            uploader.setProgressListener(progressListener);
            Video retrunedVideo = videoInsert.execute();

            System.out.println("\n Returned Video");
            System.out.println("ID: " + retrunedVideo.getId());
            System.out.println("Title: " + retrunedVideo.getSnippet().getTitle());
            System.out.println("Tags: " + retrunedVideo.getSnippet().getTags());
            System.out.println("Privacy Status: " + retrunedVideo.getStatus().getPrivacyStatus());
            System.out.println("Video Count: " + retrunedVideo.getStatistics().getViewCount());

*/


        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " +
                    e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
