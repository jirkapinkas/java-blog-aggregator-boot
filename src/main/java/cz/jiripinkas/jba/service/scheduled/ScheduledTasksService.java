package cz.jiripinkas.jba.service.scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import cz.jiripinkas.jba.dto.ItemDto;
import cz.jiripinkas.jba.entity.Blog;
import cz.jiripinkas.jba.entity.Category;
import cz.jiripinkas.jba.entity.Configuration;
import cz.jiripinkas.jba.entity.NewsItem;
import cz.jiripinkas.jba.repository.BlogRepository;
import cz.jiripinkas.jba.repository.ItemRepository;
import cz.jiripinkas.jba.repository.NewsItemRepository;
import cz.jiripinkas.jba.service.AllCategoriesService;
import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.CategoryService;
import cz.jiripinkas.jba.service.ConfigurationService;
import cz.jiripinkas.jba.service.ItemService;
import cz.jiripinkas.jba.service.ItemService.MaxType;
import cz.jiripinkas.jba.service.ItemService.OrderType;
import cz.jiripinkas.jba.service.NewsService;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Service
public class ScheduledTasksService {

	private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);

	@Autowired
	private BlogRepository blogRepository;

	@Autowired
	private BlogService blogService;

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private NewsItemRepository newsItemRepository;

	@Autowired
	private NewsService newsService;

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private AllCategoriesService allCategoriesService;

	/**
	 * For each blog retrieve latest items and store them into database.
	 */
	// 1 hour = 60 seconds * 60 minutes * 1000
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	@CacheEvict(value = "itemCount", allEntries = true)
	public void reloadBlogs() {
		// first process blogs which have aggregator = null,
		// next blogs with aggregator = false
		// and last blogs with aggregator = true
		List<Blog> blogs = blogRepository.findAll(new Sort(Direction.ASC, "aggregator"));

		// TODO this is very memory-intensive
		List<String> allLinks = itemRepository.findAllLinks();
		List<String> allLowercaseTitles = itemRepository.findAllLowercaseTitles();
		Map<String, Object> allLinksMap = new HashMap<>();
		for (String link : allLinks) {
			allLinksMap.put(link, null);
		}
		Map<String, Object> allLowercaseTitlesMap = new HashMap<>();
		for (String title : allLowercaseTitles) {
			allLowercaseTitlesMap.put(title, null);
		}
		for (Blog blog : blogs) {
			// reindex timeout must have passed in order to index this blog
			if (reindexTimeoutPassed(blog.getLastIndexedDate())) {
				// archived blogs won't be indexed
				if(blog.getArchived() == null || blog.getArchived() == false) {
					blogService.saveItems(blog, allLinksMap, allLowercaseTitlesMap);
				}
			}
		}
		blogService.setLastIndexedDateFinish(new Date());
	}

	/**
	 * Return whether reindex timeout passed. Reindex timeout is between two
	 * dates: current date and the last time some item was saved for some blog.
	 * 
	 * @param lastReindexDate
	 * @return
	 */
	protected boolean reindexTimeoutPassed(Date lastReindexDate) {
		if (lastReindexDate == null) {
			return true;
		}
		Calendar lastReindexCalendar = new GregorianCalendar();
		lastReindexCalendar.setTime(lastReindexDate);
		// reindex timeout is 6 hours
		lastReindexCalendar.add(Calendar.HOUR_OF_DAY, 6);
		return lastReindexCalendar.before(new GregorianCalendar());
	}

	/**
	 * Run every day
	 */
	@Transactional
	@Scheduled(fixedDelay = 24 * 60 * 60 * 1000, initialDelay = 10000)
	public void computePopularity() {
		log.info("compute popularity start");
		for (Blog blog : blogService.findAll(true)) {
			Calendar dateFromCalendar = new GregorianCalendar();
			dateFromCalendar.add(Calendar.MONTH, -3);
			Integer sumPopularity = itemRepository.getSocialSum(blog.getId(), dateFromCalendar.getTime());
			int popularity = 0;
			if(sumPopularity != null) {
				popularity = sumPopularity;
			}
			blogRepository.setPopularity(blog.getId(), popularity);
		}
		log.info("compute popularity end");
	}

	int[] getCurrentWeekAndYear(Date currentDate) {
		LocalDate date = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(); 
		int week = date.get(woy);
		int year = date.getYear();
		return new int[] { week, year };
	}

	/**
	 * Generate best of weekly news
	 */
	@Transactional
	// cron format: second, minute, hour, day of month, month and day of week
	// should run every saturday at 7 A.M.
	@Scheduled(cron = "0 0 7 * * SUN")
	public void addWeeklyNews() {
		log.info("add weekly news");
		final int[] weekAndYear = getCurrentWeekAndYear(new Date());
		final int week = weekAndYear[0];
		final int year = weekAndYear[1];
		String currentWeekShortTitle = "best-of-" + week + "-" + year;
		NewsItem newsItem = newsItemRepository.findByShortName(currentWeekShortTitle);
		if (newsItem == null) {
			newsItem = new NewsItem();
			Configuration configuration = configurationService.find();
			newsItem.setTitle(configuration.getChannelTitle() + " Weekly: Best of " + week + "/" + year);
			newsItem.setShortName(currentWeekShortTitle);
			newsItem.setShortDescription("Best of " + configuration.getChannelTitle() + ", year " + year + ", week " + week);
			String description = "<p>" + configuration.getChannelTitle() + " brings you interesting news every day.";
			description += " Each week I select the best of:</p>";
			List<Category> categories = categoryService.findAll();
			for (Category category : categories) {
				description += "<table class='table'>";
				description += "<tr>";
				description += "<td>";
				description += "<h4>" + category.getName() + "</h4>";
				description += "</td>";
				description += "</tr>";
				List<ItemDto> dtoItems = itemService.getDtoItems(0, false, OrderType.MOST_VIEWED, MaxType.WEEK, new Integer[] { category.getId() });
				for (int i = 0; i < dtoItems.size() && i < 5; i++) {
					ItemDto itemDto = dtoItems.get(i);
					description += "<tr>";
					description += "<td>";
					description += "<a href='" + itemDto.getLink() + "' target='_blank'>";
					description += "<img src='/spring/icon/" + itemDto.getBlog().getId() + "' style='float:left;padding-right:5px;height:30px' />";
					description += itemDto.getTitle();
					description += "</a>";
					description += "</td>";
					description += "</tr>";
				}
				description += "</table>";
			}
			newsItem.setDescription(description);
			newsService.save(newsItem);
		}
	}

	private static class FacebookShareJson {

		private int shares;

		public int getShares() {
			return shares;
		}

		@SuppressWarnings("unused")
		public void setShares(int shares) {
			this.shares = shares;
		}
	}

	private static class LinkedinShareJson {

		private int count;

		public int getCount() {
			return count;
		}

		@SuppressWarnings("unused")
		public void setCount(int count) {
			this.count = count;
		}
	}

	@Autowired
	private RestTemplate restTemplate;

	// will run every 2 hours
	@Scheduled(fixedDelay = 2 * 60 * 60 * 1000, initialDelay = 1000)
	public void retrieveSocialShareCount() {
		log.info("retrieve social share count start");
		Integer[] allCategories = allCategoriesService.getAllCategoryIds();
		int page = 0;
		int retrievedItems = 0;
		do {
			List<ItemDto> dtoItems = itemService.getDtoItems(page++, true, OrderType.LATEST, MaxType.WEEK, allCategories);
			retrievedItems = dtoItems.size();
			for (ItemDto itemDto : dtoItems) {
				try {
					String twitterOauth = configurationService.find().getTwitterOauth();
					if (twitterOauth != null && !twitterOauth.trim().isEmpty()) {
						String[] twitterOauthParts = twitterOauth.split(":");
						String consumerKey = twitterOauthParts[0];
						String consumerKeySecret = twitterOauthParts[1];
						String accessToken = twitterOauthParts[2];
						String accessTokenSecret = twitterOauthParts[3];
						ConfigurationBuilder cb = new ConfigurationBuilder();
						cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerKeySecret).setOAuthAccessToken(accessToken)
								.setOAuthAccessTokenSecret(accessTokenSecret);

						TwitterFactory twitterFactory = new TwitterFactory(cb.build());
						Twitter twitter = twitterFactory.getInstance();

						RateLimitStatus rateLimitStatus = twitter.getRateLimitStatus().get("/search/tweets");
						int remaining = rateLimitStatus.getRemaining();
						if (remaining <= 1) {
							Thread.sleep(15 * 60 * 1000); // sleep for 15
															// minutes, this
															// will reset the
															// limit for sure
						}
						Query query = new Query(itemDto.getLink());
						QueryResult result = twitter.search(query);
						int retweetCount = result.getTweets().size();
						if (retweetCount > itemDto.getTwitterRetweetCount()) {
							itemRepository.setTwitterRetweetCount(itemDto.getId(), retweetCount);
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					FacebookShareJson facebookShareJson = restTemplate.getForObject("http://graph.facebook.com/?id=" + itemDto.getLink(), FacebookShareJson.class);
					if (facebookShareJson.getShares() != itemDto.getFacebookShareCount()) {
						itemRepository.setFacebookShareCount(itemDto.getId(), facebookShareJson.getShares());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					LinkedinShareJson linkedinShareJson = restTemplate.getForObject("https://www.linkedin.com/countserv/count/share?format=json&url=" + itemDto.getLink(), LinkedinShareJson.class);
					if (linkedinShareJson.getCount() != itemDto.getLinkedinShareCount()) {
						itemRepository.setLinkedinShareCount(itemDto.getId(), linkedinShareJson.getCount());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} while (retrievedItems > 0);
		log.info("retrieve social share count finish");
	}

}
