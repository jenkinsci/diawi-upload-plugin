package org.jenkinsci.plugins.diawi;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.model.ParameterDefinition;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
//import java.util.Base64;
import org.apache.commons.codec.binary.Base64;


/**
 * Created by salaheld on 17/06/2017.
 */
public class DiawiUploader extends hudson.tasks.Builder implements SimpleBuildStep{

    private String token;
    private String fileName;
    private String proxyHost;
    private int proxyPort=8080;
    private String proxyProtocol="http";

    @DataBoundConstructor
    public DiawiUploader(String token,String fileName, String proxyHost, int proxyPort, String proxyProtocol)
    {
        
        //byte[] decodedBytes = Base64.getDecoder().decode(token);
        //this.token = new String(decodedBytes);

        byte[] valueDecoded = Base64.decodeBase64(token.getBytes());
        this.token = new String(valueDecoded);

        //this.token=token;
        this.fileName=fileName;
        this.proxyHost=proxyHost;
        this.proxyPort=proxyPort;
        this.proxyProtocol=proxyProtocol;
    }
    public void setToken(String value)
    {
        this.token=value;
    }
    public void setFileName(String value)
    {
        this.fileName=value;
    }
    public void setProxyHost(String value)
    {
        this.proxyHost=value;
    }
    public void setProxyPort(int value)
    {
        this.proxyPort=value;
    }
    public void setProxyProtocol(String value)
    {
        this.proxyProtocol=value;
    }

    public String getToken()
    {
        return this.token;
    }
    public String getFileName()
    {
        return this.fileName;
    }
    public String getProxyHost()
    {
        return this.proxyHost;
    }
    public int getProxyPort()
    {
        return this.proxyPort;
    }
    public String getProxyProtocol()
    {
        return this.proxyProtocol;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        for ( String file : fileName.split(",")) {
            try {

                file = file.trim();
                DiawiRequest dr = new DiawiRequest(token, proxyHost, proxyPort, proxyProtocol);

                String path = workspace.child(file).toURI().getPath();
                listener.getLogger().println(path + " is being uploaded ... ");

                DiawiRequest.DiawiJob job = dr.sendReq(path);

                listener.getLogger().println("upload job is " + job.job);


                listener.getLogger().println("used proxy host is " + proxyHost);
                listener.getLogger().println("used proxy port is " + proxyPort);
                listener.getLogger().println("used proxy protocol is " + proxyProtocol);

                DiawiRequest.DiawiJobStatus S = job.getStatus(token, proxyHost, proxyPort, proxyProtocol);

                int max_trials = 30;
                int wait_time = 3000;
                int i = 0;

                while (S.status == 2001 && i < max_trials) {
                    System.out.println("trying again");
                    S = job.getStatus(token, proxyHost, proxyPort, proxyProtocol);
                    Thread.sleep(wait_time);
                    i++;
                }


                listener.getLogger().println("status " + S.status);
                listener.getLogger().println("message " + S.message);


                listener.getLogger().println(path + " have been uploaded successfully to diawi ... ");


                if (S.status == 2001)
                    throw new Exception("Looks like upload job hanged. please login to Diawi.com and check the uplaod status");
                else if (S.status == 4000)
                    throw new Exception("Upload Failed, looks like you chose the wrong file");
                else if (S.status != 2000)
                    throw new Exception("Unknown error. Upload failed");


                listener.getLogger().println("hash " + S.hash);
                listener.getLogger().println("link " + S.link);


                String saveLinkTo = workspace.toURI().getPath();
                writelink(saveLinkTo + "/" + file + ".diawilink", S.link);


            } catch (Exception e) {
                listener.getLogger().print(e.getMessage());
                e.printStackTrace();
                throw new AbortException(e.getMessage());
            }
        }

    }


    public static void writelink(String filename, String content) {

        BufferedWriter bw = null;
        OutputStreamWriter fw = null;

        try {


            fw = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8);
            bw = new BufferedWriter(fw);
            bw.write(content);

            System.out.println("Done");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }

    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType == FreeStyleProject.class;
        }

        @Override
        public String getDisplayName() {
            return "Diawi Upload Step";
        }
    }


}
