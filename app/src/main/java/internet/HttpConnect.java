package internet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpConnect
{
    private HttpsURLConnection conn;
    private BufferedWriter writer;
    private OutputStream os;
    private InputStream is;
    private SSLContext sslContext;
    private SSLSocketFactory socketFactory;
    private HostnameVerifier hostnameVerifier;
    private boolean keepAline;
    private boolean close;

    public HttpConnect(boolean keepAline)
    {
        this.keepAline = keepAline;
        TrustManager[] trustAllCerts =
                {
                        new X509TrustManager()
                        {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
                            {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
                            {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers()
                            {
                                return new X509Certificate[0];
                            }
                        }
                };
        hostnameVerifier = new HostnameVerifier()
        {
            @Override
            public boolean verify(String hostname, SSLSession session)
            {
                return true;
            }
        };
        try
        {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            socketFactory = sslContext.getSocketFactory();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (KeyManagementException e)
        {
            e.printStackTrace();
        }
    }

    public String sendPostRequest(String requestURL, HashMap<String, String> postDataParams)
    {
        URL url;
        String response = "0";
        try
        {
            url = new URL(requestURL);

            conn = (HttpsURLConnection) url.openConnection();/**??????connection??????**/

            conn.setHostnameVerifier(hostnameVerifier);
            /**HttpURLConnection?????????HTTP???????????????????????????socket???????????????????????????????????????timeout??????????????????????????????????????????????????????????????????????????????????????????**/
            conn.setReadTimeout(15000);/**???????????????**/
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");/**?????????????????? >> POST (??????????????????????????????????????????????????????)**/
            conn.setSSLSocketFactory(socketFactory);
            //conn.setDoInput(true);
            conn.setDoOutput(true);
            if (!keepAline)
                conn.setRequestProperty("Connection", "close");

            os = conn.getOutputStream();

            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                response = br.readLine();
            }
            else
            {
                response = "Error Create" + responseCode;
            }
            while (true)
            {
                if (conn.getInputStream().read() == -1)
                    break;
            }

            if (!close)
            {
                os.close();
                is.close();
            }
            conn.disconnect();
        }
        catch (UnknownHostException e)
        {
            response="???????????????";
        }
        catch (ConnectException e)
        {
            response="???????????????";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            response = "????????????: "+e.toString();
        }
        return response;
    }

    public void stop()
    {
        close = true;
        if (conn != null)
            conn.disconnect();

    }

    private void copyInputStreamToOutputStream(InputStream in, PrintStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) > 0)
        {
            out.write(buffer, 0, count);
        }
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }
}
