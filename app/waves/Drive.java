package waves;

import com.dropbox.core.*;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.util.IOUtil;

import play.Play;
import play.libs.Files;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Luis Chávez
 */
public class Drive {

    static final String[] VALID_DRIVE = {
            "local", "dropbox"
    };

    static Map<String, String> options() {
        String drive = Play.configuration.getProperty("waves.drive", "local");

        String dropboxKey = Play.configuration.getProperty("waves.dropbox.key", "");
        String dropboxSecret = Play.configuration.getProperty("waves.dropbox.secret", "");
        String dropboxToken = Play.configuration.getProperty("waves.dropbox.token", "");

        Map<String, String> options = new HashMap<>();
        options.put("drive", drive);
        options.put("dropbox.key", dropboxKey);
        options.put("dropbox.secret", dropboxSecret);
        options.put("dropbox.token", dropboxToken);

        return options;
    }

    static DriveFile readLocal(String in) {
        File file = new File(in);

        if (file.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(file);

                return new DriveFile(file.length(), inputStream);
            } catch (FileNotFoundException e) {
            }
        }

        return null;
    }

    static boolean writeLocal(File in, String out, String name, String ext) {
        File path = new File(out);
        path.mkdirs();

        File target = new File(String.format("%s/%s.%s", out, name, ext));
        try {
            target.createNewFile();
        } catch (IOException e) {
        }

        if (!target.exists()) {
            return false;
        }

        try {
            Files.copy(in, target);
            Files.delete(in);

            return true;
        } catch (RuntimeException ex) {
        }

        return false;
    }

    static boolean deleteLocal(String in) {
        File file = new File(in);

        if (file.exists()) {
            return Files.delete(file);
        }

        return false;
    }

    /*static DbxClient authDropbox(String dropBoxAppKey, String dropBoxAppSecret)
            throws IOException, DbxException {
        DbxAppInfo dbxAppInfo = new DbxAppInfo(dropBoxAppKey, dropBoxAppSecret);
        DbxRequestConfig dbxRequestConfig = new DbxRequestConfig(
                "waves-project", Locale.getDefault().toString());
        DbxWebAuthNoRedirect dbxWebAuthNoRedirect = new DbxWebAuthNoRedirect(
                dbxRequestConfig, dbxAppInfo);
        String authorizeUrl = dbxWebAuthNoRedirect.start();
        System.out.println("1. Authorize: Go to URL and click Allow : "
                + authorizeUrl);
        System.out
                .println("2. Auth Code: Copy authorization code and input here ");
        String dropboxAuthCode = new BufferedReader(new InputStreamReader(
                System.in)).readLine().trim();
        DbxAuthFinish authFinish = dbxWebAuthNoRedirect.finish(dropboxAuthCode);
        String authAccessToken = authFinish.accessToken;
        DbxClient client = new DbxClient(dbxRequestConfig, authAccessToken);
        System.out.println("Dropbox Account Name: "
                + client.getAccountInfo().displayName);

        return client;
    }*/

    static DriveFile readDropbox(String in) {
        Map<String, String> options = options();

        String key = options.get("dropbox.key");
        String secret = options.get("dropbox.secret");
        String token = options.get("dropbox.token");

        String auth = String.format("{\"key\": \"%s\", \"secret\": \"%s\", \"access_token\": \"%s\"}", key, secret, token);

        try {
            /*try {
                authDropbox(key, secret);
            } catch (IOException e) {
            } catch (DbxException e) {
            }*/

            DbxAuthInfo authInfo = DbxAuthInfo.Reader.readFully(auth);

            String userLocale = Locale.getDefault().toString();

            DbxRequestConfig requestConfig = new DbxRequestConfig("waves-project", userLocale);
            DbxClient client = new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);

            in = "/".concat(in);
            if (null != DbxPath.findError(in)) {
                return null;
            }

            try {
                DbxClient.Downloader downloader = client.startGetFile(in, null);

                return new DriveFile(downloader.metadata.numBytes, downloader.body);
            } catch (DbxException e) {
            }
        } catch (JsonReadException e) {
        }

        return null;
    }

    static boolean writeDropbox(File in, String out, String name, String ext) {
        Map<String, String> options = options();

        String key = options.get("dropbox.key");
        String secret = options.get("dropbox.secret");
        String token = options.get("dropbox.token");

        String auth = String.format("{\"key\": \"%s\", \"secret\": \"%s\", \"access_token\": \"%s\"}", key, secret, token);

        try {
            /*try {
                authDropbox(key, secret);
            } catch (IOException e) {
            } catch (DbxException e) {
            }*/
            DbxAuthInfo authInfo = DbxAuthInfo.Reader.readFully(auth);

            String userLocale = Locale.getDefault().toString();

            DbxRequestConfig requestConfig = new DbxRequestConfig("waves-project", userLocale);
            DbxClient client = new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);

            //String folder = String.format("/%s", out);
            //client.createFolder(folder);

            String file = String.format("/%s/%s.%s", out, name, ext);

            if (null != DbxPath.findError(file)) {
                return false;
            }

            try {
                InputStream inputStream = new FileInputStream(in);
                try {
                    client.uploadFile(file, DbxWriteMode.add(), -1, inputStream);
                    return true;
                } catch (DbxException ex) {
                    return false;
                } finally {
                    IOUtil.closeInput(inputStream);
                }
            } catch (IOException ex) {
                return false;
            }
        } catch (JsonReadException e) {
            System.out.println(e);
        }

        return false;
    }

    static boolean deleteDropbox(String in) {
        Map<String, String> options = options();

        String key = options.get("dropbox.key");
        String secret = options.get("dropbox.secret");
        String token = options.get("dropbox.token");

        String auth = String.format("{\"key\": \"%s\", \"secret\": \"%s\", \"access_token\": \"%s\"}", key, secret, token);

        try {
            /*try {
                authDropbox(key, secret);
            } catch (IOException e) {
            } catch (DbxException e) {
            }*/

            DbxAuthInfo authInfo = DbxAuthInfo.Reader.readFully(auth);

            String userLocale = Locale.getDefault().toString();

            DbxRequestConfig requestConfig = new DbxRequestConfig("waves-project", userLocale);
            DbxClient client = new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);

            in = "/".concat(in);
            if (null != DbxPath.findError(in)) {
                return false;
            }

            try {
                client.delete(in);

                return true;
            } catch (DbxException e) {
            }
        } catch (JsonReadException e) {
        }

        return false;
    }

    public static DriveFile read(String in) {
        Map<String, String> options = options();

        String drive = options.get("drive");
        if (!Arrays.asList(VALID_DRIVE).contains(drive)) {
            return readLocal(in);
        }

        return "local".equals(drive) ? readLocal(in) : readDropbox(in);
    }

    public static boolean write(File in, String out, String name, String ext) {
        Map<String, String> options = options();

        String drive = options.get("drive");
        if (!Arrays.asList(VALID_DRIVE).contains(drive)) {
            return writeLocal(in, out, name, ext);
        }

        return "local".equals(drive) ? writeLocal(in, out, name, ext) : writeDropbox(in, out, name, ext);
    }

    public static boolean delete(String in) {
        Map<String, String> options = options();

        String drive = options.get("drive");
        if (!Arrays.asList(VALID_DRIVE).contains(drive)) {
            return deleteLocal(in);
        }

        return "local".equals(drive) ? deleteLocal(in) : deleteDropbox(in);
    }

    public static class DriveFile {

        public long length;
        public InputStream inputStream;

        public DriveFile(long length, InputStream inputStream) {
            this.length = length;
            this.inputStream = inputStream;
        }
    }
}
