package org.jenkinsci.plugins.diawi;

/**
 * Created by salaheld on 18/06/2017.
 */

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DiawiRequest {


    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String upload_url ="https://upload.diawi.com/";
    private static final String status_url="https://upload.diawi.com/status";
    private  String token;
    private  BufferedReader rd;
    private String proxyHost="";
    private int proxyPort=8080;
    private String proxyProtocol="http";

    public String getProxyHost(){return proxyHost;}
    public int getProxyPort(){return proxyPort;}
    public String getProxyProtocol(){return proxyProtocol;}

    public void setProxyHost(String value){this.proxyHost=value;}
    public void setProxyPort(int value){this.proxyPort=value;}
    public void setProxyProtocol(String value){this.proxyProtocol=value;}

    public String getUrl()
    {
        return upload_url;
    }

    public void setToken(String value)
    {
        this.token=value;
    }

    public DiawiRequest(String token, String proxyHost,int proxyPort, String proxyProtocol)
    {
        this.token=token;
        this.proxyHost=proxyHost;
        this.proxyPort=proxyPort;
        this.proxyProtocol=proxyProtocol;
    }

    public  DiawiJob sendReq(String fname) throws IOException {

            HttpClient httpclient = HttpClientBuilder.create().build();

            HttpPost httpPost = new HttpPost(upload_url);






            if ( !this.proxyHost.equals("")) {
                HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyProtocol);
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .build();

                httpPost.setConfig(config);
            }

            File uploadFile = new File(fname);

            FileBody uploadFilePart = new FileBody(uploadFile);
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("file", uploadFilePart);
            reqEntity.addPart("token", new StringBody(token));

            httpPost.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(httpPost);
            System.out.println(response.getStatusLine().getStatusCode());


            StringBuffer result = new StringBuffer();
            try {
                rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(),StandardCharsets.UTF_8));

                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
            }
            finally {
                rd.close();
            }
            Gson g = new Gson();
            DiawiJob j = g.fromJson(result.toString(), DiawiJob.class);

            if (j.job.equals(""))
                throw new IOException("Invalid job id. looks like upload step failed");

            return (j);


    }
    public class DiawiJob
    {
        public String job="";

        public DiawiJobStatus getStatus(String token, String proxyHost,int proxyPort, String proxyProtocol) throws Exception
        {

            CloseableHttpClient httpclient = HttpClients.createDefault();
            String responseBody;
            try {
                URI uri = new URIBuilder(status_url)
                        .addParameter("token", token)
                        .addParameter("job", job)
                        .build();
                HttpGet httpget = new HttpGet(uri);


                if ( !proxyHost.equals("")) {
                    HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyProtocol);
                    RequestConfig config = RequestConfig.custom()
                            .setProxy(proxy)
                            .build();

                    httpget.setConfig(config);
                }


                System.out.println("Executing request " + httpget.getRequestLine());

                // Create a custom response handler
                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                    @Override
                    public String handleResponse(
                            final HttpResponse response) throws ClientProtocolException, IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException("Unexpected response status: " + status);
                        }
                    }

                };
                 responseBody = httpclient.execute(httpget, responseHandler);
            } finally {
                httpclient.close();
            }
            Gson g = new Gson();
            DiawiJobStatus s = g.fromJson(responseBody, DiawiJobStatus.class);
            return (s);


        }

    }

    public class DiawiJobStatus
    {
        public int status;
        public String message;
        public String hash;
        public String link;

    }


}
