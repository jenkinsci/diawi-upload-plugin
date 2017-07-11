package org.jenkinsci.plugins.diawi;

/**
 * Created by salaheld on 18/06/2017.
 */

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DiawiRequest {


    private static final String USER_AGENT = "Mozilla/5.0";

    private static final String upload_url ="https://upload.diawi.com/";
    private static final String status_url="https://upload.diawi.com/status";
    private  String token;
    private  BufferedReader rd;

    public String getUrl()
    {
        return upload_url;
    }

    public void setToken(String value)
    {
        this.token=value;
    }

    public DiawiRequest(String token)
    {
        this.token=token;
    }

    public  DiawiJob sendReq(String fname) throws IOException {

            HttpClient httpclient = HttpClientBuilder.create().build();
            ;
            HttpPost httpPost = new HttpPost(upload_url);


            File uploadFile = new File(fname);

            FileBody uploadFilePart = new FileBody(uploadFile);
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("file", uploadFilePart);
            reqEntity.addPart("token", new StringBody(token));

            httpPost.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(httpPost);
            System.out.println(response.getStatusLine().getStatusCode());


            try {
            rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(),StandardCharsets.UTF_8));
            }
            finally {
                rd.close();
            }
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
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

        public DiawiJobStatus getStatus(String token) throws Exception
        {


                if (job.equals(""))
                    throw new IOException("Invalid job id. looks like upload step failed");


                System.out.println("job " + job);
                System.out.println("token " + token);

                URI uri = new URIBuilder(status_url)
                        .addParameter("token", token)
                        .addParameter("job", job)
                        .build();

                HttpClient httpclient = HttpClients.createDefault();
                ;
                HttpGet httpGet = new HttpGet(uri);

                HttpResponse response = httpclient.execute(httpGet);
                System.out.println(response.getStatusLine().getStatusCode());


                try {
                rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));}
                finally {
                    rd.close();
                }
                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                Gson g = new Gson();
                DiawiJobStatus s = g.fromJson(result.toString(), DiawiJobStatus.class);
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
