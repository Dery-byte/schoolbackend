## BUILD STAGE
#FROM maven:3.9.4-eclipse-temurin-22 AS build
#WORKDIR /app
#COPY pom.xml .
#COPY . .
#RUN mvn clean package -DskipTests
#
## RUNTIME STAGE
#FROM eclipse-temurin:22-jdk-alpine
#ENV JAVA_OPTS="--enable-preview"
#WORKDIR /app
#COPY --from=build /app/target/school-docker.jar school-docker.jar
#ENTRYPOINT ["java", "--enable-preview", "-jar", "school-docker.jar"]




# BUILD STAGE
#FROM maven:3.9.4-eclipse-temurin-21 AS build
#WORKDIR /app
#COPY pom.xml .
#COPY . .
#RUN mvn clean package -DskipTests
#
## RUNTIME STAGE
#FROM eclipse-temurin:21-jre-jammy
#WORKDIR /app
#COPY --from=build /app/target/school-docker.jar school-docker.jar
#ENTRYPOINT ["java", "--enable-preview", "-jar", "school-docker.jar"]




#FROM maven:3.9.4-amazoncorretto-21-debian AS build
##FROM maven:3.9.4-eclipse-temurin-22 AS build
#WORKDIR /app
#COPY pom.xml .
##RUN mvn dependency:go-offline
#COPY . .
#RUN mvn clean package -DskipTests
#
#FROM openjdk:21-slim
#ENV JAVA_OPTS="--enable-preview"
#WORKDIR /app
#COPY --from=build /app/target/school-docker.jar school-docker.jar
#ENTRYPOINT ["java",  "--enable-preview", "-jar", "school-docker.jar"]


# BUILD STAGE
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY . .

RUN rm -f /app/src/main/java/com/alibou/book/Entity/ResultDetail.java
RUN mvn clean package -DskipTests

# RUNTIME STAGE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/school-docker.jar school-docker.jar
ENTRYPOINT ["java", "--enable-preview", "-jar", "school-docker.jar"]