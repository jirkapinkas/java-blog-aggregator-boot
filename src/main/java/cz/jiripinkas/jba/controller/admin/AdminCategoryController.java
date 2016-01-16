package cz.jiripinkas.jba.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jba.dto.CategoryDto;
import cz.jiripinkas.jba.entity.Category;
import cz.jiripinkas.jba.service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

	@Autowired
	private CategoryService categoryService;

	@RequestMapping
	public String categories(Model model) {
		model.addAttribute("categories", categoryService.findAll());
		model.addAttribute("current", "admin-categories");
		return "admin-categories";
	}

	@ModelAttribute
	public Category construct() {
		return new Category();
	}

	@RequestMapping(method = RequestMethod.POST)
	public View save(@ModelAttribute Category category) {
		categoryService.save(category);
		RedirectView redirectView = new RedirectView("categories");
		redirectView.setExposeModelAttributes(false);
		return redirectView;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
	public void delete(@PathVariable int id) {
		categoryService.delete(id);
	}

	@ResponseBody
	@RequestMapping("/{id}")
	public CategoryDto categoryShortName(@PathVariable int id) {
		return categoryService.findOneDto(id);
	}

	@ResponseBody
	@RequestMapping(value = "/set/{blogId}/cat/{categoryId}", method = RequestMethod.POST)
	public String setMapping(@PathVariable int blogId, @PathVariable int categoryId) {
		categoryService.addMapping(blogId, categoryId);
		return "ok";
	}

}
