FROM eclipse-temurin:21-jdk
WORKDIR /
ADD target/mazev-server-1.0-SNAPSHOT-allinone.jar mazev-server-1.0-SNAPSHOT-allinone.jar
EXPOSE 8080
CMD java -XX:+PrintFlagsFinal -Xmx=450m -jar mazev-server-1.0-SNAPSHOT-allinone.jar