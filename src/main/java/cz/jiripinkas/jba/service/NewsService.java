package cz.jiripinkas.jba.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.jiripinkas.jba.entity.Configuration;
import cz.jiripinkas.jba.entity.NewsItem;
import cz.jiripinkas.jba.repository.NewsItemRepository;
import cz.jiripinkas.jba.util.MyUtil;
import cz.jiripinkas.jsitemapgenerator.RssItemBuilder;
import cz.jiripinkas.jsitemapgenerator.generator.RssGenerator;

@Service
public class NewsService {

	private static final int PAGE_SIZE = 10;

	@Autowired
	private NewsItemRepository newsItemRepository;

	@Autowired
	private ConfigurationService configurationService;

	public void save(NewsItem newsItem) {
		newsItem.setPublishedDate(new Date());
		if (newsItem.getShortName() == null || newsItem.getShortName().isEmpty()) {
			newsItem.setShortName(MyUtil.generatePermalink(newsItem.getTitle()));
		}
		newsItemRepository.save(newsItem);
	}

	public Page<NewsItem> findNews(int page) {
		return newsItemRepository.findAll(PageRequest.of(page, PAGE_SIZE, Direction.DESC, "publishedDate"));
	}
	
	public List<NewsItem> findAll() {
		return newsItemRepository.findAll();
	}

	@Transactional
	public NewsItem findOne(String shortName) {
		return newsItemRepository.findByShortName(shortName);
	}

	public String getFeed() {
		Configuration configuration = configurationService.find();
		Page<NewsItem> firstTenNews = findNews(0);
		RssGenerator rssGenerator = new RssGenerator(configuration.getChannelLink(), configuration.getChannelTitle(), configuration.getChannelDescription());
		for (NewsItem newsItem : firstTenNews.getContent()) {
			rssGenerator.addPage(
					new RssItemBuilder()
					.title(newsItem.getTitle())
					.description(newsItem.getShortDescription())
					.name("news/" + newsItem.getShortName())
					.pubDate(newsItem.getPublishedDate())
					.build()
					);
		}
		return rssGenerator.constructRss();
	}

	public void delete(int id) {
		newsItemRepository.deleteById(id);
	}

}
