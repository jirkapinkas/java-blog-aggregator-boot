package cz.jiripinkas.jba.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class ConfigurationInterceptor extends HandlerInterceptorAdapter {

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private BlogService blogService;

	@Autowired
	private UserService userService;

	@Autowired
	private ItemService itemService;
	
	@Autowired
	private Environment environment;

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		/*
		 * Attributes will be available in every page.
		 */
		if (modelAndView != null) {
			if(environment.acceptsProfiles("dev")) {
				modelAndView.getModelMap().addAttribute("isDevProfileActive", true);
			} else {
				modelAndView.getModelMap().addAttribute("isDevProfileActive", false);
			}
			String adBlockClass = RandomStringUtils.randomAlphabetic(30);
			modelAndView.getModelMap().addAttribute("adBlockMessageClass", adBlockClass);
			String adBlockCss = 
					"		<style type=\"text/css\">\n" + 
					"			/* adBlockMessage */\n" + 
					"			." + adBlockClass + " {position: fixed; bottom: 0; left: 0; right: 0; background: #fffd59; color: #222; font-size: 16px; padding: 2em 1em; z-index: 2010; box-shadow: 0 -1px 29px rgba(9,0,0,.78); font-weight: bold; text-align: center; }\n" + 
					"			." + adBlockClass + " a {color: #222; text-decoration: none; padding: 0.6em 1em; margin-left: 1em; border: 2px solid #dad91a; background-color: #fff; }\n" + 
					"			." + adBlockClass + " a:hover {border-color: #222; }\n" + 
					"		</style>\n";
			modelAndView.getModelMap().addAttribute("adBlockCss", adBlockCss);
			modelAndView.getModelMap().addAttribute("configuration", configurationService.find());
			modelAndView.getModelMap().addAttribute("categories", categoryService.findAll());
			modelAndView.getModelMap().addAttribute("lastIndexDate", blogService.getLastIndexDateMinutes());
			modelAndView.getModelMap().addAttribute("blogCount", blogService.count());
			if (request.isUserInRole("ADMIN")) {
				modelAndView.getModelMap().addAttribute("itemCount", itemService.count());
				modelAndView.getModelMap().addAttribute("userCount", userService.count());
				modelAndView.getModelMap().addAttribute("blogCountUnapproved", blogService.countUnapproved());
			}
		}
	}
}