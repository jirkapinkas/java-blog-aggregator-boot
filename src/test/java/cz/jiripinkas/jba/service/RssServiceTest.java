package cz.jiripinkas.jba.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.entity.Item;
import cz.jiripinkas.jba.exception.RssException;
import cz.jiripinkas.jba.exception.UrlException;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RssServiceTest {

	private RssService rssService;

	@Mock
	private CloseableHttpClient httpClient;

	@Mock
	private CloseableHttpResponse httpResponse;

	@Before
	public void setUp() throws Exception {
		rssService = new RssService();
		rssService.setHttpClient(httpClient);
	}

	@Test
	public void testGetItemsFileJavaVids() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/javavids.xml", true, null, new HashMap<String, Object>());
		assertEquals(10, items.size());
		Item firstItem = items.get(0);
		assertEquals("How to generate web.xml in Eclipse", firstItem.getTitle());
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals("23 03 2014 09:01:34", simpleDateFormat.format(firstItem.getPublishedDate()));
	}

	@Test
	public void testGetItemsFileSpring() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/spring.xml", true, null, new HashMap<String, Object>());
		assertEquals(20, items.size());
		Item firstItem = items.get(0);
		assertEquals("Spring Boot 1.0.1.RELEASE Available Now", firstItem.getTitle());
		assertEquals("https://spring.io/blog/2014/04/07/spring-boot-1-0-1-release-available-now", firstItem.getLink());
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals("07 04 2014 09:14:00", simpleDateFormat.format(firstItem.getPublishedDate()));
		if (!firstItem.getDescription().startsWith("Spring Boot 1.0.1.RELEASE is available")) {
			fail("description does not match");
		}
	}

	@Test
	public void testGetItemsFileHibernate() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/hibernate.xml", true, null, new HashMap<String, Object>());
		assertEquals(14, items.size());
		Item firstItem = items.get(0);
		assertEquals("Third milestone on the path for Hibernate Search 5", firstItem.getTitle());
		assertEquals("http://in.relation.to/Bloggers/ThirdMilestoneOnThePathForHibernateSearch5", firstItem.getLink());
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals("04 04 2014 16:20:32", simpleDateFormat.format(firstItem.getPublishedDate()));
		if (!firstItem.getDescription().startsWith("Version 5.0.0.Alpha3 is now available")) {
			fail("description does not match");
		}
	}

	@Test
	public void testPullLinks() {
		String inputText = "Hibernate ORM 4.2.12.Final was just released! Please see the full changelog for more information: https://hibernate.atlassian.net/secure/ReleaseNote.jspa?projectId=10031&version=16350. "
				+ "Maven Central: http://repo1.maven.org/maven2/org/hibernate/hibernate-core (should update in a couple of days) "
				+ "SourceForge: www.sourceforge.net/projects/hibernate/files/hibernate4 "
				+ "JBoss Nexus: ftp://repository.jboss.org/nexus/content/groups/public/org/hibernate "
				+ " mailto:test@email.com";
		ArrayList<String> links = rssService.pullLinks(inputText);
		assertEquals(5, links.size());
		assertEquals(links.get(0), "https://hibernate.atlassian.net/secure/ReleaseNote.jspa?projectId=10031&version=16350");
		assertEquals(links.get(1), "http://repo1.maven.org/maven2/org/hibernate/hibernate-core");
		assertEquals(links.get(2), "www.sourceforge.net/projects/hibernate/files/hibernate4");
		assertEquals(links.get(3), "ftp://repository.jboss.org/nexus/content/groups/public/org/hibernate");
		assertEquals(links.get(4), "mailto:test@email.com");
	}

	@Test
	public void testCleanTitle() {
		assertEquals("test", rssService.cleanTitle("   test   "));
		assertEquals("Pat & Mat", rssService.cleanTitle("Pat & Mat"));
		assertEquals("bla bla", rssService.cleanTitle("[OmniFaces utilities 2.0] bla bla"));
		assertEquals("Thank You", rssService.cleanTitle("Blog Post: Thank You"));
		assertEquals("return \"*\"?", rssService.cleanTitle("return “*”?"));
		assertEquals("return \"*\"?", rssService.cleanTitle("return &#8220;*&#8221;?"));
		assertEquals("link:", rssService.cleanTitle("link: <a href='http://something.com'></a>"));
		assertEquals("script:", rssService.cleanTitle("script: <script><!-- alert('hello'); --></script>"));
	}

	@Test
	public void testGetRssDate() throws ParseException {
		assertEquals("Sun Mar 23 09:01:34 CET 2014", rssService.getRssDate("Sun, 23 Mar 2014 08:01:34 +0000").toString());
		assertEquals("Sun Mar 23 00:00:00 CET 2014", rssService.getRssDate("Sun, 23 Mar 2014").toString());
		assertEquals("Sun Mar 23 00:00:00 CET 2014", rssService.getRssDate("23 Mar 2014").toString());
		assertEquals("Thu Mar 12 00:00:00 CET 2015", rssService.getRssDate("2015-03-12").toString());
		assertEquals("Sun Jan 25 21:00:00 CET 2015", rssService.getRssDate("25 Jan 2015 20:00:00 GMT").toString());
		assertEquals("Sun Mar 18 00:00:00 CET 2012", rssService.getRssDate("2012.03.18").toString());
		assertEquals("Thu Mar 24 15:57:00 CET 2016", rssService.getRssDate("Mar 24, 2016 9:57:00 am EST").toString());
		assertEquals("Sat Sep 24 16:57:00 CEST 2016", rssService.getRssDate("Sept 24, 2016 9:57:00 am EST").toString());
		assertEquals("Wed Feb 23 15:40:00 CET 2011", rssService.getRssDate("Wed, Feb 23, 2011 09:40:00 AM EST").toString());
		assertEquals("Thu Sep 16 15:55:00 CEST 2010", rssService.getRssDate("Thu, Sep 16, 2010 8:55:00 EST").toString());
		assertEquals("Tue Sep 25 18:30:00 CEST 2007", rssService.getRssDate("Tue, 25 Sep 2007 11:30:00 EST").toString());
		assertEquals("Tue Sep 25 18:30:00 CEST 2007", rssService.getRssDate(" Tue, 25 Sep 2007 11:30:00 EST").toString());
	}

	@Test
	public void testFixDate() {
		String fixDate = rssService.fixDate("<guid>http://www.jsfcentral.com/listings/R221940</guid><pubDate>    25 Jan 2015 20:00:00 GMT   </pubDate><description>");
		assertEquals("<guid>http://www.jsfcentral.com/listings/R221940</guid><pubDate>25 Jan 2015 20:00:00 GMT</pubDate><description>", fixDate);
	}

	@Test
	public void testCleanDescription() {
		assertEquals(
				"this is loooooooooooooooooooooooo ooooooooooooooooooooooooo ooooooong description loooooooooooooooooooooooo ooooooooong once more looooooooo...",
				rssService
						.cleanDescription("this is loooooooooooooooooooooooooooooooooooooooooooooooooooooooong description looooooooooooooooooooooooooooooooong once more looooooooooooooooooooooooooooooooong"));
		assertEquals("JVM is an abbreviated form of Java Virtual Machine", rssService.cleanDescription("RSS from Javabeginnerstutorial.com JVM is an abbreviated form of Java Virtual Machine"));
		assertEquals("test this is strong", rssService.cleanDescription("test <strong>this is strong</strong>"));
		assertEquals("test this is strong", rssService.cleanDescription("test &lt;strong&gt;this is strong&lt;/strong&gt;"));
		assertEquals("test this is strong", rssService.cleanDescription("<![CDATA[test &lt;strong&gt;this is strong&lt;/strong&gt;]]>"));
		assertEquals("stupid description", rssService.cleanDescription("~~~~~~~~ stupid description ~~~~~~~~"));
		assertEquals(
				"This tutorial will show example codes on how to convert Java String To Long. This is a common scenario when programming with Core Java. [......",
				rssService
						.cleanDescription("<![CDATA[ This tutorial will show example codes on how to convert &lt;strong&gt;Java String To Long&lt;/strong&gt;. This is a common scenario when programming with Core Java. <a href='http://javadevnotes.com/java-string-to-long-examples' class='excerpt-more'>[...]</a> ]]>"));
		assertEquals("test", rssService.cleanDescription("test <script> alert('hello')</script>"));
		assertEquals("it's working", rssService.cleanDescription("it's working"));
		assertEquals("Hello hello hello! It's time", rssService.cleanDescription("Hello hello hello! It&#8217;s time"));
		assertEquals("The following were the five most viewed presentation on in the year 2015", rssService.cleanDescription("Tweet The following were the five most viewed presentation on in the year 2015"));
		assertEquals("The following were the five most viewed presentation on in the year 2015", rssService.cleanDescription("TweetThe following were the five most viewed presentation on in the year 2015"));
		assertEquals("Tweet something", rssService.cleanDescription("Tweet something"));
		assertEquals("Something", rssService.cleanDescription("TweetSomething"));
		assertEquals("Tweetsomething", rssService.cleanDescription("Tweetsomething"));
		assertEquals("I'm honored to deliver", rssService.cleanDescription("I’m honored to deliver"));
		assertEquals("I couldn't think of anything", rssService.cleanDescription("I couldn&#039;t think of anything"));
		assertEquals("Unless you", rssService.cleanDescription("TL;DR Unless you"));
		assertEquals("Unless you", rssService.cleanDescription("TLDR Unless you"));
		assertEquals("Unless you", rssService.cleanDescription("TLDR: Unless you"));
		assertEquals("Unless you", rssService.cleanDescription("TLDR; Unless you"));
		assertEquals("Well, if this would be", rssService.cleanDescription("Preface Well, if this would be"));
		assertEquals("Cyber technology couldn't", rssService.cleanDescription("Cyber technology couldn’t"));
		assertEquals("return \"*\"?", rssService.cleanDescription("return “*”?"));
		assertEquals("return \"*\"?", rssService.cleanDescription("return &#8220;*&#8221;?"));
		assertEquals("At first glance", rssService.cleanDescription("Share this post: At first glance"));
		assertEquals("At first glance", rssService.cleanDescription("Share this post: At first glance ...Read More"));
		assertEquals("At first glance", rssService.cleanDescription("Share this post: At first glance Read More"));
		assertEquals("At first glance, we get excited about lambdas. But soon, as we begin to program in Java 8, we begin to realize", rssService.cleanDescription("Share this post: At first glance, we get excited about lambdas. But soon, as we begin to program in Java 8, we begin to realize...<p class=\"readmore\"><a class=\"more-btn\" href=\"http://virtualjug.com/streams-the-real-powerhouse-in-java-8/\">Read More</a>"));
		assertEquals("Yesterday page views of my blog touched the magical figure of 1 Million.", rssService.cleanDescription("RSS content Yesterday page views of my blog touched the magical figure of 1 Million."));
		assertEquals("", rssService.cleanDescription("Dilbert readers - Please visit Dilbert.com to read this feature. Due to changes with our feeds, we are now making this RSS feed"));
	}

	@Test
	public void testGetItemsFileInstanceofJavaPublishedDate() throws RssException, ClientProtocolException, IOException {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/instanceofjava.xml", true, null, new HashMap<String, Object>());
		Item firstItem = items.get(0);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MM yyyy HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals("22 02 2015 13:35:00", simpleDateFormat.format(firstItem.getPublishedDate()));
		assertEquals("http://www.instanceofjava.com/2015/02/java-8-interface-static-default-methods.html", firstItem.getLink());
	}

	@Test
	public void testGetItemsFileBaeldungFeedburnerOrigLink() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/baeldung.xml", true, null, new HashMap<String, Object>());
		Item firstItem = items.get(0);
		assertEquals("http://www.baeldung.com/spring-security-oauth2-authentication-with-reddit", firstItem.getLink());
	}

	@Test
	public void testGetItemsFileReddit() throws Exception {
		mockHttpClient200Status();
		Blog blog = new Blog();
		blog.setMinRedditUps(30);
		List<Item> items = rssService.getItems("src/test/resources/test-rss/reddit.com.json", true, blog, new HashMap<String, Object>());
		assertEquals(4, items.size());
		Item firstItem = items.get(0);
		
//		assertEquals("http://www.baeldung.com/spring-security-oauth2-authentication-with-reddit", firstItem.getLink());
	}

	@Test
	public void testRedirect() throws Exception {
		@SuppressWarnings("resource")
		CloseableHttpResponse closeableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
		HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		HttpClientContext httpClientContext = Mockito.mock(HttpClientContext.class);
		Mockito.when(httpClientContext.getRedirectLocations()).thenReturn(Arrays.asList(new URI("http://www.java-skoleni.cz/kurz/java")));
		Mockito.when(statusLine.getStatusCode()).thenReturn(200);
		Mockito.when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
		Mockito.when(httpClient.execute(Mockito.any(), (HttpContext) Mockito.any())).thenReturn(closeableHttpResponse);
		String realLink = rssService.getRealLink("http://www.java-skoleni.cz/skoleni.php?id=java", httpClientContext);
		assertEquals("http://www.java-skoleni.cz/kurz/java", realLink);
	}

	public void test404() throws Exception {
		try {
			mockHttpClientStatus(404);
			rssService.getRealLink("http://www.java-skoleni.cz/xxxx", HttpClientContext.create());
			fail();
		} catch (UrlException ex) {
			// ignore expected exception
		}
	}

	private void mockHttpClient200Status() throws IllegalStateException, IOException {
		mockHttpClientStatus(200);
	}

	private void mockHttpClientStatus(int returnStatus) throws IllegalStateException, IOException {
		@SuppressWarnings("resource")
		CloseableHttpResponse closeableHttpResponse = Mockito.mock(CloseableHttpResponse.class);
		HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(statusLine.getStatusCode()).thenReturn(returnStatus);
		Mockito.when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
		Mockito.when(httpClient.execute(Mockito.any(), (HttpContext) Mockito.any())).thenReturn(closeableHttpResponse);
	}

	@Test
	public void test200Ampersand() throws Exception {
		mockHttpClient200Status();
		String realLink = rssService.getRealLink("http://www.phoronix.com/scan.php?page=news_item&amp;px=LWJGL-Vulkan-Java", HttpClientContext.create());
		assertEquals("http://www.phoronix.com/scan.php?page=news_item&px=LWJGL-Vulkan-Java", realLink);
	}

	@Test
	public void test200() throws Exception {
		mockHttpClient200Status();
		String realLink = rssService.getRealLink("http://www.java-skoleni.cz", HttpClientContext.create());
		assertEquals("http://www.java-skoleni.cz", realLink);
	}

	@Test
	public void testWhitespaces() throws Exception {
		mockHttpClient200Status();
		String realLink = rssService.getRealLink("   http://www.java-skoleni.cz   ", HttpClientContext.create());
		assertEquals("http://www.java-skoleni.cz", realLink);
	}

	@Test
	public void testAtomAlternate() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/knitelius.xml", true, null, new HashMap<String, Object>());
		Item firstItem = items.get(0);
		assertEquals("http://www.knitelius.com/2015/03/03/jsf-2-ajaxsubmit-issues-with-conversationscoped-beans/", firstItem.getLink());
	}

	@Test
	public void testDfetter() throws Exception {
		mockHttpClient200Status();
		List<Item> items = rssService.getItems("src/test/resources/test-rss/dfetter.xml", true, null, new HashMap<String, Object>());
		assertEquals("What time was it? This is a question that may not always be easy to answer, even with the excellent TIMESTAMPTZ data type. While it stores t...", items.get(0).getDescription());
	}
	
	@Test
	public void testFixRealLink() {
		assertEquals("http://www.javaworld.com/article/3033916/open-source-tools/github-apologizes-for-ignoring-community-concerns.html", rssService.fixRealLink("http://www.javaworld.com/article/3033916/open-source-tools/github-apologizes-for-ignoring-community-concerns.html#tk.rss_all"));
		assertEquals("https://dzone.com/articles/defensive-programming-via-validating-decorators", rssService.fixRealLink("https://dzone.com/articles/defensive-programming-via-validating-decorators?utm_medium=feed&utm_source=feedpress.me&utm_campaign=Feed%3A+dzone%2Fjava"));
		assertEquals("http://www.infoq.com/articles/Easily-Create-Java-Agents-with-ByteBuddy", rssService.fixRealLink("http://www.infoq.com/articles/Easily-Create-Java-Agents-with-ByteBuddy?utm_campaign=infoq_content&utm_source=infoq&utm_medium=feed&utm_term=Java"));
		assertEquals("http://techblog.bozho.net/the-astonishingly-low-quality-of-scientific-code/", rssService.fixRealLink("http://techblog.bozho.net/the-astonishingly-low-quality-of-scientific-code/?utm_source=rss&utm_medium=rss&utm_campaign=the-astonishingly-low-quality-of-scientific-code"));
		assertEquals("http://movingfulcrum.com/jetbrains-the-unicorn-silicon-valley-doesnt-like-to-talk-about/", rssService.fixRealLink("http://movingfulcrum.com/jetbrains-the-unicorn-silicon-valley-doesnt-like-to-talk-about/?utm_content=bufferfdf4d&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer"));
	}

}
