package cz.jiripinkas.jba.controller;

import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.ConfigurationService;
import cz.jiripinkas.jba.service.NewsService;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import cz.jiripinkas.jsitemapgenerator.robots.RobotsRule;
import cz.jiripinkas.jsitemapgenerator.robots.RobotsTxtGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SitemapController {
	
	@Autowired
	private ConfigurationService configurationService;
	
	@Autowired
	private BlogService blogService;
	
	@Autowired
	private NewsService newsService;
	
	@ResponseBody
	@RequestMapping(path = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getRobots() {
		return RobotsTxtGenerator.of(configurationService.find().getChannelLink())
				.addSitemap("sitemap.xml")
				.addRule(RobotsRule.builder().userAgentAll().allowAll().build())
				.toString();
	}

	@ResponseBody
	@RequestMapping(path = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String getSitemap() {
		return SitemapGenerator.of(configurationService.find().getChannelLink())
				.addPage(WebPage.builder().nameRoot().build())
				.addPage(WebPage.of("blogs"))
				.addPage(WebPage.of("news"))
				.defaultDir("blog")
				.addPages(blogService.findAll(false), blog -> WebPage.of(blog.getShortName()))
				.defaultDir("news")
				.addPages(newsService.findAll(), newsItem -> WebPage.of(newsItem.getShortName()))
				.toString();
	}

}
