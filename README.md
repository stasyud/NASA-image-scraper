# Image scraper

Fun project intended to download hi-res images of US space program available from [one](http://grin.hq.nasa.gov) of the NASA websites.
Written on [groovy](http://www.groovy-lang.org/index.html)

To run project execute following commands from the root of the project:
```
mvn clean package
```

Then you can run the result jar file by running following command
```
java -jar ./target/NASA-images-scraper-1.0-jar-with-dependencies.jar
```

After successful launch, application starts downloading images to the folder DESTINATION_FOLDER specified in the main class. 

Please be aware: size of downloaded images are more than 10GB. 

Requirements: application requires java 8 (jre 1.8 or jdk 1.8) installed on your machine.
