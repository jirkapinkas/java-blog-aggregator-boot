package cz.jiripinkas.jba.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.dto.ItemDto;
import cz.jiripinkas.jba.service.AllCategoriesService;
import cz.jiripinkas.jba.service.ConfigurationService;
import cz.jiripinkas.jba.service.ItemService;
import cz.jiripinkas.jba.service.ItemService.MaxType;
import cz.jiripinkas.jba.service.ItemService.OrderType;

@Controller
public class IndexController {

	private static final Logger log = LoggerFactory.getLogger(IndexController.class);

	@Autowired
	private ItemService itemService;

	@Autowired
	private AllCategoriesService allCategoriesService;

	@Autowired
	private ConfigurationService configurationService;
	
	@RequestMapping("/404")
	public String error404() {
		return "404";
	}

	/**
	 * This method is called from index.html to get all itemIds from list of Items
	 * @return All itemIds from list of Items
	 */
	public List<Integer> getItemIds(List<ItemDto> items) {
		return items.stream().map(ItemDto::getId).collect(Collectors.toList());
	}

	private String showFirstPage(Model model, HttpServletRequest request, OrderType orderType, MaxType maxType, String selectedCategoriesString) {
		return showPage(model, request, 0, orderType, maxType, selectedCategoriesString);
	}

	private String showPage(Model model, HttpServletRequest request, int page, OrderType orderType, MaxType maxType, String selectedCategoriesString) {
		boolean showAll = false;
		if (request.isUserInRole("ADMIN")) {
			showAll = true;
		}
		model.addAttribute("items", itemService.getDtoItems(page, showAll, orderType, maxType, getSelectedCategories(selectedCategoriesString)));
		model.addAttribute("nextPage", page + 1);
		return "index";
	}

	private Integer[] getSelectedCategories(String selectedCategoriesString) {
		Integer[] selectedCategories;
		if (selectedCategoriesString == null) {
			selectedCategories = allCategoriesService.getAllCategoryIds();
		} else {
			String[] strings = selectedCategoriesString.replace("[", "").replace("]", "").split(",");
			List<Integer> selectedCategoriesList = new ArrayList<>();
			for (String string : strings) {
				if (!string.trim().isEmpty()) {
					selectedCategoriesList.add(Integer.parseInt(string.trim()));
				}
			}
			selectedCategories = selectedCategoriesList.toArray(new Integer[] {});
		}
		return selectedCategories;
	}
	
	@RequestMapping("/")
	public String index(Model model, HttpServletRequest request, @CookieValue(value = "selectedCategories", required = false) String selectedCategoriesString, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to homepage with selectedCategories: {}", selectedCategoriesString);
		model.addAttribute("title", configurationService.find().getHomepageHeading());
		return showFirstPage(model, request, OrderType.LATEST, MaxType.UNDEFINED, selectedCategoriesString);
	}

	@RequestMapping(value = "/", params = "page")
	public String index(Model model, @RequestParam int page, HttpServletRequest request, @CookieValue(required = false) String selectedCategoriesString, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to homepage with selectedCategories: {}, page: {}", selectedCategoriesString, page);
		model.addAttribute("title", configurationService.find().getHomepageHeading());
		return showPage(model, request, page, OrderType.LATEST, MaxType.UNDEFINED, selectedCategoriesString);
	}

	@ResponseBody
	@RequestMapping("/page/{page}")
	public List<ItemDto> getPageLatest(@PathVariable int page, HttpServletRequest request, @RequestParam Integer[] selectedCategories, @RequestParam(required = false) String search,
			@RequestParam(required = false) String orderBy, @RequestParam(required = false) String shortName, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Navigated to JSON, page {} with selectedCategories: {}", page, Arrays.asList(selectedCategories));
		if (search != null && !search.trim().isEmpty()) {
			log.info("search for: {}", search);
		}
		boolean showAll = false;
		if (request.isUserInRole("ADMIN")) {
			showAll = true;
		}
		if ("topWeek".equals(orderBy)) {
			return itemService.getDtoItems(page, showAll, OrderType.MOST_VIEWED, MaxType.WEEK, selectedCategories, search, shortName);
		} else if ("topMonth".equals(orderBy)) {
			return itemService.getDtoItems(page, showAll, OrderType.MOST_VIEWED, MaxType.MONTH, selectedCategories, search, shortName);
		} else {
			return itemService.getDtoItems(page, showAll, OrderType.LATEST, MaxType.UNDEFINED, selectedCategories, search, shortName);
		}
	}

	@ResponseBody
	@RequestMapping(value = "/inc-count", method = RequestMethod.POST)
	public String incItemCount(@RequestParam int itemId, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		log.info("UA: {}", userAgent);
		log.info("Inc count to item with id: {}", itemId);
		return Integer.toString(itemService.incCount(itemId));
	}

}
