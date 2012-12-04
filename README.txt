Plugin voor Hudson om een diff te mailen over de verschillen tussen 2 junit test
runs.

Dit is een maven2 project en tegelijk ook een Eclipse project.
Handig maven commando's:
# Voor packagen
mvn clean compile package
# Voor opstarten van een testomgeving op localhost
mvn hpi:run
# Meer tips:
http://wiki.hudson-ci.org/display/HUDSON/Plugin+tutorial

---
Als je de plugin wilt upgraden, gaat dat niet helemaal vlekkeloos (2009-12-17)
met hudson zelf. Het versie nummer in pom.xml wordt ook niet in MANIFEST.MF bij
plugin-version gezet.

Ik heb op linux5 hudson gestopt, van de harde schijf
/data/hudson/workspace/plugins/junitdiff gegooid en hem opnieuw geinstalleerd.

