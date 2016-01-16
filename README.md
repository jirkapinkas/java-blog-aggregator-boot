<h1>Java Blog Aggregator: Boot</h1>

<p>This application is a successor of original <a href="https://github.com/jirkapinkas/java-blog-aggregator">Java Blog Aggregator</a>. It uses Spring boot and runs as a standalone JAR file, JSP and Tiles were switched to Thymeleaf and I organized JavaScript much better.</p>

<h2>Development:</h2>

<p>
	Run with: <code>-Dspring.profiles.active="dev"</code>
</p>

<p>
	Will run on <a href="http://localhost:8080">http://localhost:8080</a> with embedded HSQL database, username / password: admin / admin
</p>

<h2>Production:</h2>

<p>
	Packaging: <code>mvn clean package -P prod</code>
</p>

<p>
	Run: <code>java -jar target/java-blog-aggregator.jar --spring.config.location=file:prod.properties --logging.config=file:logback-prod.xml</code>
</p>

<h3>sample prod.properties contents:</h3>

<p>
<code>
spring.profiles.active=prod<br />
server.port=8081<br />
spring.datasource.url=jdbc:postgresql://localhost:5432/DB_NAME<br />
spring.datasource.username=USERNAME<br />
spring.datasource.password=PASSWORD<br />
spring.datasource.driverClassName=org.postgresql.Driver<br />
</code>
</p>
