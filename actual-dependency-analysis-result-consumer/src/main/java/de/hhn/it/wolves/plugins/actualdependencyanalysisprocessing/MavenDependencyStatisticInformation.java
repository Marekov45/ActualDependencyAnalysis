package de.hhn.it.wolves.plugins.actualdependencyanalysisprocessing;

import de.hhn.it.wolves.domain.AnalysisResult;
import de.hhn.it.wolves.domain.StatisticInformation;

import org.apache.maven.artifact.Artifact;

import java.util.List;

public class MavenDependencyStatisticInformation extends StatisticInformation {

//mein plugin analysiert die Anzahl ungenutzter Bibliotheken von Projekten die Schwachstellen beinhalten,
// aber die ungenutzten Bibliotheken müssen nicht alle Schwachstellen enthalten
//es muss geschaut werden ob die ungenutzte Abhängigkeit auch tatsächlich eine Schwachstelle ist
// --> filtern der unused dependencies über vergleich mit schwachstellendatenbank?

    private final List<Artifact> allForwardedMavenDependencies ;
    private final List<Artifact> unusedForwardedMavenDependencies ;

    public MavenDependencyStatisticInformation(AnalysisResult analysisResult, String processedPluginName, List<Artifact> allForwardedMavenDependencies,List<Artifact> unusedForwardedMavenDependencies) {
        super(analysisResult, processedPluginName);
        this.allForwardedMavenDependencies = allForwardedMavenDependencies;
        this.unusedForwardedMavenDependencies = unusedForwardedMavenDependencies;
    }
    public List<Artifact> getAllForwardedMavenDependencies() {
        return allForwardedMavenDependencies;
    }

    public List<Artifact> getUnusedForwardedMavenDependencies() {
        return unusedForwardedMavenDependencies;
    }
}
