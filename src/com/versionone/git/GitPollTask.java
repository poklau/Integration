package com.versionone.git;

import com.versionone.git.configuration.Configuration;
import com.versionone.git.configuration.GitSettings;
import com.versionone.git.storage.DbStorage;
import com.versionone.git.storage.IDbStorage;
import org.apache.log4j.Logger;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

public class GitPollTask extends TimerTask {
    private static final Logger LOG = Logger.getLogger("GitIntegration");
    private final Configuration configuration;
    private Map<GitSettings, GitService> gitServices = new HashMap<GitSettings, GitService>();
    private final static String REPOSITORY_DIRECTORY_PATTERN = "%s/%sRepo";
    private final IVersionOneConnector v1Connector;

    GitPollTask(Configuration configuration) throws VersionOneException, NoSuchAlgorithmException {
        this.configuration = configuration;

        v1Connector = new VersionOneConnector();
        v1Connector.connect(configuration.getVersionOneSettings());

        cleanupLocalDirectory();
        serviceInitialize();
    }

    public void serviceInitialize() throws NoSuchAlgorithmException {
        int amountOfServices = configuration.getGitSettings().size();
        LOG.info(String.format("Creating %s Git service(s)...", amountOfServices));

        for (int gitRepositoryIndex = 0; gitRepositoryIndex < amountOfServices; gitRepositoryIndex ++) {
            GitSettings gitSettings = configuration.getGitSettings().get(gitRepositoryIndex);
            String repositoryId = Utilities.getRepositoryId(gitSettings);

            IChangeSetWriter changeSetWriter = new ChangeSetWriter(configuration, v1Connector, gitSettings.getLink());
            GitService service = getGitService(gitSettings, repositoryId, changeSetWriter);

            if (service != null) {
                gitServices.put(gitSettings, service);
            }
        }

        LOG.info("Git services created successfully");
    }

    @Override
    public void run() {
        LOG.info("Checking for new changes...");

        for (GitSettings settings : gitServices.keySet()) {
            LOG.info("Checking " + settings.getRepositoryPath());
            processRepository(gitServices.get(settings));
        }
        LOG.info("Check for new changes complete");
    }

    private void processRepository(GitService service) {
        try {
            service.onInterval();
        } catch(GitException ex) {
            LOG.fatal("Git service failed: " + ex.getInnerException().getMessage());
        } catch (VersionOneException ex) {
            LOG.fatal("VersionOne service failed: " + ex.getInnerException().getMessage());
        } catch (Exception ex) {
            LOG.fatal("Failed to process repository. It is possible that the integration is not configured properly.", ex);
        }
    }

    private GitService getGitService(GitSettings gitSettings, String repositoryId, IChangeSetWriter changeSetWriter) {
        IDbStorage storage = new DbStorage();

        IGitConnector gitConnector = new GitConnector(
                gitSettings,
                String.format(REPOSITORY_DIRECTORY_PATTERN, configuration.getLocalDirectory(), repositoryId),
                configuration.getReferenceExpression(),
                configuration.isAlwaysCreate(),
                storage, repositoryId);
        GitService service = new GitService(storage, gitConnector, changeSetWriter, repositoryId);

        LOG.info(String.format("Initializing Git Service for %s...", gitSettings.getRepositoryPath()));
        return initializeGitService(service) ? service : null;
    }

    private boolean initializeGitService(GitService service) {
        try {
            service.initialize();
        } catch (GitException ex) {
            System.out.println("Fail: " + ex.getInnerException().getMessage());
            LOG.fatal("Git service initialize failed: " + ex.getInnerException().getMessage());
            return false;
        }

        return true;
    }

    private void cleanupLocalDirectory() {
        LOG.debug(String.format("Resetting local directory %s...", configuration.getLocalDirectory()));

        if (!Utilities.deleteDirectory(new File(configuration.getLocalDirectory()))) {
            LOG.warn(configuration.getLocalDirectory() + " couldn't be reset, possibly due to this being the first time the service has been run");
        }

        boolean result = new File(configuration.getLocalDirectory()).mkdir();

        if (!result) {
            LOG.fatal(configuration.getLocalDirectory() + " couldn't be created");
            System.exit(-1);
        }
    }
}
