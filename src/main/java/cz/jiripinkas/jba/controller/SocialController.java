package cz.jiripinkas.jba.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.service.ItemService;

@Controller
@RequestMapping("/social")
public class SocialController {
	
	private static final Logger log = LoggerFactory.getLogger(SocialController.class);

	@Autowired
	private ItemService itemService;

	@RequestMapping(value = "/like", method = RequestMethod.POST)
	@ResponseBody
	public String like(@RequestParam int itemId, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Inc like to item with id: {}", itemId);
		return Integer.toString(itemService.incLike(itemId));
	}

	@RequestMapping(value = "/unlike", method = RequestMethod.POST)
	@ResponseBody
	public String unlike(@RequestParam int itemId, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Dec like to item with id: {}", itemId);
		return Integer.toString(itemService.decLike(itemId));
	}

	@RequestMapping(value = "/dislike", method = RequestMethod.POST)
	@ResponseBody
	public String dislike(@RequestParam int itemId, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Inc dislike to item with id: {}", itemId);
		return Integer.toString(itemService.incDislike(itemId));
	}

	@RequestMapping(value = "/undislike", method = RequestMethod.POST)
	@ResponseBody
	public String undislike(@RequestParam int itemId, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Dec dislike to item with id: {}", itemId);
		return Integer.toString(itemService.decDislike(itemId));
	}

}
