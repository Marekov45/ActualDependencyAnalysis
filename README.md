# ActualDependencyAnalysis
Plugin, das auf globaler Ebene analysiert, ob vorhandene Open-Source Komponenten in Projekten überhaupt verwendet werden oder nur noch als Altlast in den Projekten verweilen.

## Motivation
Im Rahmen seiner Masterarbeit untersuchte Maximilian Westers die Verwendung von Bibliotheken mit bekannten Schwachstellen. Dieses Problem wird auch in der [OWASP Top 10 Liste der kritischsten Sicherheitsrisiken für Webanwendungen](https://www.owasp.org/images/7/72/OWASP_Top_10-2017_%28en%29.pdf.pdf) auf Platz A9
unter dem Namen "Using Components with Known Vulnerabilities" näher beschrieben. Das Ziel war es eine repräsentative Analyse für die globale Einschätzung der Bedrohung
durch solche Schwachstellen zu erstellen. Dafür wurde ein Plugin-basiertes Framework namens Wolves entwickelt, mit dem Analysen einer großen Anzahl von Projekten nach dem Prinzip 
"Crawling" --> "Analysis" --> "Processing" --> "Statistic" durchgeführt werden können. Jedoch wurde für valide Schlussfolgerungen nicht beachtet, ob
die Komponenten mit Schwachstellen in dem analysierten Projekt auch genutzt werden und das Projekt somit auch selbst anfällig ist. Im schlechtesten Fall wurden Daten inkludiert,
die ein Projekt zwar als anfällig darstellen, in Wirklichkeit wird die gefundene Komponente aber nie genutzt und ist somit nur noch als Altlast in dem Projekt vorhanden.
In solch einem Fall wäre also trotz der Existenz einer Komponente mit bekannten Schwachstellen das Projekt selbst nicht verwundbar.
Das entwickelte Plugin untersucht deshalb auf globaler Ebene, ob Open-Source Bibliotheken in Projekten tatsächlich Verwendung finden oder nicht, um so im weiteren Verlauf
einen verbesserten Analyseprozess zu gewährleisten.
