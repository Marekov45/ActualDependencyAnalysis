package de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing;

import de.hhn.it.wolves.components.AnalysisResultConsumerPlugin;
import de.hhn.it.wolves.domain.*;
import de.hhn.it.wolves.plugins.actualdependencyanalyser.MavenDependencyAnalysisResult;
import de.hhn.it.wolves.plugins.actualdependencyanalyser.NodeDependencyAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActualDependencyAnalysisResultConsumer implements AnalysisResultConsumerPlugin {

    private static final String PLUGIN_NAME = "Actual dependency analysis RC";
    private static final Logger logger = LoggerFactory.getLogger(ActualDependencyAnalysisResultConsumer.class.getName());


    @Override
    public StatisticInformation processAnalysisResults(AnalysisResult analysisResult) {
        if ((analysisResult instanceof MavenDependencyAnalysisResult)) {
            logger.info("I got a Maven dependency analysis result. We will forward it to the correspondent statistic generator plugin!");
            return new MavenDependencyStatisticInformation(analysisResult, getUniqueName(),((MavenDependencyAnalysisResult) analysisResult).getAllMavenDependencies(),((MavenDependencyAnalysisResult) analysisResult).getUnusedMavenDependencies(),((MavenDependencyAnalysisResult) analysisResult).isMultiModule());
        } else if ((analysisResult instanceof NodeDependencyAnalysisResult)) {
            logger.info("I got a Nodejs dependency analysis result. We will forward it to the correspondent statistic generator plugin!");
            return new NodeDependencyStatisticInformation(analysisResult,getUniqueName(),((NodeDependencyAnalysisResult) analysisResult).getAllNodeDependencies(), ((NodeDependencyAnalysisResult) analysisResult).getUnusedNodeDependencies(),((NodeDependencyAnalysisResult) analysisResult).isMultiModule());
        } else {
            logger.error("I got an analysis result that I am not designed for!");
            return new StatisticInformationWithoutProcessing(analysisResult, getUniqueName());
        }

    }

    @Override
    public Class[] getAnalysisResultsThisPluginIsDesignedFor() {
        return new Class[]{MavenDependencyAnalysisResult.class, NodeDependencyAnalysisResult.class};
    }

    @Override
    public void init(FrameworkManager frameworkManager) {

    }

    @Override
    public String getUniqueName() {
        return PLUGIN_NAME;
    }

    @Override
    public void cleanUp() {

    }
}
