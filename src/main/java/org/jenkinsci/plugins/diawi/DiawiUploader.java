package org.jenkinsci.plugins.diawi;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

/**
 * Created by salaheld on 17/06/2017.
 */
public class DiawiUploader extends hudson.tasks.Builder implements SimpleBuildStep{

    private String token;
    private String fileName;

    @DataBoundConstructor
    public DiawiUploader(String token,String fileName)
    {
        this.token=token;
        this.fileName=fileName;
    }
    public String getToken()
    {
        return this.token;
    }
    public void setToken(String value)
    {
        this.token=value;
    }

    public String getFileName()
    {
        return this.fileName;
    }
    public void setFileName(String value)
    {
       this.fileName=value;
    }



    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        try {
            listener.getLogger().println(fileName+" is being uploaded ... ");

            DiawiRequest dr = new DiawiRequest(token);

            DiawiRequest.DiawiJob job= dr.sendReq(fileName);
            listener.getLogger().println("upload job is "+job.job);


            DiawiRequest.DiawiJobStatus S = job.getStatus(token);

            int max_trials=30;
            int i=0;

            while (S.status ==2001 && i<max_trials)
            {
                System.out.println("trying again");
                S = job.getStatus(token);
                i++;
            }


            listener.getLogger().println("status "+S.status);
            listener.getLogger().println("message "+S.message);


            listener.getLogger().println(fileName+" have been uploaded successfully to diawi ... ");


            if (S.status==2001)
                throw new Exception("Looks like upload job hanged. please login to Diawi.com and check the uplaod status");
            else if (S.status==4000)
                throw new Exception("Upload Failed, looks like you chose the wrong file");
            else if (S.status !=2000)
                throw new Exception("Unknown error. Upload failed");


            listener.getLogger().println("has "+S.hash);
            listener.getLogger().println("link "+S.link);


        }
        catch (Exception e)
        {
            listener.getLogger().print(e.getMessage());
            e.printStackTrace();
            throw new AbortException(e.getMessage());
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
