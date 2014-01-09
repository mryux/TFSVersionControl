1: run build.bat to generate Tfs.jar
2: copy Tfs.jar to Plugins folder of Fitnesse
3: create plugins.properties in Fitnesse folder, and add following line to it:
	VersionsController=fitnesse.Tfs.TFSVersionsController
4: create TFS_Path environment variable which points to the folder contains TF.exe, such as C:\Program Files\Microsoft Visual Studio 12.0\Common7\IDE\.
5: start fitnesse with following command:
	java -cp Plugins/Tfs.jar -jar fitnesse-standalone.jar -p 8280 -e 0

enjoy!