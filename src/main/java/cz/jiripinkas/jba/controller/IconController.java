package cz.jiripinkas.jba.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.jiripinkas.jba.service.BlogService;
import cz.jiripinkas.jba.service.ConfigurationService;

@Controller
@RequestMapping(value = "/spring/icon", produces = MediaType.IMAGE_PNG_VALUE)
public class IconController {

	@Autowired
	private BlogService blogService;

	@Autowired
	private ConfigurationService configurationService;
	
	@RequestMapping
	public @ResponseBody byte[] getIcon() throws IOException {
		return configurationService.find().getIcon();
	}

	@RequestMapping(value = "/{blogId}")
	public @ResponseBody byte[] getBlogIcon(@PathVariable int blogId) throws IOException {
		return blogService.getIcon(blogId);
	}
	
	@RequestMapping(value = "/favicon")
	public @ResponseBody byte[] getFavicon() throws IOException {
		return configurationService.find().getFavicon();
	}

	@RequestMapping(value = "/appleTouchIcon")
	public @ResponseBody byte[] getAppleTouchIcon() throws IOException {
		return configurationService.find().getAppleTouchIcon();
	}

}
