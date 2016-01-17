
JBA.blogs = {};

/*
 * admininstrator functionality
 */

JBA.blogs.categorySelectChange = function (element) {
	var blogId = $(element).attr("id");
	var categoryId = $(element).val();
	$.post("admin/categories/set/" + blogId + "/cat/" + categoryId + ".json", function(data) { });
}

JBA.blogs.remove = function (e) {
	var origin = $(e);
    BootstrapDialog.show({
        title: 'Really delete?',
        message: 'Really delete?',
        buttons: [{
            label: 'Cancel',
            action: function(dialog) {
            	dialog.close();
            }
        }, {
            label: 'Delete',
            cssClass: 'btn-primary',
            action: function(dialog) {
            	var blogId = origin.attr("id");
				$.post("blog/remove/" + blogId, function (data) {
					location.reload(true); // reload page
				});
            	dialog.close();
            }
        }]
    });
}
