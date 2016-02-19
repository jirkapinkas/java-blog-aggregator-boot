
JBA.index = {}

JBA.index.init = function (topViews, max, maxValue, blogDetail, blogShortName, isAdmin) {
	JBA.index.topViews = topViews;
	JBA.index.max = max;
	JBA.index.maxValue = maxValue;
	JBA.index.blogDetail = blogDetail;
	JBA.index.blogShortName = blogShortName;
	JBA.index.isAdmin = isAdmin;
}

JBA.index.orderByLabelClick = function (e) {
	e.preventDefault();
	orderByCriteria = $(this).attr("id");
	$(".orderByLabel").css("text-decoration", "none");
	$(".orderByLabel").css("font-weight", "normal");
	$(this).css("text-decoration", "underline");
	$(this).css("font-weight", "bold");
	// reload first page
	JBA.index.loadNextPage(null, true);
}

JBA.index.itemRowMouseIn = function (e) {
	var icons = "";
	var itemTitle = $(this).find(".itemTitle").text();
	var itemLink = $(this).find(".itemLink").attr("href");
	icons += "<i class='fa fa-twitter-square fa-4x socialIconTwitter' onclick=\"twShare('" + itemLink + "','" + itemTitle + "')\"></i>";
	icons += "<i class='fa fa-facebook-square fa-4x socialIconFacebook' onclick=\"fbShare('" + itemLink + "')\"></i>";
	icons += "<i class='fa fa-google-plus-square fa-4x socialIconGooglePlus' onclick=\"gpShare('" + itemLink + "')\"></i>";
	$(this).find(".socialButtons").html(icons);
}

JBA.index.itemRowMouseOut = function (e) {
	$(this).find(".socialButtons").html("");
}

JBA.index.setSelectedCategories = function (e) {
	// set selectedCategories
	if($.cookie("selectedCategories")) {
		// retrieve categories from cookie
		selectedCategories = JSON.parse(unescape($.cookie("selectedCategories")));
		// update category labels
		$(".categoryLabel").css("text-decoration", "line-through");
		for (var i = 0; i < selectedCategories.length; i++) {
			$("#" + selectedCategories[i] +  ".categoryLabel").css("text-decoration", "none");
		}
	} else {
		// select all categories
		$.getJSON("/all-categories", function(data) {
			selectedCategories = data;
			$.cookie("selectedCategories", JSON.stringify(selectedCategories), {expires: 30, path: '/'});
		});
	}
}

JBA.index.categoryLabelClick = function (e) {
	var categoryId = parseInt($(this).attr("id"));
	var arrIndex = $.inArray(categoryId, selectedCategories);
	if(arrIndex != -1) {
		selectedCategories.splice(arrIndex, 1);
		$(this).css("text-decoration", "line-through");
	} else {
		selectedCategories.push(categoryId);
		$(this).css("text-decoration", "none");
	}
	// store categories to cookie
	$.cookie("selectedCategories", unescape(JSON.stringify(selectedCategories)), {expires: 30, path: '/'});
	// reload first page
	JBA.index.loadNextPage(null, true);
}

/*
 * clear = boolean whether the list should be cleared.
 */

JBA.index.loadNextPage = function (e, clear) {
	if(clear) {
		currentPage = -1;
	}
	if(e != null) {
		e.preventDefault();
	}
	startRefresh();
	var nextPage = currentPage + 1;
	var url = "/page/" + nextPage;
	var iconBaseUrl = "/spring/icon/";
	var blogDetailBaseUrl = "/blog/";
	if(JBA.index.topViews == true) {
		url = url + "?topviews=true";
		if(JBA.index.max == true) {
			url = url + "&max=" + JBA.index.maxValue;
		}
	} else if(JBA.index.blogDetail == true) {
		url = url + "?shortName=" + JBA.index.blogShortName;
	}
	if(url.indexOf("?") == -1) {
		url = url + "?selectedCategories=" + selectedCategories.join(',');
	} else {
		url = url + "&selectedCategories=" + selectedCategories.join(',');
	}
	if(searchTxt != undefined && searchTxt != null) {
		url = url + "&search=" + searchTxt;
	}
	if(orderByCriteria != undefined && orderByCriteria != null) {
		url = url + "&orderBy=" + orderByCriteria;
	}


	$.getJSON( url, function( data ) {
		if(clear) {
			$(".tableItems tbody .item-row").remove();
		}
		// if we're not on first page, then set yellow background
		setYellowBackground = currentPage !== 0;
		var html = "";
		$.each(data, function(key, value) {
			html += "<tr class='item-row' style='";
			if(setYellowBackground) {
				html += "background-color:#ffff99";
			}
			html += "'><td>";
			html += ' <div style="float:left">';

			var css = "";
			if(value.enabled == false) {
				css = "text-decoration: line-through;color:grey";
			}
			html += "<a href='" + value.link + "' class='itemLink' style='" + css + "' onClick='itemClick(event)' id='" + value.id + "' target='_blank'>";
			html += "<img class='lazy' data-src='" + iconBaseUrl + value.blog.id + "' alt='icon' style='float:left;padding-right:10px' id='" + value.id + "' />";
			html += "<strong id='" + value.id + "'>";
			html += value.title;
			html += " <span class='glyphicon glyphicon-share-alt'></span>";
			html += "</strong>";
			html += "</a>";
			html += "<br />";
			html += "<span class='itemDesc' style='" + css + "'>";
			html += value.description;
			html += "</span>";
			html += "<div style='padding-top:10px;'></div>";

			// show like / dislike buttons
			html += ' <table style="float:left;margin-right:5px">';
			html += ' <tr>';
			html += ' <td style="padding:2px">';
			html += ' <i style="color:#6273a9;cursor:pointer;" ';
			html += ' class="fa fa-thumbs-o-up fa-lg icon_like_' + value.id + '" ';
			html += ' id="' + value.id + '" ';
			html += ' onClick="itemLike(event)" title="like"></i>';
			html += ' </td>';

			html += ' <td style="padding:2px">';
			html += ' <span class="likeCount_' + value.id + '">' + value.displayLikeCount + '</span>';
			html += ' </td>';

			html += ' </tr>';
			html += ' </table>';

			var date = new Date(value.savedDate);
			html += "<span class='label' style='color: grey;'>";
			html += ("0" + date.getDate()).slice(-2) + "-" + ("0" + (date.getMonth() + 1)).slice(-2) + "-" + date.getFullYear();
			html += " " + ("0" + date.getHours()).slice(-2) + ":" + ("0" + date.getMinutes()).slice(-2) + ":" + ("0" + date.getSeconds()).slice(-2);
			html += "</span>";
			html += "<span class='label label-info' style='margin-left: 5px'><a href='" + blogDetailBaseUrl + value.blog.shortName + "' style='color: white;'>";
			html += value.blog.publicName;
			html += "</a></span>";
			if(value.blog.category != null) {
				html += ' <span class="label label-default">' + value.blog.category.name + '</span>';
			}
			if(JBA.index.isAdmin) {
				html += JBA.index.generateAdminMenu(value);
			}
			html += ' </div>'; //end float:left
			html += ' <div style="position:relative">';
			html += ' <div class="socialButtons" style="position:absolute;z-index:10;right:0"></div>';
			html += ' </div>';
			html += "</td></tr>";
		});
		var newCode = $(".table tr:last").prev().after(html);
		$("img.lazy").unveil(unveilTreshold);
		// set like / dislike buttons state
		var changedAnything = false;
		$.each(data, function(key, value) {
			showCurrentState(value.id);
			changedAnything = true;
		});
		// remove yellow background
		if(setYellowBackground) {
			setTimeout(function() {
				$(".item-row").css("background-color", "");
			}, 300);
		}
		finishRefresh(changedAnything);
	});
	currentPage++;
}

/*
 * generates administrator menu
 */
JBA.index.generateAdminMenu = function (item) {
	var html = "";
	html += '<span class="label label-default" style="margin-left: 5px"><i class="fa fa-eye"></i> ' + item.clickCount + '</span> ';
	html += '<span class="label label-default"><i class="fa fa-thumbs-up"></i> ' + item.likeCount + '</span> ';
	html += '<span class="label label-default"><i class="fa fa-twitter"></i> ' + item.twitterRetweetCount + '</span> ';
	html += '<span class="label label-default"><i class="fa fa-facebook"></i> ' + item.facebookShareCount + '</span> ';
	html += '<span class="label label-default"><i class="fa fa-linkedin"></i> ' + item.linkedinShareCount + '</span> ';
	html += '<a href="/admin/items/toggle-enabled/' + item.id + '" class="btn btn-primary btn-xs btnToggleEnabled" onclick="event.preventDefault();JBA.index.toggleEnabledItem(this);">';
	if(item.enabled) {
		html += 'disable';
	} else {
		html += 'enable';
	}
	html += '</a>';
	return html;
}

JBA.index.toggleEnabledItem = function (e) {
	var href = $(e).attr("href");
	var curr = $(e);
	$.getJSON( href, function(data) {
		var css1 = "";
		var css2 = "";
		if(data == true) {
			css1 = "";
			css2 = "";
			$(curr).text("disable");
		} else {
			css1 = "line-through";
			css2 = "grey";
			$(curr).text("enable");
		}
		var itemLink = $(curr).closest("tr").find(".itemLink");
		itemLink.css("text-decoration", css1);
		itemLink.css("color", css2);
		var itemDesc = $(curr).closest("tr").find(".itemDesc");
		itemDesc.css("text-decoration", css1);
		itemDesc.css("color", css2);
	});
};
